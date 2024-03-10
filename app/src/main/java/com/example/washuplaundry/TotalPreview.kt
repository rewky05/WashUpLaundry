package com.example.washuplaundry

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TotalPreview : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var totalPriceTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("order_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_total_preview, container, false)

        totalPriceTextView = view.findViewById(R.id.total_price)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayTotalPrice()
    }

    private fun displayTotalPrice() {
        val totalPrice = sharedPreferences.getFloat("totalPrice", 0f)
        totalPriceTextView.text = "Total: ₱" + "$totalPrice" // Update your TextView
    }
}
