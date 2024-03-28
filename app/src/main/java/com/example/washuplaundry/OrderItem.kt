package com.example.washuplaundry

open class OrderItem(
    val name: String,
    val price: Double,
    val kilo: Double = 0.0,
    val loadOrPcs: Double = 0.0,
    val pcs: Double = 0.0,
    val subtotal: Double = 0.0
)
