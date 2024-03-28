package com.example.washuplaundry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OrderViewModel : ViewModel() {
    private val _totalPrice = MutableLiveData<Double>(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    private val _orderItems = MutableLiveData<List<OrderItemsData>>(emptyList())
    val orderItems: LiveData<List<OrderItemsData>> = _orderItems

    fun totalOrder() {
        val orderDataTotal = _orderItems.value?.flatMap { it.orderData }?.sumOf { it.itemSubtotal } ?: 0.0
        val selfServiceOrderDataTotal = _orderItems.value?.flatMap { it.selfServiceOrderData }?.sumOf { it.itemSubtotal } ?: 0.0
        val dryCleanOrderDataTotal = _orderItems.value?.flatMap { it.dryCleanOrderData }?.sumOf { it.itemSubtotal } ?: 0.0

        _totalPrice.value = orderDataTotal + selfServiceOrderDataTotal + dryCleanOrderDataTotal
    }

    fun addOrderItem(orderItem: OrderItemsData) {
        val currentList = _orderItems.value?.toMutableList() ?: mutableListOf()
        currentList.add(orderItem)
        _orderItems.value = currentList
    }

    fun resetTotalPrice() {
        _orderItems.value = emptyList()
        _totalPrice.value = 0.0
    }
}
