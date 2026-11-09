package com.firstissuefinder.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class GitHubIssueServiceTest {

    @Test
    void queryCombinesSelectedLabelsAndLanguage() {
        GitHubIssueService service = new GitHubIssueService(RestClient.builder(), "");

        assertEquals("is:issue is:open label:\"good first issue\",\"help wanted\" language:Java",
                service.buildQuery("java", List.of("help wanted", "good first issue")));
        assertEquals("is:issue is:open label:\"good first issue\" language:Java",
                service.buildQuery("Java", List.of("not a supported label")));
    }

    @Test
    void mapsGithubIssueIntoSmallApiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://api.github.com/search/issues?")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("""
                        {
                          "total_count": 1,
                          "items": [{
                            "id": 17,
                            "number": 42,
                            "title": "Add a keyboard shortcut",
                            "html_url": "https://github.com/acme/project/issues/42",
                            "repository_url": "https://api.github.com/repos/acme/project",
                            "labels": [{"name": "good first issue"}],
                            "created_at": "2026-01-01T00:00:00Z",
                            "updated_at": "2026-02-01T00:00:00Z",
                            "comments": 3
                          }, {
                            "id": 18,
                            "number": 43,
                            "title": "Pull request should not appear",
                            "html_url": "https://github.com/acme/project/pull/43",
                            "repository_url": "https://api.github.com/repos/acme/project",
                            "labels": [],
                            "created_at": "2026-01-01T00:00:00Z",
                            "updated_at": "2026-02-01T00:00:00Z",
                            "comments": 0,
                            "pull_request": {"url": "https://api.github.com/repos/acme/project/pulls/43"}
                          }]
                        }
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        IssueSearchResponse result = new GitHubIssueService(builder, "").search("Java", null, 1);

        assertEquals(1, result.totalCount());
        assertEquals("acme/project", result.items().get(0).repository());
        assertEquals(List.of("good first issue"), result.items().get(0).labels());
        assertEquals(3, result.items().get(0).comments());
        server.verify();
    }

    @Test
    void turnsGithubRateLimitInto429() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/issues?")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .header("X-RateLimit-Remaining", "0")
                        .body("{\"message\":\"API rate limit exceeded\"}"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new GitHubIssueService(builder, "").search("Java", null, 1));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        server.verify();
    }
}
