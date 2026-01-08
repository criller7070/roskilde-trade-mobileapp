package dk.rosswap.mobile.core.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dk.rosswap.mobile.databinding.FragmentChatconvoBinding


class ChatConvoFragment : Fragment() {

    private var _binding: FragmentChatconvoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Inflate fragment_chatconvo.xml using ViewBinding
        _binding = FragmentChatconvoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null         // Clear binding reference to avoid memory leaks

    }
}