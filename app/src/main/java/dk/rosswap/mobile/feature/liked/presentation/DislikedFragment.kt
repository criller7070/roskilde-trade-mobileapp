package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.utils.toDetailBundle

@AndroidEntryPoint
class DislikedFragment : Fragment(R.layout.fragment_disliked) {

    private val viewModel: DislikedViewModel by viewModels()
    private lateinit var adapter: DislikedAdapter
    private var recyclerView: RecyclerView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById<RecyclerView>(R.id.recycler_posts)

        adapter = DislikedAdapter(
            // Adapter provides DislikedItem objects; handle them accordingly
            onItemClick = { dislikedItem ->
                // Navigate to item detail
                findNavController().navigate(R.id.action_nav_disliked_to_itemDetail, dislikedItem.item.toDetailBundle())
            },
            onLikeAgainClicked = { dislikedItem ->
                viewModel.likeAgain(dislikedItem)
                Toast.makeText(context, "Moved to Liked Posts", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter

        viewModel.dislikedItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            // Scroll to top so the user sees the most recent reset/changes when navigating here
            if (items.isNotEmpty()) {
                recyclerView?.scrollToPosition(0)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Always refresh when fragment becomes visible so navigation from other screens produces a fresh view
        viewModel.refresh()
    }
}
