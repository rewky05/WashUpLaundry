package com.example.washuplaundry

data class OrderDetails(
    val totalPrice: Double = 0.0,
    var userName: String = "",
    val orderItems: List<OrderItemsData> = emptyList()
)
