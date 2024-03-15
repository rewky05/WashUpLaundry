package com.example.washuplaundry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class OrderViewModel : ViewModel() {
    private val _prices = MutableLiveData<List<Double>>(emptyList())
    private val prices: LiveData<List<Double>> = _prices

    val totalPrice: LiveData<Double> = prices.map { it.sum() }

    val _orderItems = MutableLiveData<List<OrderData>>(emptyList())
    val orderItems: LiveData<List<OrderData>> = _orderItems

    // Update total price by adding a new price
//    fun addNewOrder(newPrice: Double) {
//        _prices.value = _prices.value?.plus(newPrice) ?: listOf(newPrice)
//    }

    fun addNewOrder(newPrice: Double = 0.0) { // Add default value 0.0
        _prices.value = listOf(newPrice) // Reset and set initial price if needed
    }

    fun addOrderItem(orderItem: OrderData) {
        val currentList = _orderItems.value?.toMutableList() ?: mutableListOf()
        currentList.add(orderItem)
        _orderItems.value = currentList
    }
}
