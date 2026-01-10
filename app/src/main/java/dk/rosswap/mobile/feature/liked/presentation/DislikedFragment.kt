package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentDislikedBinding

@AndroidEntryPoint
class DislikedFragment : Fragment(R.layout.fragment_disliked) {

    private var _binding: FragmentDislikedBinding? = null
    private val binding: FragmentDislikedBinding
        get() = _binding!!

    private val viewModel: DislikedViewModel by viewModels()

    private lateinit var adapter: RecyclerView.Adapter<*>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDislikedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        // TODO: Observe viewModel data and submit it to the adapter when available.
    }

    private fun setupRecyclerView() {
        // Simple placeholder adapter; replace with project-specific adapter as needed.
        adapter = object : RecyclerView.Adapter<SimpleViewHolder>() {
            private val items: List<String> = emptyList()

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                return SimpleViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
                // No-op for now; bind data here when items are available.
            }

            override fun getItemCount(): Int = items.size
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}