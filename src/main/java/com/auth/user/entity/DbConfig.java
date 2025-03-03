package com.auth.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "db_config")
public class DbConfig extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name; // readable name

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DbConfigType type;
}