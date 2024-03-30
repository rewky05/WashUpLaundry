package com.example.washuplaundry

data class HistoryDataRow(
    val date: String,
    val joNumber: String,
    val timestamp: String?,
    val userName: String = "",
    val orderItems: List<OrderItemsData>,
    val totalPrice: Double,
    var isExpanded: Boolean = false
)
