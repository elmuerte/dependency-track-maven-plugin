package io.github.pmckeown.dependencytrack.project;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.pmckeown.TestMojoLoader.loadDeleteProjectMojo;
import static io.github.pmckeown.dependencytrack.ResourceConstants.V1_PROJECT_LOOKUP;
import static io.github.pmckeown.dependencytrack.TestResourceConstants.V1_PROJECT_UUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.http.Fault;
import io.github.pmckeown.dependencytrack.AbstractDependencyTrackMojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;

public class DeleteProjectMojoIntegrationTest extends AbstractDependencyTrackMojoTest {

    private DeleteProjectMojo deleteProjectMojo;

    @Before
    public void setup() throws Exception {
        deleteProjectMojo = loadDeleteProjectMojo(mojoRule);
        deleteProjectMojo.setDependencyTrackBaseUrl("http://localhost:" + wireMockRule.port());
        deleteProjectMojo.setProjectName("dependency-track");
        deleteProjectMojo.setProjectVersion("3.6.0-SNAPSHOT");
        deleteProjectMojo.setFailOnError(false);
    }

    @Test
    public void thatAProjectCanBeDeleted() throws Exception {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP))
                .willReturn(aResponse().withBodyFile("api/v1/project/dependency-track-3.6.json")));
        stubFor(delete(urlPathMatching(V1_PROJECT_UUID)).willReturn(aResponse().withStatus(200)));

        deleteProjectMojo.execute();

        verify(exactly(1), deleteRequestedFor(urlPathMatching(V1_PROJECT_UUID)));
    }

    @Test
    public void thatWhenProjectDeletionFailedAndFailOnErrorFalseThenMojoSucceeds() {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP))
                .willReturn(aResponse().withBodyFile("api/v1/project/dependency-track-3.6.json")));
        stubFor(delete(urlPathMatching(V1_PROJECT_UUID)).willReturn(aResponse().withStatus(500)));

        deleteProjectMojo.setFailOnError(false);

        try {
            deleteProjectMojo.execute();
        } catch (Exception ex) {
            fail("No exception expected");
        }
    }

    @Test(expected = MojoFailureException.class)
    public void thatWhenProjectDeletionFailedAndFailOnErrorTrueThenMojoFailureExceptionIsThrown()
            throws MojoExecutionException, MojoFailureException {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP))
                .willReturn(aResponse().withBodyFile("api/v1/project/testName-project.json")));
        stubFor(delete(urlPathMatching(V1_PROJECT_UUID)).willReturn(aResponse().withStatus(500)));

        deleteProjectMojo.setFailOnError(true);

        deleteProjectMojo.execute();
    }

    @Test
    public void thatWhenProjectIsNotFoundDeletionIsNotAttempted() throws Exception {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP)).willReturn(aResponse()));
        deleteProjectMojo.setProjectName("unknown");
        deleteProjectMojo.setProjectVersion("1.2.3-SNAPSHOT");

        try {
            deleteProjectMojo.execute();
        } catch (MojoExecutionException ex) {
            assertThat(ex, is(instanceOf(MojoExecutionException.class)));
        }

        verify(exactly(0), deleteRequestedFor(urlPathMatching(V1_PROJECT_UUID)));
    }

    @Test
    public void thatWhenProjectDeleteErrorsAndFailOnErrorTrueThenMojoExecutionExceptionIsThrown() {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP))
                .willReturn(aResponse().withBodyFile("api/v1/project/testName-project.json")));
        stubFor(delete(urlPathMatching(V1_PROJECT_UUID))
                .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        deleteProjectMojo.setFailOnError(true);

        try {
            deleteProjectMojo.execute();
        } catch (Exception ex) {
            assertThat(ex, is(instanceOf(MojoExecutionException.class)));
        }
    }

    @Test
    public void thatWhenProjectDeleteErrorsAndFailOnErrorFalseThenMojoSucceeds() {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP))
                .willReturn(aResponse().withBodyFile("api/v1/project/testName-project.json")));
        stubFor(delete(urlPathMatching(V1_PROJECT_UUID))
                .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        deleteProjectMojo.setFailOnError(false);

        try {
            deleteProjectMojo.execute();
        } catch (Exception ex) {
            fail("No exception expected");
        }
    }

    @Test
    public void thatDeleteIsSkippedWhenSkipIsTrue() throws Exception {
        stubFor(get(urlPathEqualTo(V1_PROJECT_LOOKUP))
                .willReturn(aResponse().withBodyFile("api/v1/project/testName-project.json")));
        stubFor(delete(urlPathMatching(V1_PROJECT_UUID)).willReturn(aResponse().withStatus(200)));

        deleteProjectMojo.setSkip("true");

        deleteProjectMojo.execute();

        verify(exactly(0), deleteRequestedFor(urlPathMatching(V1_PROJECT_UUID)));
    }
}
