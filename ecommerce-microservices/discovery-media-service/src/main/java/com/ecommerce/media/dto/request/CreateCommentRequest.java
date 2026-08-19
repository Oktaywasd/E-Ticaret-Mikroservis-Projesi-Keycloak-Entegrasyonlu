package com.ecommerce.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Yorum içeriği boş olamaz")
    @Size(max = 500, message = "Yorum en fazla 500 karakter olabilir")
    private String content;
}