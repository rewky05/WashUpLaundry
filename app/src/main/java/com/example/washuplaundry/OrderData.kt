package com.example.washuplaundry

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class OrderData(
    val name: String,
    val price: Double,
    var kilo: Double,
    var subtotal: Double,
    var totalPrice: Double,
    val userName: String = "",
    val userId: String = ""
)
