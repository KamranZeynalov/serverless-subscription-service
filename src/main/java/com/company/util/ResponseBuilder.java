package com.company.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ResponseBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> buildResponse(int statusCode, Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", Map.of("Content-Type", "application/json"));
        try {
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            response.put("body", "{\"error\":\"Response serialization failed\"}");
        }
        return response;
    }
}
