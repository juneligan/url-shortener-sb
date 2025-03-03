package com.auth.user.service;

import com.auth.user.entity.DbConfig;
import com.auth.user.entity.DbConfigType;
import com.auth.user.repository.DbConfigRepository;
import com.auth.user.service.model.dbconfig.IDbConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Service
@Slf4j
public class DbConfigService {
    private final DbConfigRepository dbConfigRepository;
    private final ObjectMapper objectMapper;

    @Cacheable("config")
    public Optional<IDbConfig> getConfig(DbConfigType type) {
        return dbConfigRepository.findByType(type)
                .map(dbConfig -> {
                    try {
                        return objectMapper.readValue(dbConfig.getValue(), type.getClazz());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse config value", e);
                        return null;
                    }
                });
    }

    // Save the config values to the database
    public void saveConfig(String name, Map<String, Object> values) {
        DbConfigType type = Optional.ofNullable(DbConfigType.fromName(name))
                .orElseThrow(() -> new IllegalArgumentException("Invalid config name: " + name));
        try {
            String jsonValue = objectMapper.writeValueAsString(values);
            DbConfig dbConfig = dbConfigRepository.findByType(type)
                    .orElse(DbConfig.builder()
                            .name(name)
                            .value(jsonValue)
                            .build()
                    );
            dbConfigRepository.save(dbConfig);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config values", e);
        }
    }
}