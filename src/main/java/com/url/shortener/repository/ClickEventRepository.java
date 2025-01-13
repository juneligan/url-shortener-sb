package com.url.shortener.repository;

import com.url.shortener.entity.ClickEvent;
import com.url.shortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByUrlMappingAndClickDateBetween(UrlMapping urlMapping, LocalDateTime start, LocalDateTime end);
    List<ClickEvent> findByUrlMappingInAndClickDateBetween(
            List<UrlMapping> urlMappings, LocalDateTime start, LocalDateTime end
    );
}
