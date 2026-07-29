package com.example.sms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val messages: List<SmsMessage>,
    private val onClick: (SmsMessage) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvBody: TextView = view.findViewById(R.id.tvBody)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvAddress.text = message.address
        holder.tvBody.text = message.body

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(message.timestamp))

        holder.itemView.setOnClickListener { onClick(message) }
    }

    override fun getItemCount() = messages.size
}
