package com.ecommerce.productcatalog.controller;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import com.ecommerce.productcatalog.dto.PriceDto;
import com.ecommerce.productcatalog.dto.StockDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse productResponse;
    private ProductCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id("prod_101")
                .productCode("PRD-001")
                .name("Oversize Tişört")
                .isActive(true)
                .build();

        PriceDto priceDto = PriceDto.builder()
                .sellingPrice(BigDecimal.valueOf(250.00))
                .build();

        StockDto stockDto = StockDto.builder()
                .currentStock(20)
                .minimumStock(0)
                .build();

        validCreateRequest = ProductCreateRequest.builder()
                .productCode("PRD-001")
                .categoryId("cat_123")
                .name("Oversize Tişört")
                .brand("Nike")
                .price(priceDto)
                .stock(stockDto)
                .build();
    }

    @Test
    @DisplayName("Public Endpoint: Anonim (Token'sız) kullanıcı ürün detayını 200 OK ile alabilmeli")
    void getProductById_WhenAnonymousUser_ShouldReturn200Ok() throws Exception {
        when(productService.getProductById("prod_101")).thenReturn(productResponse);

        mockMvc.perform(get("/api/v1/products/prod_101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod_101"))
                .andExpect(jsonPath("$.productCode").value("PRD-001"));

        verify(productService, times(1)).getProductById("prod_101");
    }

    @Test
    @DisplayName("Başarılı Ürün Ekleme: Geçerli istekte 201 Created dönmeli")
    @WithMockUser(roles = "SELLER")
    void createProduct_WhenValidRequest_ShouldReturn201Created() throws Exception {
        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(productResponse);

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("prod_101"));

        verify(productService, times(1)).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("DTO Bean Validation: Boş/Geçersiz alanlar gönderildiğinde 400 Bad Request dönmeli")
    @WithMockUser(roles = "SELLER")
    void createProduct_WhenInvalidDto_ShouldReturn400BadRequest() throws Exception {
        ProductCreateRequest invalidRequest = new ProductCreateRequest(); // Boş DTO

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("RBAC Yetkisiz Erişim: ROLE_CUSTOMER ile toggle-status çağrıldığında 403 Forbidden dönmeli")
    @WithMockUser(roles = "CUSTOMER")
    void toggleProductStatus_WhenCustomerRole_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/products/prod_101/toggle-status")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(productService, never()).toggleProductStatus(anyString());
    }

    @Test
    @DisplayName("RBAC Yetkili Erişim: ROLE_SELLER ile toggle-status çağrıldığında 200 OK dönmeli")
    @WithMockUser(roles = "SELLER")
    void toggleProductStatus_WhenSellerRole_ShouldReturn200Ok() throws Exception {
        when(productService.toggleProductStatus("prod_101")).thenReturn(productResponse);

        mockMvc.perform(patch("/api/v1/products/prod_101/toggle-status")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod_101"));

        verify(productService, times(1)).toggleProductStatus("prod_101");
    }

    @Test
    @DisplayName("RBAC Admin Yetkili Erişim: ROLE_ADMIN ile toggle-status çağrıldığında 200 OK dönmeli")
    @WithMockUser(roles = "ADMIN")
    void toggleProductStatus_WhenAdminRole_ShouldReturn200Ok() throws Exception {
        when(productService.toggleProductStatus("prod_101")).thenReturn(productResponse);

        mockMvc.perform(patch("/api/v1/products/prod_101/toggle-status")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod_101"));

        verify(productService, times(1)).toggleProductStatus("prod_101");
    }
}