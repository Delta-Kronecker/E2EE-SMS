package com.example.sms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private val messages: List<com.example.sms.model.Message>,
    private val myUuid: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBody: TextView = view.findViewById(R.id.tvSentBody)
        val tvTime: TextView = view.findViewById(R.id.tvSentTime)
        val tvEncrypted: TextView = view.findViewById(R.id.tvEncrypted)
        val tvDeliveryStatus: TextView = view.findViewById(R.id.tvDeliveryStatus)
    }

    class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBody: TextView = view.findViewById(R.id.tvReceivedBody)
        val tvTime: TextView = view.findViewById(R.id.tvReceivedTime)
        val tvEncrypted: TextView = view.findViewById(R.id.tvEncrypted)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderUuid == myUuid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = sdf.format(Date(message.timestamp))

        when (holder) {
            is SentMessageViewHolder -> {
                holder.tvBody.text = message.plaintext
                holder.tvTime.text = timeText
                holder.tvEncrypted.visibility = View.VISIBLE
                holder.tvDeliveryStatus.visibility = View.VISIBLE
                holder.tvDeliveryStatus.text = when (message.deliveryStatus) {
                    1 -> "✓"
                    2 -> "✓✓"
                    3 -> "✓✓"
                    else -> "⏳"
                }
                holder.tvDeliveryStatus.setTextColor(
                    if (message.deliveryStatus >= 2) 0xFF03DAC5.toInt() else 0xFF808080.toInt()
                )
            }
            is ReceivedMessageViewHolder -> {
                holder.tvBody.text = message.plaintext
                holder.tvTime.text = timeText
                holder.tvEncrypted.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount() = messages.size
}
