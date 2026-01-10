package dk.rosswap.mobile.feature.items.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.items.data.Item

// Adapter for displaying a list of items in a RecyclerView.
// classic kotlin android thing. Its in /presentation because
// its UI related.

class ItemsAdapter(
    private val onMessageClicked: (Item) -> Unit = {},
    private val onLikeClicked: (Item) -> Unit = {}
) : RecyclerView.Adapter<ItemsAdapter.VH>() {

    private val items = mutableListOf<Item>()

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
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val desc: TextView = itemView.findViewById(R.id.tv_description)
        private val type: TextView = itemView.findViewById(R.id.tv_type)
        private val author: TextView = itemView.findViewById(R.id.tv_author)
        private val message: Button = itemView.findViewById(R.id.btn_message)

        fun bind(item: Item) {
            // NOTE: image loading for URLs will be added later (Coil/Glide).
            image.setImageResource(R.drawable.ic_photo_placeholder)

            title.text = item.title
            desc.text = item.description
            type.text = item.mode
            author.text = item.userName.ifBlank { item.userId }

            message.text = itemView.context.getString(R.string.message)

            message.setOnClickListener { onMessageClicked(item) }
            fav.setOnClickListener { onLikeClicked(item) }
        }
    }
}