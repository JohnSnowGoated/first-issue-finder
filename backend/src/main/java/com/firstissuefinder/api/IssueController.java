package com.firstissuefinder.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IssueController {
    private final GitHubIssueService issueService;

    public IssueController(GitHubIssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/issues")
    public IssueSearchResponse searchIssues(
            @RequestParam(defaultValue = "") String language,
            @RequestParam(name = "label", required = false) List<String> labels,
            @RequestParam(defaultValue = "1") int page
    ) {
        return issueService.search(language, labels, page);
    }

    @GetMapping("/languages")
    public List<String> languages() {
        return GitHubIssueService.LANGUAGES;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    public record HealthResponse(String status) { }
}
