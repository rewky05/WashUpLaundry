package com.example.washuplaundry

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TotalPreview : Fragment() {

    private lateinit var orderViewModel: OrderViewModel
    private lateinit var combinedAdapter: CombinedOrderDataAdapter
    private lateinit var totalPriceTextView: TextView

    private val joNumberRef = Firebase.database.getReference("currentJONumber")
    private val databaseRef = Firebase.database.getReference("Receipts")

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

        val recyclerView = view.findViewById<RecyclerView>(R.id.order_items_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        combinedAdapter = CombinedOrderDataAdapter()
        recyclerView.adapter = combinedAdapter

        orderViewModel.orderItems.observe(viewLifecycleOwner) { orderItemsData ->
            val combinedOrderItems = mutableListOf<OrderItem>()

            orderItemsData.forEach { orderItemData ->
                combinedOrderItems.addAll(orderItemData.orderData)
                combinedOrderItems.addAll(orderItemData.selfServiceOrderData)
                combinedOrderItems.addAll(orderItemData.dryCleanOrderData)
            }

            combinedAdapter.updateData(combinedOrderItems)
        }

        orderViewModel.totalPrice.observe(viewLifecycleOwner) { totalPrice ->
            totalPriceTextView.text = "Total: ₱${String.format("%.2f", totalPrice)}"
        }

        val btnCharge = view.findViewById<Button>(R.id.btnCharge)
        btnCharge.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                fetchUserNameAndSaveOrder(userId) { userName, userId ->
                    val orderItemsData = collectOrderItemsFromUI()

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

                                val totalPrice =
                                    orderItemsData.orderData.sumOf { it.subtotal } +
                                            orderItemsData.selfServiceOrderData.sumOf { it.subtotal } +
                                            orderItemsData.dryCleanOrderData.sumOf { it.subtotal }

                                Log.d("totalPrice in preview", "$totalPrice")

                                if (totalPrice > 0.0) {

                                    fetchUserName(userId) { userName ->
                                        saveOrderDataToRealtime(
                                            orderItemsData.orderData,
                                            orderItemsData.selfServiceOrderData,
                                            orderItemsData.dryCleanOrderData,
                                            newJONumber,
                                            userName,
                                            totalPrice
                                        )
                                    }
                                    combinedAdapter.clearLists()
                                    orderViewModel.resetTotalPrice()
                                } else {
                                    Toast.makeText(context, "There are no added orders yet", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
        }

        val btnChargeNow = view.findViewById<MaterialButton>(R.id.btnChargeNow)
        btnChargeNow.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                fetchUserNameAndSaveOrder(userId) { userName, userId ->
                    val orderItemsData = collectOrderItemsFromUI()

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

                                val totalPrice =
                                    orderItemsData.orderData.sumOf { it.subtotal } +
                                            orderItemsData.selfServiceOrderData.sumOf { it.subtotal } +
                                            orderItemsData.dryCleanOrderData.sumOf { it.subtotal }

                                Log.d("totalPrice in preview", "$totalPrice")

                                if (totalPrice > 0.0) {

                                    fetchUserName(userId) { userName ->
                                        saveOrderDataToRealtimePaid(
                                            orderItemsData.orderData,
                                            orderItemsData.selfServiceOrderData,
                                            orderItemsData.dryCleanOrderData,
                                            newJONumber,
                                            userName,
                                            totalPrice
                                        )
                                    }
                                    combinedAdapter.clearLists()
                                    orderViewModel.resetTotalPrice()
                                } else {
                                    Toast.makeText(context, "There are no added orders yet", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
        }

        return view
    }

    private fun collectOrderItemsFromUI(): OrderItemsData {
        val extractedOrderData = mutableListOf<OrderData>()
        val extractedSelfOrderData = mutableListOf<SelfServiceOrderData>()
        val extractedDryOrderData = mutableListOf<DryCleanOrderData>()

        for (i in 0 until combinedAdapter.itemCount) {
            val item = combinedAdapter.combinedOrderItems[i]
            if (item is OrderData) {
                extractedOrderData.add(item)
            } else if (item is SelfServiceOrderData) {
                extractedSelfOrderData.add(item)
            } else if (item is DryCleanOrderData) {
                extractedDryOrderData.add(item)
            }
        }

    return OrderItemsData(extractedOrderData, extractedSelfOrderData, extractedDryOrderData)
    }


    private fun fetchUserName(userId: String, callback: (userName: String) -> Unit) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).child("userName")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                val userName = dataSnapshot.getValue(String::class.java) ?: ""
                callback(userName)
            }
            .addOnFailureListener {
                Log.e("Fetch User Name", "Failed to fetch user name")
            }
    }

    private fun fetchUserNameAndSaveOrder(
        userId: String,
        callback: (userName: String, userId: String) -> Unit
    ) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).child("userName")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                val userName = dataSnapshot.getValue(String::class.java)
                if (userName != null) {
                    callback(userName, userId)
                }
            }
            .addOnFailureListener {
                Log.e("Fetch User Name", "Failed to fetch user name")
            }
    }

    private fun saveOrderDataToRealtimePaid(
        orderData: List<OrderData>,
        selfServiceOrderData: List<SelfServiceOrderData>,
        dryServiceOrderData: List<DryCleanOrderData>,
        newJONumber: Int,
        userName: String,
        totalPrice: Double
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val databaseRef = databaseRef.child("Paid").child(currentDate).child("JO-$newJONumber")

            val totalDataList = mutableListOf<Any>()

            orderData.forEach { order ->
                val orderMap = mapOf(
                    "name" to order.itemName,
                    "price" to order.itemPrice,
                    "kilo" to order.kilo,
                    "subtotal" to order.itemSubtotal,
                    "userName" to userName,
                    "userId" to userId,
                    "orderType" to "regular"
                )
                totalDataList.add(orderMap)
            }

            // Add self-service order data to totalDataList
            selfServiceOrderData.forEach { selfServiceOrder ->
                val selfServiceMap = mapOf(
                    "name" to selfServiceOrder.itemName,
                    "price" to selfServiceOrder.itemPrice,
                    "loadOrPcs" to selfServiceOrder.loadOrPcs,
                    "subtotal" to selfServiceOrder.itemSubtotal,
                    "userName" to userName,
                    "userId" to userId,
                    "orderType" to "selfService"
                )
                totalDataList.add(selfServiceMap)
            }

            dryServiceOrderData.forEach { dryServiceOrder ->
                val dryServiceMap = mapOf(
                    "name" to dryServiceOrder.itemName,
                    "price" to dryServiceOrder.itemPrice,
                    "pcs" to dryServiceOrder.pcs,
                    "subtotal" to dryServiceOrder.itemSubtotal,
                    "userName" to userName,
                    "userId" to userId,
                    "orderType" to "dryClean"
                )
                totalDataList.add(dryServiceMap)
            }

            val data = mapOf(
                "time" to currentTime,
                "total" to totalPrice.toString(),
                "totalData" to totalDataList
            )

            databaseRef.setValue(data)
                .addOnSuccessListener {
                    Log.d("Receipt saved", "Receipt saved!!!")
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase Save Error", "Error saving data", exception)
                }
        }
    }

    private fun saveOrderDataToRealtime(
        orderData: List<OrderData>,
        selfServiceOrderData: List<SelfServiceOrderData>,
        dryServiceOrderData: List<DryCleanOrderData>,
        newJONumber: Int,
        userName: String,
        totalPrice: Double
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val databaseRef = databaseRef.child("Unpaid").child(currentDate).child("JO-$newJONumber")

            val totalDataList = mutableListOf<Any>()

            orderData.forEach { order ->
                val orderMap = mapOf(
                    "name" to order.itemName,
                    "price" to order.itemPrice,
                    "kilo" to order.kilo,
                    "subtotal" to order.itemSubtotal,
                    "userName" to userName,
                    "userId" to userId,
                    "orderType" to "regular"
                )
                totalDataList.add(orderMap)
            }

            // Add self-service order data to totalDataList
            selfServiceOrderData.forEach { selfServiceOrder ->
                val selfServiceMap = mapOf(
                    "name" to selfServiceOrder.itemName,
                    "price" to selfServiceOrder.itemPrice,
                    "loadOrPcs" to selfServiceOrder.loadOrPcs,
                    "subtotal" to selfServiceOrder.itemSubtotal,
                    "userName" to userName,
                    "userId" to userId,
                    "orderType" to "selfService"
                )
                totalDataList.add(selfServiceMap)
            }

            dryServiceOrderData.forEach { dryServiceOrder ->
                val dryServiceMap = mapOf(
                    "name" to dryServiceOrder.itemName,
                    "price" to dryServiceOrder.itemPrice,
                    "pcs" to dryServiceOrder.pcs,
                    "subtotal" to dryServiceOrder.itemSubtotal,
                    "userName" to userName,
                    "userId" to userId,
                    "orderType" to "dryClean"
                )
                totalDataList.add(dryServiceMap)
            }

            val data = mapOf(
                "time" to currentTime,
                "total" to totalPrice.toString(),
                "totalData" to totalDataList
            )

            databaseRef.setValue(data)
                .addOnSuccessListener {
                    Log.d("Receipt saved", "Receipt saved!!!")
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase Save Error", "Error saving data", exception)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<ImageButton>(R.id.backButton).visibility = View.VISIBLE
        requireActivity().findViewById<MaterialButton>(R.id.btnClearPreview).visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<ImageButton>(R.id.backButton).visibility = View.GONE
        requireActivity().findViewById<MaterialButton>(R.id.btnClearPreview).visibility = View.GONE
    }
}
