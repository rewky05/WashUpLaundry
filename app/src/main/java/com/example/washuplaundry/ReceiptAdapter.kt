package com.example.washuplaundry

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReceiptAdapter(private val receiptData: List<ReceiptDataRow>) :
    RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder>() {

    class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val joNumberTextView: TextView = itemView.findViewById(R.id.jo_number)
        val timeTextView: TextView = itemView.findViewById(R.id.time)
        val totalPriceTextView: TextView = itemView.findViewById(R.id.total_price)
        val detailsContainer: LinearLayout = itemView.findViewById(R.id.details_container)
        val employee: TextView = itemView.findViewById(R.id.employee)
        val serviceName: TextView = itemView.findViewById(R.id.service_name)
        val itemSubtotal: TextView = itemView.findViewById(R.id.item_subtotal)
        val kilo: TextView = itemView.findViewById(R.id.kilo)
        val price: TextView = itemView.findViewById(R.id.price)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.receipt_row, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val row = receiptData[position]
        holder.joNumberTextView.text = "JO Number: ${row.joNumber}"
        holder.timeTextView.text = "Time: ${row.timestamp}"
        holder.totalPriceTextView.text = "Total: ₱${row.totalPrice}"

        if (row.isExpanded) {
            holder.detailsContainer.visibility = View.VISIBLE
            if (row.orderItems.size >= 5) {
                holder.employee.text = "Employee: ${row.orderItems[4].userName}"
            } else {
                holder.employee.text = "Employee: ${row.orderItems[0].userName}"
            }
            holder.serviceName.text = "${row.orderItems[0].name}"
            holder.itemSubtotal.text = "${row.orderItems[3].subtotal}"
            holder.kilo.text = "${row.orderItems[2].kilo}"
            holder.price.text = "${row.orderItems[1].price}"
            Log.d("Order items:", "${row.orderItems}")
            Log.d("Order items size:", "${row.orderItems.size}")
        } else {
            holder.detailsContainer.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            row.isExpanded = !row.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return receiptData.size
    }
}
