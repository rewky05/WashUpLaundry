package com.example.washuplaundry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OrderViewModel : ViewModel() {
    private val _totalPrice = MutableLiveData<Double>(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    private val _orderItems = MutableLiveData<MutableList<OrderData>>(mutableListOf())
    val orderItems: LiveData<MutableList<OrderData>> = _orderItems

    fun updateTotalPrice(newTotalPrice: Double) {
        _totalPrice.value = newTotalPrice
    }

    fun addOrderItem(orderItem: OrderData) {
        _orderItems.value?.add(orderItem)
        _orderItems.value = _orderItems.value
    }
}
