package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentLikedBinding

@AndroidEntryPoint
class LikedFragment : Fragment(R.layout.fragment_liked) {

    private var _binding: FragmentLikedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LikedViewModel by viewModels()

    private val adapter = SimpleStringAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLikedBinding.bind(view)

        // Setup recycler
        val recycler = binding.recyclerPosts
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Observe posts
        viewModel.posts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.btnDisliked.isEnabled = list.isNotEmpty()
        }

        // Load sample content so preview/device shows something
        viewModel.loadSamplePosts()

        binding.btnDisliked.setOnClickListener {
            // placeholder action - navigation or other logic can be added here
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Very small adapter that uses android simple list item to avoid depending on missing layouts.
    private class SimpleStringAdapter : ListAdapter<String, SimpleStringAdapter.VH>(StringDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.text.text = getItem(position)
        }

        class VH(val text: TextView) : RecyclerView.ViewHolder(text)
    }

    private class StringDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}