package com.example.washuplaundry

data class ServiceList(
    val name: String = "",
    val price: Double = 0.0
) {
    constructor() : this("", 0.0)
}
