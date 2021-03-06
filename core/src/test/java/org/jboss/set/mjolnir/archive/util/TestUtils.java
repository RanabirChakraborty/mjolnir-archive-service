package org.jboss.set.mjolnir.archive.util;

import org.apache.commons.io.FileUtils;
import org.jboss.set.mjolnir.archive.domain.RemovalStatus;
import org.jboss.set.mjolnir.archive.domain.UserRemoval;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Timestamp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Prepares GitHub API stubs.
     *
     * The test case need to contain @Rule annotated WireMockRule field.
     */
    public static void setupGitHubApiStubs() throws IOException, URISyntaxException {
        stubFor(get(urlPathEqualTo("/api/v3/orgs/testorg/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readSampleResponse("responses/gh-orgs-repos-response.json"))));

        stubFor(get(urlPathMatching("/api/v3/repos/testorg/aphrodite/forks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readSampleResponse("responses/gh-repos-forks-aphrodite-response.json"))));

        stubFor(get(urlPathMatching("/api/v3/repos/testorg/activemq-artemis/forks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readSampleResponse("responses/gh-repos-forks-artemis-response.json"))));
    }

    public static String readSampleResponse(String responseFileName) throws URISyntaxException, IOException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(responseFileName);

        if (resource == null) {
            throw new RuntimeException();
        }
        return FileUtils.readFileToString(
                Paths.get(resource.toURI()).toFile(),
                StandardCharsets.UTF_8.name());
    }

    public static UserRemoval createUserRemoval(String ldapName, Timestamp timeStarted, Timestamp timeCompleted, RemovalStatus status) throws NoSuchFieldException, IllegalAccessException {
        return createUserRemoval(ldapName, null, timeStarted, timeCompleted, status);
    }

    public static UserRemoval createUserRemoval(String ldapName, String ghName, Timestamp timeStarted, Timestamp timeCompleted, RemovalStatus status) throws NoSuchFieldException, IllegalAccessException {
        Field completedField = UserRemoval.class.getDeclaredField("completed");
        completedField.setAccessible(true);
        Field statusField = UserRemoval.class.getDeclaredField("status");
        statusField.setAccessible(true);

        UserRemoval userRemoval = new UserRemoval();
        userRemoval.setLdapUsername(ldapName);
        userRemoval.setGithubUsername(ghName);
        userRemoval.setStarted(timeStarted);

        completedField.set(userRemoval, timeCompleted);
        statusField.set(userRemoval, status);

        return userRemoval;
    }
}
