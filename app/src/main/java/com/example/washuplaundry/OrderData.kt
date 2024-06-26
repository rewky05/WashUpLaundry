package com.example.washuplaundry

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class OrderData(
    val itemName: String,
    val itemPrice: Double,
    val itemKilo: Double,
    var itemSubtotal: Double,
    val userName: String = "",
    val userId: String = ""
) : OrderItem(itemName, itemPrice, itemKilo, 0.0, 0.0, itemSubtotal) {
    constructor() : this("", 0.0, 0.0, 0.0, "", "")
}



