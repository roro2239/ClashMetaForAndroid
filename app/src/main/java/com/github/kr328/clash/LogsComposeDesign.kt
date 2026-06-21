package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.format

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
            PageTheme {
                PageContent()
            }
        }
    }

    fun patchLogs(value: List<LogFile>) {
        logs = value
    }

    @Composable
    private fun PageTheme(content: @Composable () -> Unit) {
        val colors = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

        MaterialTheme(colorScheme = colors, content = content)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PageContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.logs)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { requests.trySend(Request.StartLogcat) }) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.logcat),
                            )
                        }
                        IconButton(onClick = { deleteAllDialogVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.delete_all_logs),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(logs, key = { it.fileName }) { log ->
                    Surface(onClick = { requests.trySend(Request.OpenFile(log)) }) {
                        ListItem(
                            headlineContent = {
                                Text(log.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(log.date.format(context), maxLines = 1)
                            },
                        )
                    }
                }
            }
        }

        if (deleteAllDialogVisible) {
            AlertDialog(
                onDismissRequest = { deleteAllDialogVisible = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteAllDialogVisible = false
                            requests.trySend(Request.DeleteAll)
                        }
                    ) {
                        Text(context.getString(com.github.kr328.clash.design.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteAllDialogVisible = false }) {
                        Text(context.getString(com.github.kr328.clash.design.R.string.cancel))
                    }
                },
                title = {
                    Text(context.getString(com.github.kr328.clash.design.R.string.delete_all_logs))
                },
                text = {
                    Text(context.getString(com.github.kr328.clash.design.R.string.delete_all_logs_warn))
                },
            )
        }
    }
}
