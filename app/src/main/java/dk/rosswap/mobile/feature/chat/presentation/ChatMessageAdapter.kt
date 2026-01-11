package dk.rosswap.mobile.feature.chat.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dk.rosswap.mobile.core.utils.ChatTimeFormatter
import dk.rosswap.mobile.databinding.ItemMessageReceivedBinding
import dk.rosswap.mobile.databinding.ItemMessageSentBinding
import dk.rosswap.mobile.feature.chat.domain.ChatMessage

class ChatMessageAdapter(
    private val currentUserId: () -> String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatMessage>()

    fun submit(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val msg = items[position]
        val isSent = msg.senderId != null && msg.senderId == currentUserId()
        return if (isSent) VIEW_SENT else VIEW_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SENT) {
            SentVH(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        when (holder) {
            is SentVH -> holder.bind(msg)
            is ReceivedVH -> holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = items.size

    class SentVH(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvMessage.text = msg.text ?: ""
            binding.tvTime.text = ChatTimeFormatter.formatRelativeSeconds(binding.root.context, msg.timestamp?.seconds)
        }
    }

    class ReceivedVH(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvMessage.text = msg.text ?: ""
            binding.tvTime.text = ChatTimeFormatter.formatRelativeSeconds(binding.root.context, msg.timestamp?.seconds)
        }
    }

    private companion object {
        const val VIEW_SENT = 1
        const val VIEW_RECEIVED = 2
    }
}
