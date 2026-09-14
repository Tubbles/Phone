package org.fossify.phone.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.phone.R
import org.fossify.phone.activities.SimpleActivity
import org.fossify.phone.databinding.DialogEnterNumberBinding

class EnterNumberDialog(val activity: SimpleActivity, titleId: Int, currentValue: Int, callback: (value: Int) -> Unit) {

    init {
        val binding = DialogEnterNumberBinding.inflate(activity.layoutInflater).apply {
            enterNumberValue.setText(currentValue.toString())
            enterNumberValue.selectAll()
        }

        activity.getAlertDialogBuilder().setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null).apply {
            activity.setupDialogStuff(binding.root, this, titleId) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val enteredValue = binding.enterNumberValue.value.toIntOrNull()
                    if (enteredValue == null || enteredValue < 0) {
                        activity.toast(R.string.invalid_number)
                    } else {
                        callback(enteredValue)
                        alertDialog.dismiss()
                    }
                }
            }
        }
    }
}
