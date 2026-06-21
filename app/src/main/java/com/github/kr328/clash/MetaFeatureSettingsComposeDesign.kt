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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.Design
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            PageTheme {
                PageContent()
            }
        }
    }

    suspend fun requestResetConfirm(): Boolean {
        return suspendCancellableCoroutine { ctx ->
            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(com.github.kr328.clash.design.R.string.reset_override_settings)
                .setMessage(com.github.kr328.clash.design.R.string.reset_override_settings_message)
                .setPositiveButton(com.github.kr328.clash.design.R.string.ok) { _, _ -> ctx.resume(true) }
                .setNegativeButton(com.github.kr328.clash.design.R.string.cancel) { _, _ -> }
                .show()

            dialog.setOnDismissListener {
                if (!ctx.isCompleted) ctx.resume(false)
            }
            ctx.invokeOnCancellation { dialog.dismiss() }
        }
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
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.meta_features)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { requests.trySend(Request.ResetOverride) }) {
                            Icon(Icons.Default.Restore, contentDescription = context.getString(com.github.kr328.clash.design.R.string.reset_override_settings))
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { applyContent() },
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Text(context.getString(com.github.kr328.clash.design.R.string.save))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AgeKeyCard() {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(context.getString(com.github.kr328.clash.design.R.string.age_key_category), style = MaterialTheme.typography.titleMedium)
                TextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(context.getString(com.github.kr328.clash.design.R.string.age_secret_key)) },
                )
                TextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(context.getString(com.github.kr328.clash.design.R.string.age_public_key)) },
                )
                Button(modifier = Modifier.fillMaxWidth(), onClick = { generateAgeKey(false) }) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.age_key_type_x25519))
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = { generateAgeKey(true) }) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.age_key_type_hybrid))
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { publicKey = Clash.toPublicKeys(secretKey).firstOrNull().orEmpty() }) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.age_key_to_public))
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { copy("age_secret_key", secretKey) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(context.getString(com.github.kr328.clash.design.R.string.age_key_copy))
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { copy("age_public_key", publicKey) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(context.getString(com.github.kr328.clash.design.R.string.age_key_copy))
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
                Text(context.getString(com.github.kr328.clash.design.R.string.geox_files), style = MaterialTheme.typography.titleMedium)
                GeoButton(com.github.kr328.clash.design.R.string.import_geoip_file, Request.ImportGeoIp)
                GeoButton(com.github.kr328.clash.design.R.string.import_geosite_file, Request.ImportGeoSite)
                GeoButton(com.github.kr328.clash.design.R.string.import_country_file, Request.ImportCountry)
                GeoButton(com.github.kr328.clash.design.R.string.import_asn_file, Request.ImportASN)
            }
        }
    }

    @Composable
    private fun GeoButton(title: Int, request: Request) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { requests.trySend(request) },
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Text(context.getString(title))
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
