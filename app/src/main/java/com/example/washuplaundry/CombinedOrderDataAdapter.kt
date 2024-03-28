package com.example.washuplaundry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CombinedOrderDataAdapter(
    var combinedOrderItems: MutableList<OrderItem> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_REGULAR_SERVICE = 0
        private const val TYPE_SELF_SERVICE = 1
        private const val TYPE_DRYCLEAN_SERVICE = 2
    }

    inner class RegularServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_name)
        val price: TextView = itemView.findViewById(R.id.item_price)
        val kilo: TextView = itemView.findViewById(R.id.item_kilo)
        val subtotal: TextView = itemView.findViewById(R.id.item_subtotal)
    }

    inner class SelfServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_name)
        val price: TextView = itemView.findViewById(R.id.item_price)
        val loadOrPcs: TextView = itemView.findViewById(R.id.item_kilo)
        val subtotal: TextView = itemView.findViewById(R.id.item_subtotal)
    }

    inner class DryCleanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_name)
        val price: TextView = itemView.findViewById(R.id.item_price)
        val pcs: TextView = itemView.findViewById(R.id.item_pcs)
        val subtotal: TextView = itemView.findViewById(R.id.item_subtotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_REGULAR_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.total_preview_regular, parent, false)
                RegularServiceViewHolder(view)
            }
            TYPE_SELF_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.total_preview_self, parent, false)
                SelfServiceViewHolder(view)
            }
            TYPE_DRYCLEAN_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.total_preview_dry, parent, false)
                DryCleanViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = combinedOrderItems[position]
        when (holder.itemViewType) {
            TYPE_REGULAR_SERVICE -> {
                val regularServiceHolder = holder as RegularServiceViewHolder
                regularServiceHolder.name.text = item.name
                regularServiceHolder.price.text = "₱${item.price}"
                regularServiceHolder.kilo.text = "${item.kilo}"
                regularServiceHolder.subtotal.text = "₱${String.format("%.2f", item.subtotal)}"
            }
            TYPE_SELF_SERVICE -> {
                val selfServiceHolder = holder as SelfServiceViewHolder
                selfServiceHolder.name.text = item.name
                selfServiceHolder.price.text = "₱${item.price}"
                selfServiceHolder.loadOrPcs.text = "${item.loadOrPcs}"
                selfServiceHolder.subtotal.text = "₱${String.format("%.2f", item.subtotal)}"
            }
            TYPE_DRYCLEAN_SERVICE -> {
                val dryCleanViewHolder = holder as DryCleanViewHolder
                dryCleanViewHolder.name.text = item.name
                dryCleanViewHolder.price.text = "₱${item.price}"
                dryCleanViewHolder.pcs.text = "${item.pcs}"
                dryCleanViewHolder.subtotal.text = "₱${item.subtotal}"
            }
        }
    }

    fun updateData(orderItems: List<OrderItem>) {
        combinedOrderItems.clear()
        combinedOrderItems.addAll(orderItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = combinedOrderItems.size

    override fun getItemViewType(position: Int): Int {
        return when (combinedOrderItems[position]) {
            is OrderData -> TYPE_REGULAR_SERVICE
            is SelfServiceOrderData -> TYPE_SELF_SERVICE
            is DryCleanOrderData -> TYPE_DRYCLEAN_SERVICE
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    fun clearLists() {
        combinedOrderItems.clear()
        notifyDataSetChanged()
    }
}
