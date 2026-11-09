package io.github.mgadek84.lichess.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Lichess error body, e.g. {@code {"error": "Not found"}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiError(String error) {}
