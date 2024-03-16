package com.example.washuplaundry

import com.google.firebase.database.IgnoreExtraProperties
import java.math.BigDecimal

@IgnoreExtraProperties
data class OrderData(
    val name: String,
    val price: Double,
    var kilo: Double,
    var subtotal: Double,
    val userName: String = "",
    val userId: String = "",
    var totalPrice: BigDecimal = BigDecimal.ZERO
)
