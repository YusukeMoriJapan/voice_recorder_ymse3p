package ymse3p.app.voicelogger.ui.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DeleteAlertDialogFragment : DialogFragment() {

    private lateinit var listener: DeleteAlertDialogListener

    interface DeleteAlertDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    /** FragmentにActivityをアタッチする前に呼び出すことは禁止。
     * アタッチ前に呼び出した場合は、IllegalStateExceptionをthrowする
     * @throws IllegalStateException if not currently associated with
     * an activity or if associated only with a context.*/
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireActivity()).apply {
            setMessage("本当に削除しますか？")
            setPositiveButton("はい") { dialog, which ->
                listener.onDialogPositiveClick(this@DeleteAlertDialogFragment)
            }
            setNegativeButton("いいえ") { dialog, which ->
                listener.onDialogNegativeClick(this@DeleteAlertDialogFragment)
            }
        }

        return builder.create()
    }

    companion object {
        fun create(listenerArg: DeleteAlertDialogListener): DeleteAlertDialogFragment =
            DeleteAlertDialogFragment().apply {
                isCancelable = false
                listener = listenerArg
            }
    }
}