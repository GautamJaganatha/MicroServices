package com.microserrvices.OrderService.service;

import com.microserrvices.OrderService.model.OrderRequest;
import com.microserrvices.OrderService.model.OrderResponse;

public interface OrderService {

    Long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(Long orderId);
}
