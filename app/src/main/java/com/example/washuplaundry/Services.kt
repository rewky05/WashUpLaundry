package com.example.washuplaundry
import java.io.Serializable

data class Services(
    val regular: Map<String, Service> = emptyMap(),
    val dryClean: Map<String, Service> = emptyMap(),
    val selfService: Map<String, Any?> = emptyMap()
) : Serializable
