package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentLikedBinding
import dk.rosswap.mobile.core.utils.toDetailBundle

@AndroidEntryPoint
class LikedFragment : Fragment(R.layout.fragment_liked) {

    private var _binding: FragmentLikedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LikedViewModel by viewModels()
    private lateinit var adapter: LikedItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLikedBinding.bind(view)

        setupRecyclerView()
        observeData()

        binding.btnDisliked.setOnClickListener {
            // Navigate to the Disliked screen
            findNavController().navigate(R.id.nav_disliked)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure we refresh data when the screen becomes visible,
        // catching any new likes from the feed.
        viewModel.loadLikedPosts()
    }

    private fun setupRecyclerView() {
        adapter = LikedItemAdapter(
            onItemClick = { item ->
                findNavController().navigate(R.id.nav_item_detail, item.toDetailBundle())
            },
            onUnlikeClick = { item ->
                viewModel.unlikePost(item)
            }
        )

        binding.recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPosts.adapter = adapter
    }

    private fun observeData() {
        viewModel.likedPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.emptyStateGroup.isVisible = posts.isEmpty()
            binding.recyclerPosts.isVisible = posts.isNotEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
