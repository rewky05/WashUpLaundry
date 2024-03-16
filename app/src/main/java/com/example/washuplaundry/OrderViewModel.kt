package com.example.washuplaundry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger

class OrderViewModel : ViewModel() {
    private val _prices = MutableLiveData<List<Double>>(emptyList())
    private val prices: LiveData<List<Double>> = _prices

    private val dailyOrderCounter = AtomicInteger(0)

    private val _totalPrice = MutableLiveData<Double>(0.0) // Initialize with a default value
    val totalPrice: LiveData<Double> = _totalPrice

    val _orderItems = MutableLiveData<List<OrderData>>(emptyList())
    val orderItems: LiveData<List<OrderData>> = _orderItems

    fun addNewOrder() {
        val updatedTotalPrice = _orderItems.value?.sumOf { it.subtotal } ?: 0.0
        _totalPrice.postValue(updatedTotalPrice)
    }

    fun addOrderItem(orderItem: OrderData) {
        val currentList = _orderItems.value?.toMutableList() ?: mutableListOf()
        currentList.add(orderItem)
        _orderItems.value = currentList
    }

//    fun resetTotalPrice() {
//        _totalPrice.postValue(0.0)
//    }
}
