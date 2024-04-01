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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date

class History : Fragment() {

    private lateinit var receiptRecyclerView: RecyclerView
    private lateinit var adapterData: MutableList<HistoryDataRow>
    private lateinit var collectionRecyclerView: RecyclerView
    private lateinit var collectedAdapterData: MutableList<HistoryDataRow>
    private lateinit var dbUnpaid: DatabaseReference
    private val receiptDataByDate: MutableMap<Date, MutableList<HistoryDataRow>> = mutableMapOf()
    private val receiptDataByDateColl: MutableMap<Date, MutableList<HistoryDataRow>> = mutableMapOf()
    private lateinit var dateString: String
    private lateinit var dateStringUnpaid: String
    private lateinit var joUnpaid: String
    private lateinit var db: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        receiptRecyclerView = view.findViewById(R.id.unpaid_recycler_view)
        adapterData = mutableListOf()
        collectionRecyclerView = view.findViewById(R.id.collection_recycler_view)
        collectedAdapterData = mutableListOf()
        db = FirebaseDatabase.getInstance().getReference("Receipts/Paid")
        dbUnpaid = FirebaseDatabase.getInstance().getReference("Receipts/Unpaid")
        dateString = ""
        dateStringUnpaid = ""
        joUnpaid = ""

        fetchDataFromFirebase()

        // Inside onCreateView() method
        val unpaidSearchEditText = view.findViewById<EditText>(R.id.unpaid_search)
        unpaidSearchEditText?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(searchQuery: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplayResults(searchQuery.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun fetchDataFromFirebase() {
        // Fetch unpaid receipts data
        dbUnpaid.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                collectedAdapterData.clear()
                collectedAdapterData.addAll(processAndPrepareUnpaidData(snapshot))

                collectionRecyclerView.layoutManager = LinearLayoutManager(context)
                collectionRecyclerView.adapter = HistoryAdapter(collectedAdapterData)
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
                receiptRecyclerView.adapter = HistoryAdapter(adapterData)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "Error fetching paid data", error.toException())
            }
        })
    }

    private fun processAndPrepareData(snapshot: DataSnapshot): List<HistoryDataRow> {
        val adapterData = mutableListOf<HistoryDataRow>()
        receiptDataByDate.clear()

        for (dateSnapshot in snapshot.children) {
            val dateString = dateSnapshot.key ?: continue
            val receiptDate = SimpleDateFormat("yyyy-MM-dd").parse(dateString) ?: continue

            receiptDataByDate.getOrPut(receiptDate) { mutableListOf<HistoryDataRow>() }.let { dateReceipts ->

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

                        when (orderType) {
                            "regular" -> {
                                val orderItem = itemSnapshot.getValue(OrderData::class.java)
                                orderItem?.let { orderItems.add(it) }
                            }
                            "selfService" -> {
                                val serviceItem = itemSnapshot.getValue(SelfServiceOrderData::class.java)
                                serviceItem?.let { selfServiceOrderItems.add(it) }
                            }
                            "dryClean" -> {
                                val dryServiceItem = itemSnapshot.getValue(DryCleanOrderData::class.java)
                                dryServiceItem?.let { dryCleanOrderItems.add(it) }
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

                    val receiptDataRow = createReceiptDataRow(dateString, joData)
                    adapterData.add(receiptDataRow)

                    dateReceipts.add(receiptDataRow)
                }
            }
        }

        return receiptDataByDate.values.flatten()
    }

    private fun processAndPrepareUnpaidData(snapshot: DataSnapshot): List<HistoryDataRow> {
        val adapterData = mutableListOf<HistoryDataRow>()
        receiptDataByDateColl.clear()

        for (dateSnapshot in snapshot.children) {
            val dateString = dateSnapshot.key ?: continue
            val receiptDate = SimpleDateFormat("yyyy-MM-dd").parse(dateString) ?: continue

            receiptDataByDateColl.getOrPut(receiptDate) { mutableListOf<HistoryDataRow>() }.let { dateReceipts ->

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

                        when (orderType) {
                            "regular" -> {
                                val orderItem = itemSnapshot.getValue(OrderData::class.java)
                                orderItem?.let { orderItems.add(it) }
                            }
                            "selfService" -> {
                                val serviceItem = itemSnapshot.getValue(SelfServiceOrderData::class.java)
                                serviceItem?.let { selfServiceOrderItems.add(it) }
                            }
                            "dryClean" -> {
                                val dryServiceItem = itemSnapshot.getValue(DryCleanOrderData::class.java)
                                dryServiceItem?.let { dryCleanOrderItems.add(it) }
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

                    val receiptDataRow = createReceiptDataRow(dateString, joData)
                    collectedAdapterData.add(receiptDataRow)

                    dateReceipts.add(receiptDataRow)
                }
            }
        }

        return receiptDataByDateColl.values.flatten()
    }

    private fun createReceiptDataRow(dateString: String, joData: JONumberData): HistoryDataRow {
        val formattedTime = joData.timestamp
        val orderDetails = joData.details
        val orderTotalPrice = orderDetails.totalPrice

        return HistoryDataRow(
            date = dateString,
            joNumber = joData.joNumber,
            timestamp = formattedTime,
            orderItems = orderDetails.orderItems,
            totalPrice = orderTotalPrice,
            isExpanded = false
        )
    }

//    private fun filterAndDisplayResults(searchQuery: String) {
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
//
//        val filteredPaidData = adapterData.flatMap { receiptDataRow ->
//            val date = receiptDataRow.date
//            if (date != null) {
//                try {
//                    val formattedDate = dateFormat.format(date)
//                    if (formattedDate.contains(searchQuery, ignoreCase = true) || receiptDataRow.joNumber.contains(searchQuery, ignoreCase = true)) {
//                        listOf(receiptDataRow)
//                    } else {
//                        emptyList()
//                    }
//                } catch (e: Exception) {
//                    Log.e("Filtering Error", "Error formatting date: $date", e)
//                    emptyList()
//                }
//            } else {
//                Log.e("Filtering Error", "Invalid date object found: $date")
//                emptyList()
//            }
//        }
//
//        val filteredUnpaidData = collectedAdapterData.flatMap { receiptDataRow ->
//            val date = receiptDataRow.date
//            if (date != null) {
//                try {
//                    val formattedDate = dateFormat.format(date)
//                    if (formattedDate.contains(searchQuery, ignoreCase = true) || receiptDataRow.joNumber.contains(searchQuery, ignoreCase = true)) {
//                        listOf(receiptDataRow)
//                    } else {
//                        emptyList()
//                    }
//                } catch (e: Exception) {
//                    Log.e("Filtering Error", "Error formatting date: $date", e)
//                    emptyList()
//                }
//            } else {
//                Log.e("Filtering Error", "Invalid date object found: $date")
//                emptyList()
//            }
//        }
//
//        receiptRecyclerView.adapter?.updateList(filteredPaidData)
//        collectionRecyclerView.adapter?.updateList(filteredUnpaidData)
//    }


    private fun filterAndDisplayResults(searchQuery: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val filteredData = receiptDataByDate.entries.flatMap { (date, receiptList) ->
            receiptList.asSequence().filter { receiptDataRow ->
                val dateMatch = dateFormat.format(date).contains(searchQuery, ignoreCase = true)
                receiptDataRow.joNumber.contains(searchQuery, ignoreCase = true) || dateMatch
            }.toList()
        }
        val filteredDataColl = receiptDataByDateColl.entries.flatMap { (date, receiptList) ->
            receiptList.asSequence().filter { receiptDataRow ->
                val dateMatch = dateFormat.format(date).contains(searchQuery, ignoreCase = true)
                receiptDataRow.joNumber.contains(searchQuery, ignoreCase = true) || dateMatch
            }.toList()
        }
        receiptRecyclerView.adapter?.updateList(filteredData)
        collectionRecyclerView.adapter?.updateList(filteredDataColl)
    }

    private fun RecyclerView.Adapter<*>.updateList(newList: List<HistoryDataRow>) {
        (this as HistoryAdapter).apply {
            historyData = newList
            notifyDataSetChanged()
        }
    }
}
