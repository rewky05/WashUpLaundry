package com.example.washuplaundry

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Receipts : Fragment() {

    private lateinit var receiptRecyclerView: RecyclerView
    private lateinit var current_date: TextView
    private lateinit var currentDate: Calendar
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipts, container, false)

        receiptRecyclerView = view.findViewById(R.id.receipt_recycler_view)
        current_date = view.findViewById(R.id.current_date)
        currentDate = Calendar.getInstance()

        fetchDataFromFirebase()

        return view
    }

    private fun fetchDataFromFirebase() {
        val receiptRef = database.child("Receipts/Unpaid")

        receiptRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val receiptData = processAndPrepareData(snapshot)

                receiptRecyclerView.layoutManager = LinearLayoutManager(context)
                receiptRecyclerView.adapter = ReceiptAdapter(receiptData)

                currentDate = Calendar.getInstance()
                current_date.text = SimpleDateFormat("yyyy-MM-dd").format(currentDate.time)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "Error fetching data", error.toException())
            }
        })
    }

    private fun processAndPrepareData(snapshot: DataSnapshot): List<ReceiptDataRow> {
        val adapterData = mutableListOf<ReceiptDataRow>()

        for (dateSnapshot in snapshot.children) {
            val dateString = dateSnapshot.key ?: continue

            val receiptDate = SimpleDateFormat("yyyy-MM-dd").parse(dateString)
            if (receiptDate != null && isSameDate(currentDate.time, receiptDate)) {

                for (joDataSnapshot in dateSnapshot.children) {
                    val joNumber = joDataSnapshot.key ?: continue
                    val timestamp = joDataSnapshot.child("time").value as? String ?: ""

                    val totalPriceNode = joDataSnapshot.child("total")
                    val totalPriceString = totalPriceNode.value.toString()
                    val totalPrice = totalPriceString.toDoubleOrNull() ?: 0.0

                    val orderItems = mutableListOf<OrderData>()
                    for (itemSnapshot in totalPriceNode.children) {
                        val orderItem = itemSnapshot.getValue(OrderData::class.java)
                        orderItem?.let { orderItems.add(it) }
                    }

                    val joData = JONumberData(
                        joNumber = joNumber,
                        timestamp = timestamp,
                        details = OrderDetails(
                            totalPrice = totalPrice,
                            orderItems = orderItems
                        )
                    )

                    val receiptDataRow = createReceiptDataRow(joData)
                    adapterData.add(receiptDataRow)
                }
            }
        }

        return adapterData
    }

    private fun createReceiptDataRow(joData: JONumberData): ReceiptDataRow {
        val formattedTime = joData.timestamp
        val orderDetails = joData.details
        val orderItems = orderDetails.orderItems
        val orderTotalPrice = orderDetails.totalPrice

        return ReceiptDataRow(
            joNumber = joData.joNumber,
            timestamp = formattedTime,
            orderItems = orderItems,
            totalPrice = orderTotalPrice,
            isExpanded = false
        )
    }

    private fun isSameDate(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}
