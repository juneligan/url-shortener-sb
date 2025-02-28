package com.auth.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;

public class TestHelper {

    private TestHelper() {
        throw new IllegalStateException("Utility static class");
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    public static boolean compareObjectAsJsonString(@NotNull  Object obj1, @NotNull Object obj2) {
        try {
            return getObjectMapper().writeValueAsString(obj1).equals(getObjectMapper().writeValueAsString(obj2));
        } catch (Exception e) {
            // e.printStackTrace(); // uncomment to debug
            return false;
        }
    }
}
