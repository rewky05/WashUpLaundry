package com.example.washuplaundry

data class OrderDetails(
    val totalPrice: Double = 0.0,
    val orderItems: List<OrderData> = emptyList()
)
