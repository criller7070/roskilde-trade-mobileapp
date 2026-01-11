package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.databinding.FragmentWallBinding
import dk.rosswap.mobile.feature.items.presentation.ItemsAdapter
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class ItemListFragment : Fragment() {

    private var _binding: FragmentWallBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemListViewModel by viewModels()

    private lateinit var adapter: ItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ItemsAdapter(
            onMessageClicked = {
                // TODO: navigate to chat/item page
            },
            onLikeClicked = { item ->
                viewModel.likeItem(item)
                lifecycleScope.launch {
                    PopupBus.showSuccess("Added to Liked Posts")
                }
            }
        )

        binding.recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPosts.adapter = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                lifecycleScope.launch {
                    PopupBus.showError(msg)
                }
            }
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
