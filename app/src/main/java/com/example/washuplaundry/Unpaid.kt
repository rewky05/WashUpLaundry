package com.example.washuplaundry

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date

class Unpaid : Fragment() {

    private lateinit var receiptRecyclerView: RecyclerView
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var adapterData: MutableList<ReceiptDataRow> // Make this accessible
    private val receiptDataByDate: MutableMap<Date, MutableList<ReceiptDataRow>> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_unpaid, container, false)

        receiptRecyclerView = view.findViewById(R.id.unpaid_recycler_view)
        adapterData = mutableListOf() // Initialize adapterData

        fetchDataFromFirebase()

        return view
    }

    private fun fetchDataFromFirebase() {
        val receiptRef = database.child("Receipts/Unpaid")

        receiptRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapterData.clear() // Clear data before updating
                adapterData.addAll(processAndPrepareData(snapshot)) // Populate adapterData

                receiptRecyclerView.layoutManager = LinearLayoutManager(context)
                receiptRecyclerView.adapter = ReceiptAdapter(adapterData)

                // Search setup
                val searchEditText = view?.findViewById<EditText>(R.id.unpaid_search)
                searchEditText?.addTextChangedListener(object : TextWatcher {
                    override fun onTextChanged(searchQuery: CharSequence?, start: Int, before: Int, count: Int) {
                        filterAndDisplayResults(searchQuery.toString())
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }
                    override fun afterTextChanged(s: Editable?) {
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "Error fetching data", error.toException())
            }
        })
    }

    private fun processAndPrepareData(snapshot: DataSnapshot): List<ReceiptDataRow> {
        val adapterData = mutableListOf<ReceiptDataRow>()
        Log.d("adapterData", "$adapterData")
        receiptDataByDate.clear()

        for (dateSnapshot in snapshot.children) {
            val dateString = dateSnapshot.key ?: continue
            val receiptDate = SimpleDateFormat("yyyy-MM-dd").parse(dateString) ?: continue

            // Create a new list if it's the first time encountering a date, otherwise use existing list
            receiptDataByDate.getOrPut(receiptDate) { mutableListOf<ReceiptDataRow>() }.let { dateReceipts ->

                if (receiptDate != null) {
                    for (joDataSnapshot in dateSnapshot.children) {
                        val joNumber = joDataSnapshot.key ?: continue
                        val timestamp = joDataSnapshot.child("time").value as? String ?: ""

                        val totalPriceNode = joDataSnapshot.child("total")
                        val totalPriceString = totalPriceNode.value.toString()
                        val totalPrice = totalPriceString.toDoubleOrNull() ?: 0.0

                        val totalDataArray = joDataSnapshot.child("totalData").children
                        val orderItems = mutableListOf<OrderData>()
                        val selfServiceOrderItems = mutableListOf<SelfServiceOrderData>()
                        val dryCleanOrderItems = mutableListOf<DryCleanOrderData>()

                        for (itemSnapshot in totalDataArray) {
                            val orderType = itemSnapshot.child("orderType").value as? String

                            if (orderType == "regular") {
                                val orderItem = itemSnapshot.getValue(OrderData::class.java)
                                if (orderItem != null) {
                                    orderItems.add(orderItem)
                                }
                            } else if (orderType == "selfService") {
                                val serviceItem =
                                    itemSnapshot.getValue(SelfServiceOrderData::class.java)
                                if (serviceItem != null) {
                                    selfServiceOrderItems.add(serviceItem)
                                }
                            } else if (orderType == "dryClean") {
                                val dryServiceItem =
                                    itemSnapshot.getValue(DryCleanOrderData::class.java)
                                if (dryServiceItem != null) {
                                    dryCleanOrderItems.add(dryServiceItem)
                                }
                            }
                        }

                        val joData = JONumberData(
                            joNumber = joNumber,
                            timestamp = timestamp,
                            details = OrderDetails(
                                totalPrice = totalPrice,
                                orderItems = listOf(
                                    OrderItemsData(
                                        orderData = orderItems,
                                        selfServiceOrderData = selfServiceOrderItems,
                                        dryCleanOrderData = dryCleanOrderItems
                                    )
                                )
                            )
                        )

                        val receiptDataRow = createReceiptDataRow(joData)
                        adapterData.add(receiptDataRow)

                        dateReceipts.add(receiptDataRow)
                    }
                }
            }
        }

        return receiptDataByDate.values.flatten()
    }

    private fun createReceiptDataRow(joData: JONumberData): ReceiptDataRow {
        val formattedTime = joData.timestamp
        val orderDetails = joData.details
        val orderTotalPrice = orderDetails.totalPrice

        return ReceiptDataRow(
            joNumber = joData.joNumber,
            timestamp = formattedTime,
            orderItems = orderDetails.orderItems,
            totalPrice = orderTotalPrice,
            isExpanded = false
        )
    }

    private fun filterAndDisplayResults(searchQuery: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val filteredData = receiptDataByDate.entries.flatMap { (date, receiptList) ->
            receiptList.asSequence().filter { receiptDataRow ->
            val dateMatch = dateFormat.format(date).contains(searchQuery, ignoreCase = true)
                receiptDataRow.joNumber.contains(searchQuery, ignoreCase = true) || dateMatch
            }.toList()
    }
        receiptRecyclerView.adapter?.updateList(filteredData)
    }

    private fun RecyclerView.Adapter<*>.updateList(newList: List<ReceiptDataRow>) {
        (this as ReceiptAdapter).apply {
            receiptData = newList
            notifyDataSetChanged()
        }
    }
}
