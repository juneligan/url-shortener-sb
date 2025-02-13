package com.auth.user.utils;

import com.auth.user.service.model.UserDetailsImpl;

public class UserUtils {

    private UserUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getPhoneNumberFromPrincipal(Object principal) {
        return ((UserDetailsImpl) principal).getPhoneNumber();
    }
}
