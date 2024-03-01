package com.example.washuplaundry

data class DryCleanList(
    val service: ServiceList,
    val load: Int = 0,
    val pcs: Int = 0
) {
    constructor() : this(ServiceList(), 0, 0)
}