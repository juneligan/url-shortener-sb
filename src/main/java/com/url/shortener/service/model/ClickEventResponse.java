package com.url.shortener.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class ClickEventResponse {
    private LocalDate clickDate;
    private Long count;
}
