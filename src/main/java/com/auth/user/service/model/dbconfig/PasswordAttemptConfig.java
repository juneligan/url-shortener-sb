package com.auth.user.service.model.dbconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordAttemptConfig implements IAttemptConfig {
    private int maxAttempts;
    private int resetMinutes;
}
