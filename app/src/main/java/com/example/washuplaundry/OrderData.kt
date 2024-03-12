package com.example.washuplaundry

data class OrderData(
    val name: String,
    val price: Double,
    var kilo: Double,
    var subtotal: Double,
    var quantity: Int = 1
)
