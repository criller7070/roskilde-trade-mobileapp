package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dk.rosswap.mobile.databinding.FragmentSwipePageBinding

class SwipeFragment : Fragment() {

    private var _binding: FragmentSwipePageBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val swipeViewModel =
            ViewModelProvider(this)[SwipeViewModel::class.java]

        _binding = FragmentSwipePageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

