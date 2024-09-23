package com.microservices.ProductService.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class ProductRequest {
    private String name;
    private Long price;
    private Long quantity;
}
