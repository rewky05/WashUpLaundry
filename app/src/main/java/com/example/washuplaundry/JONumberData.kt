package com.example.washuplaundry

data class JONumberData(
    val joNumber: String = "",
    val timestamp: String? = "",
    val total: String? = "",
    val details: OrderDetails = OrderDetails()
)






