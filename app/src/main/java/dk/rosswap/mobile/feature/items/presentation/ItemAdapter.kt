package dk.rosswap.mobile.feature.items.presentation

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.ui.components.popup.PopupBus

class ItemAdapter(
    private val onItemClicked: (Item) -> Unit = {},
    private val onMessageClicked: (Item) -> Unit = {},
    private val onLikeClicked: (Item) -> Unit = {},
    private val isLoggedIn: () -> Boolean = { true },
    private val currentUserIdProvider: () -> String? = { null },
    private val isLikedProvider: (String) -> Boolean = { false }
) : RecyclerView.Adapter<ItemAdapter.VH>() {

    companion object {
        private const val TAG = "ItemAdapter"
    }

    private val items = mutableListOf<Item>()
    private var likedIds: Set<String> = emptySet()

    fun submitList(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateLikedIds(newLikedIds: Set<String>) {
        val old = likedIds
        if (old == newLikedIds) return
        likedIds = newLikedIds
        // Find positions which changed and notify them
        for (i in items.indices) {
            val id = items[i].id
            val was = old.contains(id)
            val now = newLikedIds.contains(id)
            if (was != now) notifyItemChanged(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_item_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.iv_post_image)
        private val fav: ImageButton = itemView.findViewById(R.id.btn_favorite)
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val desc: TextView = itemView.findViewById(R.id.tv_description)
        private val type: TextView = itemView.findViewById(R.id.tv_type)
        private val author: TextView = itemView.findViewById(R.id.tv_author)
        private val message: Button = itemView.findViewById(R.id.btn_message)

        fun bind(item: Item) {
            val url = item.imageUrl.trim()
            if (url.isBlank()) {
                image.setImageResource(R.drawable.loading2)
            } else {
                image.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.loading2)
                    error(R.drawable.loading2)
                    listener(
                        onError = { request: ImageRequest, result: ErrorResult ->
                            Log.e(
                                TAG,
                                "Coil load failed for id=${item.id} url=${request.data}: ${result.throwable.message}",
                                result.throwable
                            )
                        }
                    )
                }
            }

            title.text = item.title
            desc.text = item.description
            type.text = item.mode
            author.text = item.userName.ifBlank { item.userId }

            message.text = itemView.context.getString(R.string.message)

            // Update favorite button icon based on liked state from ViewModel/adapter
            updateFavoriteIcon(item.id)

            // Item click navigates to detail (handled by fragment via callback)
            itemView.setOnClickListener { onItemClicked(item) }

            // Determine if this is the current user's own post
            val currentUserId = currentUserIdProvider()
            val isOwnPost = currentUserId != null && currentUserId == item.userId

            // If the post belongs to the current user, hide/disable interactive controls so they
            // are visible only as a plain post (no like/message). Otherwise keep them interactive.
            if (isOwnPost) {
                message.visibility = View.GONE
                fav.visibility = View.GONE

                // Remove listeners to be safe (avoid accidental interactions)
                message.setOnClickListener(null)
                fav.setOnClickListener(null)
            } else {
                message.visibility = View.VISIBLE
                fav.visibility = View.VISIBLE

                message.setOnClickListener { onMessageClicked(item) }

                fav.setOnClickListener {
                    // Prevent the heart UI from toggling if the user isn't logged in.
                    if (!isLoggedIn()) {
                        PopupBus.showError("You must be logged in to like posts.")
                        return@setOnClickListener
                    }

                    // Let the ViewModel handle the toggle via callback
                    onLikeClicked(item)
                }
            }
        }

        private fun updateFavoriteIcon(itemId: String) {
            // Prefer the adapter's likedIds snapshot (fast) and fall back to provider
            val liked = if (likedIds.isNotEmpty()) likedIds.contains(itemId) else isLikedProvider(itemId)
            if (liked) {
                fav.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                fav.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }
}
