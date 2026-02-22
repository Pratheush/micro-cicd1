package com.example.pbookmark.domain;
/*
This wrapper class FindBookmarksQuery will be convenient
if you want to enhance the API with some filtering and sorting capabilities in the future.
 */
public record FindBookmarksQuery(int pageNo, int pageSize) {}
