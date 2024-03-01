package com.example.washuplaundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DryClean(private val subServices: List<ServiceList>) : Fragment() {

    var dryClean: List<DryCleanList> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dry_clean, container, false)
    }
}