package com.microserrvices.OrderService.service;

import com.microserrvices.OrderService.entity.Order;
import com.microserrvices.OrderService.execption.CustomExecption;
import com.microserrvices.OrderService.external.client.PaymentService;
import com.microserrvices.OrderService.external.client.ProductService;
import com.microserrvices.OrderService.external.request.PaymentRequest;
import com.microserrvices.OrderService.external.response.PaymentResponse;
import com.microserrvices.OrderService.model.OrderRequest;
import com.microserrvices.OrderService.model.OrderResponse;
import com.microserrvices.OrderService.model.PaymentMode;
import com.microserrvices.OrderService.repository.OrderRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class OrderServiceImplTest {


    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    OrderService orderService = new OrderServiceImpl();


    @DisplayName("Get Order - Success Scenario")
    @Test
    void test_When_Order_Success() {
        //Mocking
        Order order = getMockOrder();
        when(orderRepository.findById(anyLong()))
                .thenReturn(Optional.of(order));

        when(restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                OrderResponse.ProductDetails.class
        )).thenReturn(getMockProductResponse());

        when(restTemplate.getForObject(
                "http://PAYMENT-SERVICE/payment/order/" + 1,
                PaymentResponse.class
        )).thenReturn(getMockPaymentResponse());

        // Actual
        OrderResponse orderResponse = orderService.getOrderDetails(1L);

        //verification
        verify(orderRepository, times(1)).findById(anyLong());
        verify(restTemplate, times(1)).getForObject(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                OrderResponse.ProductDetails.class);
        verify(restTemplate, times(1)).getForObject(
                "http://PAYMENT-SERVICE/payment/order/" + 1,
                PaymentResponse.class);

        //Assert
        assertNotNull(orderResponse);
        assertEquals(order.getId(), orderResponse.getOrderId());
    }

    @DisplayName("Get Order - Failure Scenario")
    @Test
    void test_When_GET_Order_NOT_FOUND_then_ORDER_NOT_Found() {

        when(orderRepository.findById(anyLong()))
                .thenReturn(Optional.ofNullable(null));

        //Assert
        CustomExecption exception = assertThrows(CustomExecption.class, () -> orderService.getOrderDetails(1L));
        assertEquals("NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getStatus());

        verify(orderRepository, times(1)).findById(anyLong());
    }




    @DisplayName("Place Order - Success Scenario")
    @Test
    void test_When_Place_Order_Success(){

        Order order = getMockOrder();

        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        when(productService.reduceQuantity(anyLong(),anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(any(PaymentRequest.class)))
                .thenReturn(new ResponseEntity<Long>(1L,HttpStatus.OK));
        Long orderId = orderService.placeOrder(orderRequest);

        verify(orderRepository, times(2))
                .save(any());

        verify(productService,times(1))
                .reduceQuantity(anyLong(),anyLong());

        verify(paymentService, times(1))
                .doPayment(any(PaymentRequest.class));

        assertEquals(order.getId(),orderId);
    }

    @DisplayName("Place Order - Payment Failed Scenario")
    @Test
    void test_when_Place_Order_Payment_Fails_then_Order_Placed(){
        Order order = getMockOrder();

        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        when(productService.reduceQuantity(anyLong(),anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException());
        Long orderId = orderService.placeOrder(orderRequest);

        verify(orderRepository, times(2))
                .save(any());

        verify(productService,times(1))
                .reduceQuantity(anyLong(),anyLong());

        verify(paymentService, times(1))
                .doPayment(any(PaymentRequest.class));

        assertEquals(order.getId(),orderId);
    }

    private OrderRequest getMockOrderRequest() {
        return OrderRequest.builder()
                .productId(1L)
                .quantity(10L)
                .paymentMode(PaymentMode.CASH)
                .totalAmount(100L)
                .build();
    }

    private PaymentResponse getMockPaymentResponse() {
        return PaymentResponse.builder()
                .paymentId(1L)
                .paymentDate(Instant.now())
                .paymentMode(PaymentMode.CASH)
                .amount(200L)
                .orderId(1L)
                .status("ACCEPTED")
                .build();
    }

    private OrderResponse.ProductDetails getMockProductResponse() {
        return OrderResponse.ProductDetails.builder()
                .productId(2L)
                .productName("iPhone")
                .price(100L)
                .quantity(200L)
                .build();
    }

    private Order getMockOrder() {
        return Order.builder()
                .orderStatus("PLACED")
                .orderDate(Instant.now())
                .id(1L)
                .amount(100L)
                .quantity(200L)
                .productId(2L)
                .build();
    }
}