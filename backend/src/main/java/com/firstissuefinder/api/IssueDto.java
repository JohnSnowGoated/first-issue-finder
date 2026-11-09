package com.firstissuefinder.api;

import java.time.Instant;
import java.util.List;

public record IssueDto(
        long id,
        int number,
        String title,
        String url,
        String repository,
        List<String> labels,
        Instant createdAt,
        Instant updatedAt,
        int comments
) { }
