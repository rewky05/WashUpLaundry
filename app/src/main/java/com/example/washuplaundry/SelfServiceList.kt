package com.example.washuplaundry

data class SelfServiceList(
    val service: ServiceList,
    val kilo: Double = 0.0
) {
    constructor() : this(ServiceList(), 0.0)
}