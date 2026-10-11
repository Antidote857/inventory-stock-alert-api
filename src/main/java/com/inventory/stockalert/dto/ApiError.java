package com.inventory.stockalert.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, List<String>> fieldErrors
) {
}