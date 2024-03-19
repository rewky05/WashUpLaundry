package com.example.washuplaundry

import android.content.Context
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
        holder.totalPriceTextView.text = "₱${row.totalPrice}"
        val userName = row.orderItems.firstOrNull()?.userName ?: ""
        holder.employee.text = userName

        if (row.isExpanded) {
            holder.detailsContainer.visibility = View.VISIBLE
            holder.detailsContainer.removeAllViews()

            for (orderItem in row.orderItems) {
                val itemDetailsView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.receipt_item_detail, holder.detailsContainer, false)

                itemDetailsView.findViewById<TextView>(R.id.service_name).text = orderItem.name
                itemDetailsView.findViewById<TextView>(R.id.kilo).text = orderItem.kilo.toString()
                itemDetailsView.findViewById<TextView>(R.id.price).text = orderItem.price.toString()
                itemDetailsView.findViewById<TextView>(R.id.item_subtotal).text = orderItem.subtotal.toString()

                holder.detailsContainer.addView(itemDetailsView)
            }
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
