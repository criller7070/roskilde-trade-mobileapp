package dk.rosswap.mobile.feature.chat.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.utils.ChatTimeFormatterUtil
import dk.rosswap.mobile.databinding.ItemChatRowBinding
import dk.rosswap.mobile.feature.chat.domain.UserChat

// this is another instance of a RecyclerView adapter, classic Android Kotlin stuff it seems.
// it's kept in /presentation because it's a simple adapter for the UI of a chat list.

class ChatListAdapter(
    private val onChatClick: ((UserChat) -> Unit)? = null
) : ListAdapter<UserChat, ChatListAdapter.VH>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemChatRowBinding,
        private val onChatClick: ((UserChat) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: UserChat) {
            val ctx = binding.root.context

            binding.tvTitle.text = chat.itemName ?: ctx.getString(R.string.chat_no_title)

            val other = chat.otherUserName ?: ctx.getString(R.string.chat_unknown_user)
            binding.tvMed.text = ctx.getString(R.string.chat_with_user, other)

            binding.tvSummary.text = chat.lastMessage
                ?: ctx.getString(R.string.chat_no_messages)

            val seconds = chat.lastMessageTime?.seconds
            binding.tvTime.text = ChatTimeFormatterUtil.formatRelativeSeconds(ctx, seconds)

            // thumbnail
            val thumbUrl = chat.itemImage?.trim().orEmpty()
            if (thumbUrl.isBlank()) {
                binding.ivThumb.setImageResource(R.drawable.loading2)
            } else {
                binding.ivThumb.load(thumbUrl) {
                    crossfade(true)
                    placeholder(R.drawable.loading2)
                    error(R.drawable.loading2)
                }
            }

            // unread badge
            val unread = chat.unreadCount
            if (unread > 0) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.tvUnreadBadge.text = if (unread > 99) "99+" else unread.toString()
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onChatClick?.invoke(chat)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UserChat>() {
        override fun areItemsTheSame(oldItem: UserChat, newItem: UserChat) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UserChat, newItem: UserChat) = oldItem == newItem
    }
}
