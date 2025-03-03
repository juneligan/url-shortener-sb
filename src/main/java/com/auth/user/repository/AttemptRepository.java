package com.auth.user.repository;

import com.auth.user.entity.Attempt;
import com.auth.user.entity.AttemptType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    Optional<Attempt> findByPhoneNumberAndType(String phoneNumber, AttemptType type);
}