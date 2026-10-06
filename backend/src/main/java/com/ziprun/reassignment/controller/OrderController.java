package com.ziprun.reassignment.controller;

import com.ziprun.reassignment.dto.CreateOrderRequest;
import com.ziprun.reassignment.model.Order;
import com.ziprun.reassignment.model.OrderStatus;
import com.ziprun.reassignment.model.ReassignmentSuggestion;
import com.ziprun.reassignment.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(value = "status", required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrders(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable("id") String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping("/{id}/suggest")
    public ResponseEntity<ReassignmentSuggestion> generateSuggestion(@PathVariable("id") String id) {
        ReassignmentSuggestion suggestion = orderService.createSuggestionForOrder(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(suggestion);
    }
}
