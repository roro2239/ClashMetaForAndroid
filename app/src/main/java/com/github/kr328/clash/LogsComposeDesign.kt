package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.format
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.theme.MiuixTheme

class LogsComposeDesign(context: Context) : Design<LogsComposeDesign.Request>(context) {
    sealed class Request {
        object StartLogcat : Request()
        object DeleteAll : Request()
        data class OpenFile(val file: LogFile) : Request()
    }

    private var logs by mutableStateOf<List<LogFile>>(emptyList())
    private var deleteAllDialogVisible by mutableStateOf(false)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    fun patchLogs(value: List<LogFile>) {
        logs = value
    }

    @Composable
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.logs),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
            actions = {
                IconButton(onClick = { requests.trySend(Request.StartLogcat) }) {
                    Icon(
                        imageVector = MiuixIcons.Notes,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.logcat),
                    )
                }
                IconButton(onClick = { deleteAllDialogVisible = true }) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.delete_all_logs),
                    )
                }
            },
        ) { innerPadding, nestedScrollConnection ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(logs, key = { it.fileName }) { log ->
                    Card(onClick = { requests.trySend(Request.OpenFile(log)) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = log.fileName,
                                style = MiuixTheme.textStyles.body1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = log.date.format(context),
                                style = MiuixTheme.textStyles.body2,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        if (deleteAllDialogVisible) {
            ClashMiuixDialog(
                title = context.getString(com.github.kr328.clash.design.R.string.delete_all_logs),
                message = context.getString(com.github.kr328.clash.design.R.string.delete_all_logs_warn),
                confirmText = context.getString(com.github.kr328.clash.design.R.string.ok),
                onConfirm = {
                    deleteAllDialogVisible = false
                    requests.trySend(Request.DeleteAll)
                },
                dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
                onDismissButton = { deleteAllDialogVisible = false },
                onDismissRequest = { deleteAllDialogVisible = false },
            )
        }
    }
}
