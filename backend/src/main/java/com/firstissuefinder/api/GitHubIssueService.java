package com.firstissuefinder.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GitHubIssueService {
    public static final List<String> LANGUAGES = List.of(
            "Java", "JavaScript", "TypeScript", "Python", "C++", "Go", "Rust", "Kotlin"
    );
    private static final Set<String> ALLOWED_LABELS = Set.of("good first issue", "help wanted");
    private static final int PER_PAGE = 20;
    private static final int MAX_PAGE = 10;

    private final RestClient restClient;

    public GitHubIssueService(RestClient.Builder builder, @Value("${github.token:}") String token) {
        RestClient.Builder configured = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "FirstIssueFinder/1.0");
        if (!token.isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        this.restClient = configured.build();
    }

    @Cacheable(cacheNames = "issueSearches", key = "#root.target.cacheKey(#language, #labels, #page)")
    public IssueSearchResponse search(String language, List<String> labels, int page) {
        int safePage = Math.max(1, Math.min(page, MAX_PAGE));
        List<String> safeLabels = normalizeLabels(labels);
        String query = buildQuery(language, safeLabels);

        try {
            JsonNode response = restClient.get()
                    .uri(uri -> uri.path("/search/issues")
                            .queryParam("q", query)
                            .queryParam("sort", "updated")
                            .queryParam("order", "desc")
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("page", safePage)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub returned an empty response.");
            }
            List<IssueDto> issues = new ArrayList<>();
            JsonNode items = response.path("items");
            if (items.isArray()) {
                items.forEach(item -> {
                    if (!item.has("pull_request")) issues.add(toIssue(item));
                });
            }
            return new IssueSearchResponse(issues, response.path("total_count").asLong(), safePage, PER_PAGE);
        } catch (HttpStatusCodeException exception) {
            String message = exception.getResponseBodyAsString().toLowerCase(Locale.ROOT);
            String remaining = exception.getResponseHeaders() == null ? null
                    : exception.getResponseHeaders().getFirst("X-RateLimit-Remaining");
            if (exception.getStatusCode().value() == 429
                    || (exception.getStatusCode().value() == 403
                    && ("0".equals(remaining) || message.contains("rate limit")))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "GitHub is limiting requests. Please wait and try again.", exception);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "GitHub could not complete the issue search.", exception);
        }
    }

    public String cacheKey(String language, List<String> labels, int page) {
        return String.join("|", normalizeLabels(labels)) + "|" + languageKey(language) + "|" + page;
    }

    String buildQuery(String language, List<String> labels) {
        StringBuilder query = new StringBuilder("is:issue is:open");
        List<String> safeLabels = normalizeLabels(labels);
        if (!safeLabels.isEmpty()) {
            String labelExpression = safeLabels.stream()
                    .map(label -> "\"" + label + "\"")
                    .collect(Collectors.joining(","));
            query.append(" label:").append(labelExpression);
        }
        String normalizedLanguage = languageKey(language);
        if (!normalizedLanguage.isBlank()) query.append(" language:").append(normalizedLanguage);
        return query.toString();
    }

    List<String> normalizeLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) return List.of("good first issue");
        List<String> normalized = labels.stream()
                .map(String::trim)
                .filter(ALLOWED_LABELS::contains)
                .distinct()
                .sorted()
                .toList();
        return normalized.isEmpty() ? List.of("good first issue") : normalized;
    }

    private String languageKey(String language) {
        if (language == null || language.isBlank()) return "";
        return LANGUAGES.stream().filter(candidate -> candidate.equalsIgnoreCase(language.trim()))
                .findFirst().orElse("");
    }

    private IssueDto toIssue(JsonNode item) {
        String repositoryUrl = item.path("repository_url").asText("");
        String repository = repositoryUrl.replace("https://api.github.com/repos/", "");
        if (repository.isBlank()) {
            String htmlUrl = item.path("html_url").asText("");
            String[] segments = htmlUrl.replace("https://github.com/", "").split("/");
            repository = segments.length >= 2 ? segments[0] + "/" + segments[1] : "Unknown repository";
        }
        List<String> labels = new ArrayList<>();
        JsonNode labelNodes = item.path("labels");
        if (labelNodes.isArray()) labelNodes.forEach(label -> labels.add(label.path("name").asText()));
        return new IssueDto(
                item.path("id").asLong(),
                item.path("number").asInt(),
                item.path("title").asText("Untitled issue"),
                item.path("html_url").asText("https://github.com"),
                repository,
                labels,
                parseInstant(item.path("created_at").asText()),
                parseInstant(item.path("updated_at").asText()),
                item.path("comments").asInt()
        );
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return Instant.EPOCH;
        }
    }
}
