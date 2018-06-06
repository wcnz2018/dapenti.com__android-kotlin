package com.willchou.dapenti.view

import android.app.AlertDialog
import android.content.Context

class ConfirmDialog(private var context: Context, var title: String?, var message: String?) {
    interface ClickEventListener {
        fun confirmed()
    }

    var clickEventListener: ClickEventListener? = null

    fun show() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            clickEventListener?.confirmed()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }
}
