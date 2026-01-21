package dk.rosswap.mobile.feature.account.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.account.domain.AccountItem

class ProfilePostsAdapter(
    private val onClick: (AccountItem) -> Unit,
    private val onDelete: (AccountItem) -> Unit
) : ListAdapter<AccountItem, ProfilePostsAdapter.VH>(ItemDiffCallback()) {

    class ItemDiffCallback : DiffUtil.ItemCallback<AccountItem>() {
        override fun areItemsTheSame(oldItem: AccountItem, newItem: AccountItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AccountItem, newItem: AccountItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.ivPostImage)
        private val title = view.findViewById<TextView>(R.id.tvPostTitle)
        private val description = view.findViewById<TextView>(R.id.tvPostDescription)
        private val delete = view.findViewById<ImageView>(R.id.ivDeletePost)

        fun bind(post: AccountItem) {
            image.load(post.imageUrl)
            title.text = post.title
            description.text = post.description

            itemView.setOnClickListener { onClick(post) }
            delete.setOnClickListener { onDelete(post) }
        }
    }
}
