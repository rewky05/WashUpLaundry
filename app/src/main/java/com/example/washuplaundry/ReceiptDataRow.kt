package com.example.washuplaundry

data class ReceiptDataRow(
    val joNumber: String,
    val timestamp: String?,
    val orderItems: List<OrderData>,
    val totalPrice: Double,
    var isExpanded: Boolean = false
)
