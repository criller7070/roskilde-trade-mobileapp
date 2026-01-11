package dk.rosswap.mobile.feature.chat.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dk.rosswap.mobile.core.utils.ChatTimeFormatter
import dk.rosswap.mobile.databinding.ItemChatRowBinding
import dk.rosswap.mobile.feature.chat.domain.UserChat

class ChatListAdapter(
    private val onChatClick: ((UserChat) -> Unit)? = null
) : RecyclerView.Adapter<ChatListAdapter.VH>() {

    private val items = mutableListOf<UserChat>()

    fun submit(list: List<UserChat>) {
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
        private val onChatClick: ((UserChat) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: UserChat) {
            binding.tvTitle.text = chat.itemName ?: "(Ingen titel)"
            binding.tvMed.text = "Med: ${chat.otherUserName ?: "Ukendt"}"
            binding.tvSummary.text = chat.lastMessage
                ?: binding.root.context.getString(dk.rosswap.mobile.R.string.chat_no_messages)

            val seconds = chat.lastMessageTime?.seconds
            binding.tvTime.text = ChatTimeFormatter.formatRelativeSeconds(binding.root.context, seconds)

            binding.root.setOnClickListener {
                onChatClick?.invoke(chat)
            }
        }
    }
}
