package com.example.washuplaundry

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DryCleanOrderData (
    val itemName: String,
    val itemPrice: Double,
    var itemPcs: Double,
    var itemSubtotal: Double,
    val userName: String = "",
    val userId: String = ""
) : OrderItem(itemName, itemPrice, 0.0, 0.0, itemPcs, itemSubtotal) {
    constructor() : this("", 0.0, 0.0, 0.0, "", "")
}