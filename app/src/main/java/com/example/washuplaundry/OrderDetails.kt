package com.example.washuplaundry

import java.math.BigDecimal

data class OrderDetails(
    val totalPrice: BigDecimal = BigDecimal.ZERO,
    val orderItems: List<OrderData> = emptyList()
)
