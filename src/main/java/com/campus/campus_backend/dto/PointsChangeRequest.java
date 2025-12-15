package com.campus.campus_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointsChangeRequest {
    @NotNull(message = "amount required")
    private Integer amount;
    @NotNull(message = "type required")
    private String type;
    private String description;
}
