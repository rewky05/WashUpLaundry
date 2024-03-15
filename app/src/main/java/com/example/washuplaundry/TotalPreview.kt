package com.example.washuplaundry

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

class TotalPreview : Fragment() {

    private lateinit var orderViewModel: OrderViewModel
    private lateinit var adapter: OrderDataAdapter
    private lateinit var totalPriceTextView: TextView

    // Firebase References
    private val joNumberRef = Firebase.database.getReference("currentJONumber")
    private val databaseRef = Firebase.database.getReference("Receipts/Unpaid")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderViewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_total_preview, container, false)

        totalPriceTextView = view.findViewById(R.id.total_price)
        val totalPrice = arguments?.getDouble("totalPrice") ?: 0.0

        val orderItemsJson = arguments?.getString("orderItems")
        if (orderItemsJson != null) {
            val gson = Gson()
            val orderItems: ArrayList<OrderData> = gson.fromJson(orderItemsJson, Array<OrderData>::class.java).toList() as ArrayList<OrderData>

            Log.d("OrderItems Size", "Size of orderItems: ${orderItems.size}")

            val orderItemsList = view.findViewById<RecyclerView>(R.id.order_items_list)
                orderItemsList.layoutManager = LinearLayoutManager(context)

            if (orderItems != null && orderItems.isNotEmpty()) {
                adapter.orderItems = orderItems
                adapter.notifyDataSetChanged()
            } else {
                    // No order items
                }
        }

        val totalPriceTextView = view.findViewById<TextView>(R.id.total_price)
        totalPriceTextView.text = "Total: ₱$totalPrice"

        val orderItemsList = view.findViewById<RecyclerView>(R.id.order_items_list)
        orderItemsList.layoutManager = LinearLayoutManager(context)

        // ViewModel Observers
        orderViewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]
        adapter = OrderDataAdapter(emptyList())
        orderItemsList.adapter = adapter

        orderViewModel.orderItems.observe(viewLifecycleOwner) { orderItems ->
            adapter.orderItems = orderItems
            adapter.notifyDataSetChanged()
        }

        orderViewModel.totalPrice.observe(viewLifecycleOwner) { totalPrice ->
            totalPriceTextView.text = "Total: ₱${totalPrice}"
            updateTotalPriceDisplay()
        }

        val btnCharge = view.findViewById<Button>(R.id.btnCharge)
        btnCharge.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                fetchUserNameAndSaveOrder(userId) { userName, userId ->
                    val extractedOrderData = collectOrderItemsFromUI()

                    joNumberRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var joNumber = currentData.getValue(Int::class.java) ?: 80
                            joNumber++
                            currentData.value = joNumber
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(
                            error: DatabaseError?,
                            committed: Boolean,
                            snapshot: DataSnapshot?
                        ) {
                            if (committed) {
                                val newJONumber = snapshot?.getValue(Int::class.java) ?: 80

                                val totalPrice = extractedOrderData.sumOf { it.subtotal }
                                fetchUserName(userId) { userName ->
                                    saveOrderDataToRealtime(
                                        extractedOrderData,
                                        newJONumber,
                                        userName,
                                        totalPrice
                                    )
                                }

                                orderViewModel._orderItems.value = emptyList()
                                orderViewModel.addNewOrder()
                                Log.d("Receipt saved", "Receipt saved!!!")
                            } else {

                            }
                        }
                    })
                }
            }
        }

        return view
    }

    private fun collectOrderItemsFromUI(): List<OrderData> {
        Log.d("Order Collection", "Collecting order items")
        val extractedOrderData = mutableListOf<OrderData>()
        val recyclerView = view?.findViewById<RecyclerView>(R.id.order_items_list)

        if (recyclerView != null) {
            for (index in 0 until recyclerView.childCount) {
                val holder =
                    recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as OrderDataAdapter.OrderDataViewHolder
                val name = holder.name.text.toString()
                val price = holder.price.text.toString().removePrefix("₱").toDouble()
                val weightString = holder.kilo.text.toString() // Assuming this contains "3.00 kg"
                val numericalWeightString = weightString.substring(0, weightString.indexOf(" kg"))
                val weight = numericalWeightString.toDouble()
                val subTotal = holder.subTotal.text.toString().removePrefix("₱").toDouble()
                val totalPrice = orderViewModel.totalPrice.value ?: 0.0

                extractedOrderData.add(OrderData(name, price, weight, subTotal, totalPrice))
            }
        } else {
            // RecyclerView not found
        }

        return extractedOrderData
    }

    private fun updateTotalPriceDisplay() {
        val totalPrice = orderViewModel.totalPrice.value ?: 0.0
        totalPriceTextView.text = "Total: ₱$totalPrice"
    }

    private fun fetchUserName(userId: String, callback: (userName: String) -> Unit) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).child("userName")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                val userName = dataSnapshot.getValue(String::class.java) ?: "" // Handle potential null
                callback(userName)
            }
            .addOnFailureListener {

            }
    }

    private fun fetchUserNameAndSaveOrder(userId: String, callback: (userName: String, userId: String) -> Unit) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).child("userName")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                val userName = dataSnapshot.getValue(String::class.java)
                if (userName != null) {
                    callback(userName, userId) // Call the callback to initiate saving
                }
            }
    }

    private fun saveOrderDataToRealtime(
        orderData: List<OrderData>,
        newJONumber: Int,
        userName: String,
        totalPrice: Double
    ) {
        Log.d("Saving Order", "Saving order data to Firebase")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val databaseRef = databaseRef.child("JO-$newJONumber") // Keep the JO reference

            // Convert the orderData to a suitable format for Realtime Database
            val orderDataMap = orderData.map { it.toDatabaseMap(userId, userName) }
            databaseRef.child("totalPrice").setValue(totalPrice) // Add the totalPrice

            // Firebase update: No changes needed here
            databaseRef.setValue(orderDataMap)
                .addOnSuccessListener {
                    Log.d("Receipt saved", "Receipt saved!!!") // Adjust log message if needed
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase Save Error", "Error saving data", exception)
                }
        }
        val orderMap = mutableMapOf<String, Any?>(
            "totalPrice" to totalPrice
        )

        for ((index, item) in orderData.withIndex()) {
            val itemMap = item.toDatabaseMap(userId!!, userName)
            orderMap[index.toString()] = mapOf(
                "name" to (itemMap["name"] as? String ?: ""),
                "price" to (itemMap["price"] as? Double ?: 0.0),
                "kilo" to (itemMap["kilo"] as? Double ?: 0.0),
                "subtotal" to (itemMap["subtotal"] as? Double ?: 0.0),
                "userName" to userName,
                "userId" to userId
            )
        }

        databaseRef.child("JO-$newJONumber").setValue(orderMap)
    }

    private fun OrderData.toDatabaseMap(userId: String, userName: String): Map<String, Any?> {
        return mapOf (
            "name" to name,
            "price" to price,
            "kilo" to kilo,
            "subtotal" to subtotal,
            "totalPrice" to totalPrice,
            "userName" to userName,
            "userId" to userId
        )
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
