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
import dk.rosswap.mobile.core.common.Item

class ItemsAdapter(
    private val onMessageClicked: (Item) -> Unit = {},
    private val onLikeClicked: (Item) -> Unit = {},
    private val onDislikeClicked: (Item) -> Unit = {}
) : RecyclerView.Adapter<ItemsAdapter.VH>() {

    companion object {
        private const val TAG = "ItemsAdapter"
    }

    private val items = mutableListOf<Item>()
    private val likedItemIds = mutableSetOf<String>()

    fun submitList(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
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
        private val dislike: ImageButton? = itemView.findViewById(R.id.btn_dislike)
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

            // Update favorite button icon based on liked state
            updateFavoriteIcon(item.id)

            message.setOnClickListener { onMessageClicked(item) }
            
            fav.setOnClickListener {
                if (likedItemIds.contains(item.id)) {
                    likedItemIds.remove(item.id)
                } else {
                    likedItemIds.add(item.id)
                }
                updateFavoriteIcon(item.id)
                onLikeClicked(item)
            }
            
            dislike?.setOnClickListener {
                onDislikeClicked(item)
            }
        }

        private fun updateFavoriteIcon(itemId: String) {
            if (likedItemIds.contains(itemId)) {
                fav.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                fav.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }
}
