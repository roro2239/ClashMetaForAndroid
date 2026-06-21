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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            PageTheme {
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
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.files)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { requests.trySend(Request.PopStack) },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        if (currentInBaseDir && configurationEditable) {
                            IconButton(onClick = { requests.trySend(Request.ImportFile(null)) }) {
                                Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = context.getString(com.github.kr328.clash.design.R.string._new),
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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

    @OptIn(ExperimentalMaterial3Api::class)
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
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Article,
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    if (!file.isDirectory) {
                        Text(
                            text = "${file.size.toBytesString()} · ${(now - file.lastModified).elapsedIntervalString(context)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = context.getString(com.github.kr328.clash.design.R.string.properties),
                        )
                    }
                    FileMenu(file, expanded) {
                        expanded = false
                    }
                },
            )
        }
    }

    @Composable
    private fun FileMenu(file: File, expanded: Boolean, dismiss: () -> Unit) {
        DropdownMenu(expanded = expanded, onDismissRequest = dismiss) {
            if (!file.isDirectory && (!currentInBaseDir || configurationEditable)) {
                DropdownMenuItem(
                    text = { Text(context.getString(com.github.kr328.clash.design.R.string.import_)) },
                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    onClick = {
                        requests.trySend(Request.ImportFile(file))
                        dismiss()
                    },
                )
            }
            if (!file.isDirectory && file.size > 0) {
                DropdownMenuItem(
                    text = { Text(context.getString(com.github.kr328.clash.design.R.string.export)) },
                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    onClick = {
                        requests.trySend(Request.ExportFile(file))
                        dismiss()
                    },
                )
            }
            if (!currentInBaseDir) {
                DropdownMenuItem(
                    text = { Text(context.getString(com.github.kr328.clash.design.R.string.rename)) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        requests.trySend(Request.RenameFile(file))
                        dismiss()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = context.getString(com.github.kr328.clash.design.R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
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
