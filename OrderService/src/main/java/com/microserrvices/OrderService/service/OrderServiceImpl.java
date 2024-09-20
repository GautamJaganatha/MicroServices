package com.microserrvices.OrderService.service;

import com.microserrvices.OrderService.entity.Order;
import com.microserrvices.OrderService.execption.CustomExecption;
import com.microserrvices.OrderService.external.client.PaymentService;
import com.microserrvices.OrderService.external.client.ProductService;
import com.microserrvices.OrderService.external.request.PaymentRequest;
import com.microserrvices.OrderService.external.response.PaymentResponse;
import com.microserrvices.OrderService.model.OrderRequest;
import com.microserrvices.OrderService.model.OrderResponse;
import com.microserrvices.OrderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;


    @Override
    public Long placeOrder(OrderRequest orderRequest) {

        //Order Entity -> Save the data with Status Order Created.
        //Product Service -> Block Products (Reduce the quantity).
        //Payment Service -> Payments -> Success -> COMPLETE, Else
        //CANCELLED

        log.info("Placing Order Request: {}", orderRequest);

        productService.reduceQuantity(orderRequest.getProductId(),
                orderRequest.getQuantity());

        log.info("Creating order with status CREATED");

        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .productId(orderRequest.getProductId())
                .build();

        order = orderRepository.save(order);

        log.info("Calling payment Service to complete the payment");

        PaymentRequest paymentRequest =
                PaymentRequest.builder()
                        .orderId(order.getId())
                        .paymentMode(orderRequest.getPaymentMode())
                        .amount(orderRequest.getTotalAmount())
                        .build();

        String orderStatus;
        try {
            paymentService.doPayment(paymentRequest);
            log.info("Payment done Successfully, Changing the Order status to PLACED");
            orderStatus= "PLACED";
        } catch (Exception e){
            log.info("Error occurred in Payment, Changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }
        order.setOrderStatus(orderStatus);

        orderRepository.save(order);

        log.info("Order Placed Successfully with Order Id: {}",order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(Long orderId) {
        log.info("Get order details for Order Id : {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomExecption("Order not found for the Order ID: "+ orderId,
                        "NOT_FOUND",
                        404));

        log.info("Invoking Product Service to fetch the product for Id: "+ order.getProductId());

        OrderResponse.ProductDetails productDetails = restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                OrderResponse.ProductDetails.class
        );

        log.info("Getting Payment information form the Payment Response");
        PaymentResponse paymentResponse =
                restTemplate.getForObject(
                        "http://PAYMENT-SERVICE/payment/order/"+ order.getId(),
                        PaymentResponse.class
                        );



        assert paymentResponse != null;
        OrderResponse.PaymentDetails paymentDetails =
                OrderResponse.PaymentDetails.builder()
                        .paymentId(paymentResponse.getPaymentId())
                        .paymentDate(paymentResponse.getPaymentDate())
                        .paymentMode(paymentResponse.getPaymentMode())
                        .status(paymentResponse.getStatus())
                        .build();

        OrderResponse orderResponse =
                 OrderResponse.builder()
                         .orderId(order.getId())
                         .orderStatus(order.getOrderStatus())
                         .amount(order.getAmount())
                         .orderDate(order.getOrderDate())
                         .productDetails(productDetails)
                         .paymentDetails(paymentDetails)
                         .build();

        return orderResponse;
    }
}
