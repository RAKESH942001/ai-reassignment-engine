package com.ziprun.reassignment.model;

/**
 * Order lifecycle: ASSIGNED → REASSIGNMENT_PENDING → REASSIGNED → DELIVERED
 */
public enum OrderStatus {
    ASSIGNED,
    REASSIGNMENT_PENDING,
    REASSIGNED,
    DELIVERED
}
