package com.ecommerce.productcatalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminReplyRequest {
    @NotBlank(message = "Yanıt metni boş olamaz")
    private String replyText;
}