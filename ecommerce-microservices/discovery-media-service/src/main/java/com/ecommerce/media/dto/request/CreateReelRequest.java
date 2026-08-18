package com.ecommerce.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReelRequest {
    @NotBlank(message = "Başlık boş olamaz")
    private String title;

    private String description;

    @NotBlank(message = "Ürün ID boş olamaz")
    private String productId;

    private Integer durationInSeconds;
}