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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.dialog.ModelProgressBarConfigure
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.dialog.withModelProgressBar
import com.github.kr328.clash.design.util.ValidatorAgeSecretKey
import com.github.kr328.clash.design.util.ValidatorAutoUpdateInterval
import com.github.kr328.clash.design.util.ValidatorHttpUrl
import com.github.kr328.clash.design.util.ValidatorNotBlank
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class PropertiesComposeDesign(context: Context) : Design<PropertiesComposeDesign.Request>(context) {
    sealed class Request {
        object Commit : Request()
        object BrowseFiles : Request()
    }

    var profile by mutableStateOf<Profile?>(null)

    var progressing by mutableStateOf(false)
        private set
    private var exitConfirm by mutableStateOf<CancellableContinuation<Boolean>?>(null)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    suspend fun withProcessing(executeTask: suspend (suspend (FetchStatus) -> Unit) -> Unit) {
        try {
            progressing = true

            context.withModelProgressBar {
                configure {
                    isIndeterminate = true
                    text = context.getString(com.github.kr328.clash.design.R.string.initializing)
                }

                executeTask {
                    configure {
                        applyFrom(it)
                    }
                }
            }
        } finally {
            progressing = false
        }
    }

    suspend fun requestExitWithoutSaving(): Boolean {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { ctx ->
                exitConfirm = ctx
                ctx.invokeOnCancellation { exitConfirm = null }
            }
        }
    }

    fun inputName() {
        val current = profile ?: return

        launch {
            val name = context.requestModelTextInput(
                initial = current.name,
                title = context.getText(com.github.kr328.clash.design.R.string.name),
                hint = context.getText(com.github.kr328.clash.design.R.string.properties),
                error = context.getText(com.github.kr328.clash.design.R.string.should_not_be_blank),
                validator = ValidatorNotBlank,
            )

            if (name != current.name) {
                profile = current.copy(name = name)
            }
        }
    }

    fun inputUrl() {
        val current = profile ?: return

        if (current.type == Profile.Type.External) return

        launch {
            val url = context.requestModelTextInput(
                initial = current.source,
                title = context.getText(com.github.kr328.clash.design.R.string.url),
                hint = context.getText(com.github.kr328.clash.design.R.string.profile_url),
                error = context.getText(com.github.kr328.clash.design.R.string.accept_http_content),
                validator = ValidatorHttpUrl,
            )

            if (url != current.source) {
                profile = current.copy(source = url)
            }
        }
    }

    fun inputAgeSecretKey() {
        val current = profile ?: return

        launch {
            val ageSecretKey = context.requestModelTextInput(
                initial = current.ageSecretKey ?: "",
                title = context.getText(com.github.kr328.clash.design.R.string.age_secret_key),
                hint = context.getText(com.github.kr328.clash.design.R.string.age_secret_key_hint),
                error = context.getText(com.github.kr328.clash.design.R.string.age_secret_key_error),
                validator = ValidatorAgeSecretKey,
            )

            val newKey = ageSecretKey.ifBlank { null }
            if (newKey != current.ageSecretKey) {
                profile = current.copy(ageSecretKey = newKey)
            }
        }
    }

    fun inputInterval() {
        val current = profile ?: return

        launch {
            var minutes = TimeUnit.MILLISECONDS.toMinutes(current.interval)

            minutes = context.requestModelTextInput(
                initial = if (minutes == 0L) "" else minutes.toString(),
                title = context.getText(com.github.kr328.clash.design.R.string.auto_update),
                hint = context.getText(com.github.kr328.clash.design.R.string.auto_update_minutes),
                error = context.getText(com.github.kr328.clash.design.R.string.at_least_15_minutes),
                validator = ValidatorAutoUpdateInterval,
            ).toLongOrNull() ?: 0

            val interval = TimeUnit.MINUTES.toMillis(minutes)

            if (interval != current.interval) {
                profile = current.copy(interval = interval)
            }
        }
    }

    fun requestCommit() {
        requests.trySend(Request.Commit)
    }

    fun requestBrowseFiles() {
        requests.trySend(Request.BrowseFiles)
    }

    @Composable
    private fun PageContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = context.getString(com.github.kr328.clash.design.R.string.properties),
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        if (progressing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(onClick = { requestCommit() }) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = context.getString(com.github.kr328.clash.design.R.string.save),
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            ExitConfirmDialog()

            val current = profile

            if (current != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            PaddingValues(
                                start = 16.dp,
                                top = innerPadding.calculateTopPadding() + 12.dp,
                                end = 16.dp,
                                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                            ),
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = context.getString(com.github.kr328.clash.design.R.string.tips_properties)
                            .replace(Regex("<[^>]+>"), ""),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                    FieldItem(
                        icon = Icons.Default.Label,
                        title = context.getString(com.github.kr328.clash.design.R.string.name),
                        value = current.name,
                        onClick = { inputName() },
                    )
                    FieldItem(
                        icon = Icons.Default.Link,
                        title = context.getString(com.github.kr328.clash.design.R.string.url),
                        value = current.source.ifBlank {
                            context.getString(com.github.kr328.clash.design.R.string.accept_http_content)
                        },
                        enabled = current.type != Profile.Type.File,
                        onClick = { inputUrl() },
                    )
                    FieldItem(
                        icon = Icons.Default.Key,
                        title = context.getString(com.github.kr328.clash.design.R.string.age_secret_key),
                        value = current.ageSecretKey ?: context.getString(com.github.kr328.clash.design.R.string.age_secret_key_hint),
                        onClick = { inputAgeSecretKey() },
                    )
                    FieldItem(
                        icon = Icons.Default.Update,
                        title = context.getString(com.github.kr328.clash.design.R.string.auto_update),
                        value = if (current.interval == 0L) {
                            context.getString(com.github.kr328.clash.design.R.string.disabled)
                        } else {
                            context.getString(
                                com.github.kr328.clash.design.R.string.format_minutes,
                                TimeUnit.MILLISECONDS.toMinutes(current.interval),
                            )
                        },
                        enabled = current.type != Profile.Type.File,
                        onClick = { inputInterval() },
                    )
                    FieldItem(
                        icon = Icons.Default.FolderOpen,
                        title = context.getString(com.github.kr328.clash.design.R.string.browse_files),
                        value = context.getString(com.github.kr328.clash.design.R.string.browse_configuration_providers),
                        onClick = { requestBrowseFiles() },
                    )
                }
            }
        }
    }

    @Composable
    private fun ExitConfirmDialog() {
        val continuation = exitConfirm ?: return

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.exit_without_save),
            message = context.getString(com.github.kr328.clash.design.R.string.exit_without_save_warning),
            confirmText = context.getString(com.github.kr328.clash.design.R.string.ok),
            onConfirm = { finishExitConfirm(continuation, true) },
            dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
            onDismissButton = { finishExitConfirm(continuation, false) },
            onDismissRequest = { finishExitConfirm(continuation, false) },
        )
    }

    private fun finishExitConfirm(continuation: CancellableContinuation<Boolean>, confirmed: Boolean) {
        exitConfirm = null
        if (!continuation.isCompleted) {
            continuation.resume(confirmed)
        }
    }

    @Composable
    private fun FieldItem(
        icon: ImageVector,
        title: String,
        value: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (enabled) onClick else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = icon, contentDescription = null)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = value,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    private fun ModelProgressBarConfigure.applyFrom(status: FetchStatus) {
        when (status.action) {
            FetchStatus.Action.FetchConfiguration -> {
                text = context.getString(com.github.kr328.clash.design.R.string.format_fetching_configuration, status.args[0])
                isIndeterminate = true
            }
            FetchStatus.Action.FetchProviders -> {
                text = context.getString(com.github.kr328.clash.design.R.string.format_fetching_provider, status.args[0])
                isIndeterminate = false
                max = status.max
                progress = status.progress
            }
            FetchStatus.Action.SubscriptionInfo -> Unit
            FetchStatus.Action.Verifying -> {
                text = context.getString(com.github.kr328.clash.design.R.string.verifying)
                isIndeterminate = false
                max = status.max
                progress = status.progress
            }
        }
    }
}
