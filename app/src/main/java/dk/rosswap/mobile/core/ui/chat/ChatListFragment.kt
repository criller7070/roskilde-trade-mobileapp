package dk.rosswap.mobile.core.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class ChatListFragment : Fragment() {

    private val viewModel: ChatListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext())
        textView.text = "Chats: " + (viewModel.chats.value ?: emptyList()).joinToString(", ")
        return textView
    }
}
