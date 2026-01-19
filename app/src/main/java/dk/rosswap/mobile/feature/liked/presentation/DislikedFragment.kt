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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_posts)

        adapter = DislikedAdapter(
            onItemClick = { item ->
                // Navigate to the item detail screen with the same args as ItemListFragment
                findNavController().navigate(R.id.nav_item_detail, item.toDetailBundle())
            },
            onLikeAgainClicked = { item ->
                viewModel.likeAgain(item)
                Toast.makeText(context, "Moved to Liked Posts", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter

        viewModel.dislikedItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
