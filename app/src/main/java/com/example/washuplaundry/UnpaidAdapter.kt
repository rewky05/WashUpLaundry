package com.example.washuplaundry

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnpaidAdapter(var historyData: List<HistoryDataRow>) :
    RecyclerView.Adapter<UnpaidAdapter.ReceiptViewHolder>() {

    private var database = Firebase.database.getReference("Receipts/Unpaid")

    class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val collect: MaterialButton = itemView.findViewById(R.id.btnCollect)
        val date: TextView = itemView.findViewById(R.id.date)
        val joNumberTextView: TextView = itemView.findViewById(R.id.jo_number)
        val timeTextView: TextView = itemView.findViewById(R.id.time)
        val totalPriceTextView: TextView = itemView.findViewById(R.id.total_price)
        val detailsContainer: LinearLayout = itemView.findViewById(R.id.details_container)
        val employee: TextView = itemView.findViewById(R.id.employee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.receipt_row, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val row = historyData[position]
        holder.collect.visibility = View.VISIBLE
        holder.collect.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                fetchUserNameAndSaveOrder(userId) { userName, retrievedUserId ->
                    val dateUnpaid = row.date
                    val joNumber = row.joNumber
                    val databaseCollect = Firebase.database.getReference("Receipts/Unpaid/$dateUnpaid/$joNumber")
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val collectedByData = CollectedByData(uid = userId, username = userName)
                    val updates = hashMapOf<String, Any>(
                        "collected/collectedBy" to collectedByData,
                        "collected/collectedOn" to currentDate
                    )

                    databaseCollect.updateChildren(updates).addOnSuccessListener {
                        Log.d("Firebase Update", "Successfully added collected data")
                        holder.collect.isEnabled = false
                        holder.collect.text = "COLLECTED"
                        holder.collect.alpha = 0.5f
                    }.addOnFailureListener { e ->
                        Log.e("Firebase Update", "Failed to add collected data", e)
                    }
                }
            }
        }

        holder.date.visibility = View.VISIBLE
        holder.date.text = row.date
        holder.joNumberTextView.text = row.joNumber
        holder.timeTextView.text = "Time: ${row.timestamp}"
        holder.totalPriceTextView.text = "₱${row.totalPrice}"

        val userName = row.orderItems.firstOrNull()?.selfServiceOrderData?.firstOrNull()?.userName ?:
        row.orderItems.firstOrNull()?.orderData?.firstOrNull()?.userName ?: row.orderItems.firstOrNull()?.dryCleanOrderData?.firstOrNull()?.userName ?: ""

        Log.d("user", userName)
        holder.employee.text = userName

        if (row.isExpanded) {
            holder.detailsContainer.visibility = View.VISIBLE
            holder.detailsContainer.removeAllViews()

            for (orderItemsData in row.orderItems) {
                for (orderItem in orderItemsData.orderData + orderItemsData.selfServiceOrderData + orderItemsData.dryCleanOrderData) {
                    val itemDetailsView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.receipt_item_detail, holder.detailsContainer, false)

                    itemDetailsView.findViewById<TextView>(R.id.service_name).text = orderItem.name
                    itemDetailsView.findViewById<TextView>(R.id.kilo).text = orderItem.kilo.toString()
                    itemDetailsView.findViewById<TextView>(R.id.price).text = orderItem.price.toString()
                    itemDetailsView.findViewById<TextView>(R.id.item_subtotal).text = orderItem.subtotal.toString()

                    if (orderItem is SelfServiceOrderData) {
                        val visibleKilo = itemDetailsView.findViewById<TextView>(R.id.kilo)
                        visibleKilo.visibility = View.GONE

                        itemDetailsView.findViewById<TextView>(R.id.loadorpcs).visibility = View.VISIBLE
                        itemDetailsView.findViewById<TextView>(R.id.loadorpcs).text = orderItem.loadOrPcs.toString()
                    } else if (orderItem is DryCleanOrderData) {
                        val visibleKilo = itemDetailsView.findViewById<TextView>(R.id.kilo)
                        visibleKilo.visibility = View.GONE

                        itemDetailsView.findViewById<TextView>(R.id.pcs).visibility = View.VISIBLE
                        itemDetailsView.findViewById<TextView>(R.id.pcs).text = orderItem.pcs.toString()
                    }

                    holder.detailsContainer.addView(itemDetailsView)
                }
            }

        } else {
            holder.detailsContainer.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            row.isExpanded = !row.isExpanded
            notifyItemChanged(position)
        }

        val joNumber = row.joNumber
        val dateUnpaid = row.date
        database.child("$dateUnpaid/$joNumber/collected").get().addOnSuccessListener {
            if (it.exists()) {
                holder.collect.isEnabled = false
                holder.collect.text = "COLLECTED"
                holder.collect.alpha = 0.5f
            } else {
                holder.collect.isEnabled = true
                holder.collect.text = "COLLECT"
                holder.collect.alpha = 1.0f // Reset to fully opaque
            }
        }.addOnFailureListener {
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

    override fun getItemCount(): Int {
        return historyData.size
    }
}
