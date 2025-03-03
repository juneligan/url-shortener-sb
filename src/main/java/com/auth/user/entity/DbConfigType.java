package com.auth.user.entity;

import com.auth.user.service.model.dbconfig.IDbConfig;
import com.auth.user.service.model.dbconfig.OtpAttemptConfig;
import com.auth.user.service.model.dbconfig.PasswordAttemptConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DbConfigType {
    OTP_ATTEMPTS("OTP Attempts", OtpAttemptConfig.class),
    PASSWORD_ATTEMPTS("Password Attempts", PasswordAttemptConfig.class);

    private final String name;
    private final Class<? extends IDbConfig> clazz;

    public static DbConfigType fromName(String name) {
        for (DbConfigType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
