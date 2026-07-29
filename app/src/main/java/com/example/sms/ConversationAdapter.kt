package com.example.sms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val conversations: List<Conversation>,
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.tvName.text = conversation.address
        holder.tvLastMessage.text = conversation.lastMessage

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(conversation.lastTimestamp))

        if (conversation.messageCount > 1) {
            holder.tvCount.text = conversation.messageCount.toString()
            holder.tvCount.visibility = View.VISIBLE
        } else {
            holder.tvCount.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(conversation) }
    }

    override fun getItemCount() = conversations.size
}
