package com.example.washuplaundry

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(var historyData: List<HistoryDataRow>) :
    RecyclerView.Adapter<HistoryAdapter.ReceiptViewHolder>() {

    class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
    }

    override fun getItemCount(): Int {
        return historyData.size
    }
}
