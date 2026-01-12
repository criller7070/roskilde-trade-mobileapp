package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.items.presentation.ItemsAdapter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DislikedFragment : Fragment(R.layout.fragment_disliked) {

    private val viewModel: DislikedViewModel by viewModels()
    private lateinit var adapter: ItemsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_disliked_items)
        
        adapter = ItemsAdapter(
            onLikeClick = { item, isLiked ->
                // TODO: Implement re-liking logic (move from disliked to liked)
                // For now just refresh the list
                viewModel.loadDislikedItems()
            },
            onItemClick = { item ->
                // TODO: Navigate to details
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dislikedItems.collect { items ->
                adapter.submitList(items)
            }
        }
    }
}
