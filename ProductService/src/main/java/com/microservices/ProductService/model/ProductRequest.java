package com.microservices.ProductService.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductRequest {
    private String name;
    private Long price;
    private Long quantity;
}
