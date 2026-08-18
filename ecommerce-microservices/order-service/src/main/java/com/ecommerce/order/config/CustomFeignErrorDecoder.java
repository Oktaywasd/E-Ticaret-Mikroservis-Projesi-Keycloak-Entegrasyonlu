package com.ecommerce.order.config;

import com.ecommerce.order.exception.BusinessException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ResourceNotFoundException("Requested resource was not found in external service.");
        }
        if (response.status() >= 400 && response.status() <= 499) {
            return new BusinessException("External service client error: " + response.reason());
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}