package com.campusbite.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CanteenClosureRequest(
        @NotBlank String date,
        @NotBlank @Size(max = 200) String reason
) {}
