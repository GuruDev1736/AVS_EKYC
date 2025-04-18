package com.avs.avs_ekyc.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.Model.PendingCustomer
import com.avs.avs_ekyc.R

class PendingListAdapter(private val items: List<PendingCustomer>) : RecyclerView.Adapter<PendingListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val custName: TextView = itemView.findViewById(R.id.tvCustName)
        val accNo: TextView = itemView.findViewById(R.id.tvAccNo)
        val custNo : TextView = itemView.findViewById(R.id.tvCustNo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_customer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customer = items[position]
        holder.custNo.text = customer.CustNo
        holder.accNo.text = customer.AccNo
        holder.custName.text = customer.CustName
    }

    override fun getItemCount(): Int = items.size
}
