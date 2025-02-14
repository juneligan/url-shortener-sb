package com.auth.user.entity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

public class AuditEntityListener {
    @PrePersist
    public void prePersist(BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
    }
}