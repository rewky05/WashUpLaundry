package com.example.washuplaundry

data class RegularServiceList(
    val service: ServiceList,
    val kilo: Double = 0.0
) {
    constructor() : this(ServiceList(), 0.0)
}
