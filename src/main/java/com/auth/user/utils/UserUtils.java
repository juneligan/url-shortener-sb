package com.auth.user.utils;

import com.auth.user.service.model.UserDetailsImpl;

public class UserUtils {

    public static final int DEFAULT_SMS_LIMIT_PER_HR = 5;
    private static final String REGEX_SANITIZE_PH_CODE = "^\\+?63";
    private static final String PH_NUM_PREFIX = "0";

    private UserUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getPhoneNumberFromPrincipal(Object principal) {
        return ((UserDetailsImpl) principal).getPhoneNumber();
    }

    public static String getSanitizedPhoneNumber(String phoneNumber) {
        return phoneNumber.replaceFirst(REGEX_SANITIZE_PH_CODE, PH_NUM_PREFIX);
    }
}
