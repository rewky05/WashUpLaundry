package com.example.washuplaundry

import java.io.Serializable

data class Service(
    val id: String = "",
    val name: String,
    val price: Double,
    val kilo: Double? = null,
    val load: Int? = null,
    val pcs: Int? = null
) : Serializable {
    constructor() : this("", "", 0.0)
}
