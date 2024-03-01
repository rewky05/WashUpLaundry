package com.example.washuplaundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SelfService(private val subServices: List<ServiceList>) : Fragment() {

    var selfService: List<SelfServiceList> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_self_service, container, false)
    }
}