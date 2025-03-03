package com.auth.user.repository;

import com.auth.user.entity.DbConfig;
import com.auth.user.entity.DbConfigType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DbConfigRepository extends JpaRepository<DbConfig, Long> {
    Optional<DbConfig> findByType(DbConfigType type);
}