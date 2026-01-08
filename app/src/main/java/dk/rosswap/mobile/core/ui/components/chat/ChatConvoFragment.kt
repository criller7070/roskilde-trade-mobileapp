package dk.rosswap.mobile.core.ui.components.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class ChatConvoFragment : Fragment() {

    private val viewModel: ChatConvoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext())
        textView.text = (viewModel.messages.value ?: emptyList()).joinToString("\n")
        return textView
    }
}
