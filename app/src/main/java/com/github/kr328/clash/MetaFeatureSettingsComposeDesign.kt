package com.github.kr328.clash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.coroutines.resume

class MetaFeatureSettingsComposeDesign(
    context: Context,
    private val configuration: ConfigurationOverride,
) : Design<MetaFeatureSettingsComposeDesign.Request>(context) {
    enum class Request {
        ResetOverride, ImportGeoIp, ImportGeoSite, ImportCountry, ImportASN
    }

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
    private var content by mutableStateOf(json.encodeToString(configuration))
    private var secretKey by mutableStateOf("")
    private var publicKey by mutableStateOf("")
    private var resetConfirm by mutableStateOf<CancellableContinuation<Boolean>?>(null)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    suspend fun requestResetConfirm(): Boolean {
        return suspendCancellableCoroutine { ctx ->
            resetConfirm = ctx
            ctx.invokeOnCancellation { resetConfirm = null }
        }
    }

    @Composable
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.meta_features),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
            actions = {
                IconButton(onClick = { requests.trySend(Request.ResetOverride) }) {
                    Icon(MiuixIcons.Reset, contentDescription = context.getString(com.github.kr328.clash.design.R.string.reset_override_settings))
                }
            },
        ) { innerPadding, nestedScrollConnection ->
            ResetConfirmDialog()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        PaddingValues(
                            start = 16.dp,
                            top = innerPadding.calculateTopPadding() + 16.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AgeKeyCard()
                GeoFilesCard()
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp),
                            textStyle = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { applyContent() },
                        ) {
                            Icon(MiuixIcons.Ok, contentDescription = null)
                            Text(text = context.getString(com.github.kr328.clash.design.R.string.save))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ResetConfirmDialog() {
        val continuation = resetConfirm ?: return

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.reset_override_settings),
            message = context.getString(com.github.kr328.clash.design.R.string.reset_override_settings_message),
            confirmText = context.getString(com.github.kr328.clash.design.R.string.ok),
            onConfirm = { finishResetConfirm(continuation, true) },
            dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
            onDismissButton = { finishResetConfirm(continuation, false) },
            onDismissRequest = { finishResetConfirm(continuation, false) },
        )
    }

    private fun finishResetConfirm(continuation: CancellableContinuation<Boolean>, confirmed: Boolean) {
        resetConfirm = null
        if (!continuation.isCompleted) {
            continuation.resume(confirmed)
        }
    }

    @Composable
    private fun AgeKeyCard() {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = context.getString(com.github.kr328.clash.design.R.string.age_key_category),
                    style = MiuixTheme.textStyles.title3,
                )
                TextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = context.getString(com.github.kr328.clash.design.R.string.age_secret_key),
                )
                TextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = context.getString(com.github.kr328.clash.design.R.string.age_public_key),
                )
                Button(modifier = Modifier.fillMaxWidth(), onClick = { generateAgeKey(false) }) {
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.age_key_type_x25519))
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = { generateAgeKey(true) }) {
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.age_key_type_hybrid))
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = { publicKey = Clash.toPublicKeys(secretKey).firstOrNull().orEmpty() }) {
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.age_key_to_public))
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = { copy("age_secret_key", secretKey) }) {
                    Icon(MiuixIcons.Copy, contentDescription = null)
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.age_key_copy))
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = { copy("age_public_key", publicKey) }) {
                    Icon(MiuixIcons.Copy, contentDescription = null)
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.age_key_copy))
                }
            }
        }
    }

    @Composable
    private fun GeoFilesCard() {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = context.getString(com.github.kr328.clash.design.R.string.geox_files),
                    style = MiuixTheme.textStyles.title3,
                )
                GeoButton(com.github.kr328.clash.design.R.string.import_geoip_file, Request.ImportGeoIp)
                GeoButton(com.github.kr328.clash.design.R.string.import_geosite_file, Request.ImportGeoSite)
                GeoButton(com.github.kr328.clash.design.R.string.import_country_file, Request.ImportCountry)
                GeoButton(com.github.kr328.clash.design.R.string.import_asn_file, Request.ImportASN)
            }
        }
    }

    @Composable
    private fun GeoButton(title: Int, request: Request) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { requests.trySend(request) },
        ) {
            Icon(MiuixIcons.FileDownloads, contentDescription = null)
            Text(text = context.getString(title))
        }
    }

    private fun applyContent() {
        runCatching {
            configuration.copyFrom(json.decodeFromString(ConfigurationOverride.serializer(), content))
            content = json.encodeToString(configuration)
        }.onFailure {
            android.widget.Toast.makeText(context, it.message ?: it.javaClass.simpleName, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun generateAgeKey(hybrid: Boolean) {
        val keyPair = if (hybrid) Clash.genHybridKeyPair() else Clash.genX25519KeyPair()

        secretKey = keyPair.secretKey
        publicKey = keyPair.publicKey
    }

    private fun copy(label: String, value: String) {
        if (value.isBlank()) return

        context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(label, value))
    }
}
