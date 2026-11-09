package com.firstissuefinder.api;

import java.util.List;

public record IssueSearchResponse(List<IssueDto> items, long totalCount, int page, int perPage) { }
