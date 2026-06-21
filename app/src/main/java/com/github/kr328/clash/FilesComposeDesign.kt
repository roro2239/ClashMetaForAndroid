package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.design.util.ValidatorFileName
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toBytesString
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixMenuItem
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme

class FilesComposeDesign(context: Context) : Design<FilesComposeDesign.Request>(context) {
    sealed class Request {
        data class OpenFile(val file: File) : Request()
        data class OpenDirectory(val file: File) : Request()
        data class RenameFile(val file: File) : Request()
        data class DeleteFile(val file: File) : Request()
        data class ImportFile(val file: File?) : Request()
        data class ExportFile(val file: File) : Request()

        object PopStack : Request()
    }

    private var files by mutableStateOf<List<File>>(emptyList())
    private var currentInBaseDir by mutableStateOf(true)
    private var now by mutableLongStateOf(System.currentTimeMillis())

    var configurationEditable by mutableStateOf(false)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    suspend fun swapFiles(files: List<File>, currentInBaseDir: Boolean) {
        withContext(Dispatchers.Main) {
            this@FilesComposeDesign.files = files
            this@FilesComposeDesign.currentInBaseDir = currentInBaseDir
        }
    }

    fun updateElapsed() {
        now = System.currentTimeMillis()
    }

    suspend fun requestFileName(name: String): String {
        return context.requestModelTextInput(
            initial = name,
            title = context.getText(com.github.kr328.clash.design.R.string.file_name),
            hint = context.getText(com.github.kr328.clash.design.R.string.file_name),
            error = context.getText(com.github.kr328.clash.design.R.string.invalid_file_name),
            validator = ValidatorFileName,
        )
    }

    @Composable
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.files),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { requests.trySend(Request.PopStack) },
            actions = {
                if (currentInBaseDir && configurationEditable) {
                    IconButton(onClick = { requests.trySend(Request.ImportFile(null)) }) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = context.getString(com.github.kr328.clash.design.R.string._new),
                        )
                    }
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
                items(files, key = { file -> file.id }) { file ->
                    FileItem(file)
                }
            }
        }
    }

    @Composable
    private fun FileItem(file: File) {
        var expanded by androidx.compose.runtime.remember { mutableStateOf(false) }

        Card(
            onClick = {
                if (file.isDirectory) {
                    requests.trySend(Request.OpenDirectory(file))
                } else {
                    requests.trySend(Request.OpenFile(file))
                }
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (file.isDirectory) MiuixIcons.Folder else MiuixIcons.File,
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = file.name,
                        style = MiuixTheme.textStyles.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!file.isDirectory) {
                        Text(
                            text = "${file.size.toBytesString()} · ${(now - file.lastModified).elapsedIntervalString(context)}",
                            style = MiuixTheme.textStyles.body2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = MiuixIcons.More,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.properties),
                    )
                }
                FileMenu(file, expanded) {
                    expanded = false
                }
            }
        }
    }

    @Composable
    private fun FileMenu(file: File, expanded: Boolean, dismiss: () -> Unit) {
        if (!expanded) return

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.properties),
            onDismissRequest = dismiss,
        ) {
            if (!file.isDirectory && (!currentInBaseDir || configurationEditable)) {
                ClashMiuixMenuItem(
                    title = context.getString(com.github.kr328.clash.design.R.string.import_),
                    leadingContent = { Icon(MiuixIcons.FileDownloads, contentDescription = null) },
                    onClick = {
                        requests.trySend(Request.ImportFile(file))
                        dismiss()
                    },
                )
            }
            if (!file.isDirectory && file.size > 0) {
                ClashMiuixMenuItem(
                    title = context.getString(com.github.kr328.clash.design.R.string.export),
                    leadingContent = { Icon(MiuixIcons.UploadCloud, contentDescription = null) },
                    onClick = {
                        requests.trySend(Request.ExportFile(file))
                        dismiss()
                    },
                )
            }
            if (!currentInBaseDir) {
                ClashMiuixMenuItem(
                    title = context.getString(com.github.kr328.clash.design.R.string.rename),
                    leadingContent = { Icon(MiuixIcons.Edit, contentDescription = null) },
                    onClick = {
                        requests.trySend(Request.RenameFile(file))
                        dismiss()
                    },
                )
                ClashMiuixMenuItem(
                    title = context.getString(com.github.kr328.clash.design.R.string.delete),
                    titleColor = MiuixTheme.colorScheme.error,
                    leadingContent = {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        requests.trySend(Request.DeleteFile(file))
                        dismiss()
                    },
                )
            }
        }
    }
}
