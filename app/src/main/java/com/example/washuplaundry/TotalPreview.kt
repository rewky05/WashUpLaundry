package com.example.washuplaundry

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

class TotalPreview : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var orderViewModel: OrderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("order_prefs", Context.MODE_PRIVATE)
        orderViewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_total_preview, container, false)

        val totalPrice = arguments?.getDouble("totalPrice") ?: 0.0

        val orderItemsJson = arguments?.getString("orderItems")
        if (orderItemsJson != null) {
            val gson = Gson()
            val orderItems: ArrayList<OrderData> = gson.fromJson(orderItemsJson, Array<OrderData>::class.java).toList() as ArrayList<OrderData>

            Log.d("OrderItems Size", "Size of orderItems: ${orderItems.size}")

            val orderItemsList = view.findViewById<RecyclerView>(R.id.order_items_list)
                orderItemsList.layoutManager = LinearLayoutManager(context)

            if (orderItems != null && orderItems.isNotEmpty()) {
                    val adapter = OrderDataAdapter(orderItems)
                    orderItemsList.adapter = adapter

                } else {
                    // Handle the case where there are no order items (e.g., show a message)
                }
        }

        val totalPriceTextView = view.findViewById<TextView>(R.id.total_price)
        totalPriceTextView.text = "Total Price: $totalPrice"

        val orderItemsList = view.findViewById<RecyclerView>(R.id.order_items_list)
        orderItemsList.layoutManager = LinearLayoutManager(context)

        // ViewModel Observers
        orderViewModel.totalPrice.observe(viewLifecycleOwner) { totalPrice ->
            totalPriceTextView.text = "Total Price: $totalPrice"
        }

        orderViewModel.orderItems.observe(viewLifecycleOwner) { orderItems ->
            if (orderItems != null) {
                orderItemsList.adapter = OrderDataAdapter(orderItems)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<ImageButton>(R.id.backButton).visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<ImageButton>(R.id.backButton).visibility = View.GONE
    }

}
