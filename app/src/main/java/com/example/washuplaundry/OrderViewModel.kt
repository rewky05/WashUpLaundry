package com.example.washuplaundry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class OrderViewModel : ViewModel() {
    private val _prices = MutableLiveData<List<Double>>(emptyList())
    private val prices: LiveData<List<Double>> = _prices

    val totalPrice: LiveData<Double> = prices.map { it.sum() }

    private val _orderItems = MutableLiveData<MutableList<OrderData>>(mutableListOf())
    val orderItems: LiveData<MutableList<OrderData>> = _orderItems

    // Update total price by adding a new price
    fun addNewOrder(newPrice: Double) {
        _prices.value = _prices.value?.plus(newPrice) ?: listOf(newPrice)
    }

    fun addOrderItem(orderItem: OrderData) {
        _orderItems.value?.add(orderItem)
        _orderItems.value = _orderItems.value
    }
}
