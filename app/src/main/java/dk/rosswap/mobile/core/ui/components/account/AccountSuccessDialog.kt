package dk.rosswap.mobile.core.ui.components.account

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dk.rosswap.mobile.databinding.DialogAccountSuccessBinding

class AccountSuccessDialog(
    private val onDismiss: () -> Unit
) : DialogFragment() {

    private var _binding: DialogAccountSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAccountSuccessBinding.inflate(LayoutInflater.from(context))

        binding.btnOk.setOnClickListener {
            dismiss()
            onDismiss()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
            onDismiss()
        }

        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
