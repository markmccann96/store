package com.example.store.entity;

/**
 * This is used in the projection from the database of the orders to create the OrderDTO for the order request.
 */
public interface OrderRow {
    Long getId();
    String getDescription();
    Long getCustomerId();
    String getCustomerName();
}
