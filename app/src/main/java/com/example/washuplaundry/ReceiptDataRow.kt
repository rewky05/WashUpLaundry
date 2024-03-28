package com.example.washuplaundry

data class ReceiptDataRow(
    val joNumber: String,
    val timestamp: String?,
    val userName: String = "",
    val orderItems: List<OrderItemsData>,
    val totalPrice: Double,
    var isExpanded: Boolean = false
)
