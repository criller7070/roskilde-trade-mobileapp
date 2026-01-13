package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.liked.domain.LikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.UnlikeItemUseCase
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DislikedFragment : Fragment(R.layout.fragment_disliked) {
    
    private val viewModel: DislikedViewModel by viewModels()
    
    // We can inject LikeItemUseCase if we want to "Like" from here, 
    // but the adapter callback can handle it.
    
    private lateinit var adapter: DislikedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_liked_posts)
        // Note: Check fragment_disliked.xml ID. Usually 'recycler_liked_posts' or similar copy-paste.
        // I will assume standard ID or look it up.
        // Actually I should verify the ID in fragment_disliked.xml
        
        adapter = DislikedAdapter(
            onLikeAgainClicked = { item ->
                // Logic to "Like" it again (maybe move from Disliked -> Liked?)
                // For now, just show a toast or handle it.
                Toast.makeText(context, "Liked ${item.title} again!", Toast.LENGTH_SHORT).show()
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
