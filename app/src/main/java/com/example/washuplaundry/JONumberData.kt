package com.example.washuplaundry

data class JONumberData(
    val joNumber: String = "",
    val timestamp: String? = "",
    val total: String? = "",
    val userName: String? = null,
    val details: OrderDetails = OrderDetails()
)






