package com.ziprun.reassignment.repository;

import com.ziprun.reassignment.model.Order;
import com.ziprun.reassignment.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByAssignedAgentIdAndStatus(String assignedAgentId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByAssignedAgentId(String assignedAgentId);
}
