package com.example.washuplaundry

data class OrderItemsData(
    val orderData: List<OrderData>,
    val selfServiceOrderData: List<SelfServiceOrderData>,
    val dryCleanOrderData: List<DryCleanOrderData>
)