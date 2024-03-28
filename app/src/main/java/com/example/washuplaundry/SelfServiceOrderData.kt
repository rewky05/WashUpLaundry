package com.example.washuplaundry

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SelfServiceOrderData(
    val itemName: String,
    val itemPrice: Double,
    var itemLoadOrPcs: Double,
    var itemSubtotal: Double,
    val userName: String = "",
    val userId: String = ""
) : OrderItem(itemName, itemPrice, 0.0, itemLoadOrPcs, 0.0,  itemSubtotal) {
    constructor() : this("", 0.0, 0.0, 0.0, "", "")
}

