package com.ecommerce.productcatalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionCreateRequest {
    @NotBlank(message = "Soru metni boş olamaz")
    private String question;
}