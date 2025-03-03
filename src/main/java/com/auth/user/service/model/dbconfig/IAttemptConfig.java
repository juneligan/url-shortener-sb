package com.auth.user.service.model.dbconfig;

public interface IAttemptConfig extends IDbConfig {
    int getMaxAttempts();
    int getResetMinutes();
}
