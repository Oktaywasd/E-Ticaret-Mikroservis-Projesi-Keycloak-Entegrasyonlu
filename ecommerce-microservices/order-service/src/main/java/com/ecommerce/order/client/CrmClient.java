package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.AddressDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "crm-service", url = "${application.config.crm-service-url}")
public interface CrmClient {

    // ✅ /crm eklendi:
    @GetMapping("/api/v1/addresses/{addressId}")
    AddressDto getAddressById(@PathVariable("addressId") String addressId);
}