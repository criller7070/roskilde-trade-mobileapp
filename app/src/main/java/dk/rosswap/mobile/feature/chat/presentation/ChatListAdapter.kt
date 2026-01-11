package dk.rosswap.mobile.feature.chat.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dk.rosswap.mobile.databinding.ItemChatRowBinding
import dk.rosswap.mobile.feature.chat.domain.Chat

class ChatListAdapter(
    private val onChatClick: ((Chat) -> Unit)? = null
) : RecyclerView.Adapter<ChatListAdapter.VH>() {

    private val items = mutableListOf<Chat>()

    fun submit(list: List<Chat>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(
        private val binding: ItemChatRowBinding,
        private val onChatClick: ((Chat) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            binding.tvTitle.text = chat.itemName ?: "(Ingen titel)"
            binding.tvMed.text = "Med: ${chat.otherUserName ?: "Ukendt"}"
            binding.tvSummary.text = chat.lastMessage ?: "Ingen beskeder endnu"

            // Keep it simple for now; later we'll format like the web app ("x minutes ago").
            binding.tvTime.text = chat.lastMessageTime?.seconds?.toString() ?: ""

            binding.root.setOnClickListener {
                onChatClick?.invoke(chat)
            }
        }
    }
}

