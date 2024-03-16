package com.example.washuplaundry

import java.math.BigDecimal

data class ReceiptDataRow(
    val joNumber: String,
    val timestamp: String?,
    val orderItems: List<OrderData>,
    val totalPrice: BigDecimal,
    var isExpanded: Boolean = false
)
