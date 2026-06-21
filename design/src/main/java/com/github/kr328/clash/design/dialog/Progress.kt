package com.github.kr328.clash.design.dialog

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kr328.clash.design.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ModelProgressBarConfigure {
    var isIndeterminate: Boolean
    var text: String?
    var progress: Int
    var max: Int
}

interface ModelProgressBarScope {
    suspend fun configure(block: suspend ModelProgressBarConfigure.() -> Unit)
}

suspend fun Context.withModelProgressBar(block: suspend ModelProgressBarScope.() -> Unit) {
    val progress = LinearProgressIndicator(this)
    val text = TextView(this)
    val view = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        val padding = resources.getDimensionPixelSize(R.dimen.item_tailing_margin)
        setPadding(padding, padding, padding, padding)
        addView(text)
        addView(progress)
    }
    val dialog = MaterialAlertDialogBuilder(this)
        .setCancelable(false)
        .setView(view)
        .show()

    val configureImpl = object : ModelProgressBarConfigure {
        override var isIndeterminate: Boolean
            get() = progress.isIndeterminate
            set(value) {
                progress.isIndeterminate = value
            }
        override var text: String?
            get() = text.text?.toString()
            set(value) {
                text.text = value
            }
        override var progress: Int
            get() = progress.progress
            set(value) {
                progress.setProgressCompat(value, true)
            }
        override var max: Int
            get() = progress.max
            set(value) {
                progress.max = value
            }

    }

    val scopeImpl = object : ModelProgressBarScope {
        override suspend fun configure(block: suspend ModelProgressBarConfigure.() -> Unit) {
            withContext(Dispatchers.Main) {
                configureImpl.block()
            }
        }
    }

    try {
        scopeImpl.block()
    } finally {
        dialog.dismiss()
    }
}
