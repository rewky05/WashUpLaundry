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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Receipts : Fragment() {

    private lateinit var receiptRecyclerView: RecyclerView
    private lateinit var adapterData: MutableList<ReceiptDataRow>
    private lateinit var collectionRecyclerView: RecyclerView
    private lateinit var collectedAdapterData: MutableList<ReceiptDataRow>
    private val receiptDataByDate: MutableMap<Date, MutableList<ReceiptDataRow>> = mutableMapOf()
    private val receiptDataByDateColl: MutableMap<Date, MutableList<ReceiptDataRow>> = mutableMapOf()
    private lateinit var current_date: TextView
    private lateinit var currentDate: Calendar
    private lateinit var currentDateText: String
    private lateinit var dbUnpaid: DatabaseReference
    private lateinit var db: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipts, container, false)

        currentDate = Calendar.getInstance()

        current_date = view.findViewById(R.id.current_date)
        currentDateText = SimpleDateFormat("yyyy-MM-dd").format(Date())
        current_date.text = currentDateText

        db = FirebaseDatabase.getInstance().getReference("Receipts/Paid")
        dbUnpaid = FirebaseDatabase.getInstance().getReference("Receipts/Unpaid")

        receiptRecyclerView = view.findViewById(R.id.receipt_recycler_view)
        adapterData = mutableListOf()
        collectionRecyclerView = view.findViewById(R.id.collection_recycler_view)
        collectedAdapterData = mutableListOf()

        fetchDataFromFirebase()

        return view
    }

    private fun fetchDataFromFirebase() {
        // Fetch unpaid receipts data
        dbUnpaid.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                collectedAdapterData.clear()
                collectedAdapterData.addAll(processAndPrepareUnpaidData(snapshot))

                collectionRecyclerView.layoutManager = LinearLayoutManager(context)
                collectionRecyclerView.adapter = ReceiptAdapter(collectedAdapterData)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "Error fetching unpaid data", error.toException())
            }
        })

        // Fetch paid receipts data
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapterData.clear()
                adapterData.addAll(processAndPrepareData(snapshot))

                receiptRecyclerView.layoutManager = LinearLayoutManager(context)
                receiptRecyclerView.adapter = ReceiptAdapter(adapterData)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "Error fetching paid data", error.toException())
            }
        })
    }

    private fun processAndPrepareUnpaidData(snapshot: DataSnapshot): List<ReceiptDataRow> {
        receiptDataByDateColl.clear()

        val collectAdapterData = mutableListOf<ReceiptDataRow>()
        Log.d("collectAdapterData", "$collectAdapterData")

        for (dateSnapshot in snapshot.children) {
            val dateString = dateSnapshot.key ?: continue

            val receiptDate = SimpleDateFormat("yyyy-MM-dd").parse(dateString)
            if (receiptDate != null && isSameDate(currentDate.time, receiptDate)) {

                receiptDataByDateColl.getOrPut(receiptDate) { mutableListOf<ReceiptDataRow>() }
                    .let { dateReceipts ->

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
                                Log.d("orderType", "$orderType")

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
                            collectAdapterData.add(receiptDataRow)

                            dateReceipts.add(receiptDataRow)
                        }
                    }
            }
        }
        return receiptDataByDateColl.values.flatten()
    }

    private fun processAndPrepareData(snapshot: DataSnapshot): List<ReceiptDataRow> {
        receiptDataByDate.clear()

        val adapterData = mutableListOf<ReceiptDataRow>()
        Log.d("adapterData", "$adapterData")

        for (dateSnapshot in snapshot.children) {
            val dateString = dateSnapshot.key ?: continue

            val receiptDate = SimpleDateFormat("yyyy-MM-dd").parse(dateString)

            receiptDataByDate.getOrPut(receiptDate) { mutableListOf<ReceiptDataRow>() }
                .let { dateReceipts ->

                    if (receiptDate != null && isSameDate(currentDate.time, receiptDate)) {

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
                            Log.d("receiptDataRow", "$receiptDataRow")
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
