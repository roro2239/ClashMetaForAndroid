package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.coroutines.resume

class OverrideSettingsComposeDesign(
    context: Context,
    private val configuration: ConfigurationOverride,
) : Design<OverrideSettingsComposeDesign.Request>(context) {
    enum class Request {
        ResetOverride
    }

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
    private var content by mutableStateOf(json.encodeToString(configuration))
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = context.getString(com.github.kr328.clash.design.R.string.override),
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { requests.trySend(Request.ResetOverride) }) {
                            Icon(MiuixIcons.Reset, contentDescription = context.getString(com.github.kr328.clash.design.R.string.reset_override_settings))
                        }
                    },
                )
            },
        ) { innerPadding ->
            ResetConfirmDialog()

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
                                .heightIn(min = 360.dp),
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

    private fun applyContent() {
        runCatching {
            configuration.copyFrom(json.decodeFromString(ConfigurationOverride.serializer(), content))
            content = json.encodeToString(configuration)
        }.onFailure {
            Toast.makeText(context, it.message ?: it.javaClass.simpleName, Toast.LENGTH_LONG).show()
        }
    }
}

fun ConfigurationOverride.copyFrom(other: ConfigurationOverride) {
    httpPort = other.httpPort
    socksPort = other.socksPort
    redirectPort = other.redirectPort
    tproxyPort = other.tproxyPort
    mixedPort = other.mixedPort
    authentication = other.authentication
    allowLan = other.allowLan
    bindAddress = other.bindAddress
    mode = other.mode
    logLevel = other.logLevel
    ipv6 = other.ipv6
    externalController = other.externalController
    externalControllerTLS = other.externalControllerTLS
    externalControllerCors.allowOrigins = other.externalControllerCors.allowOrigins
    externalControllerCors.allowPrivateNetwork = other.externalControllerCors.allowPrivateNetwork
    secret = other.secret
    hosts = other.hosts
    unifiedDelay = other.unifiedDelay
    geodataMode = other.geodataMode
    tcpConcurrent = other.tcpConcurrent
    findProcessMode = other.findProcessMode
    dns.enable = other.dns.enable
    dns.preferH3 = other.dns.preferH3
    dns.listen = other.dns.listen
    dns.ipv6 = other.dns.ipv6
    dns.useHosts = other.dns.useHosts
    dns.enhancedMode = other.dns.enhancedMode
    dns.nameServer = other.dns.nameServer
    dns.fallback = other.dns.fallback
    dns.defaultServer = other.dns.defaultServer
    dns.fakeIpFilter = other.dns.fakeIpFilter
    dns.fakeIPFilterMode = other.dns.fakeIPFilterMode
    dns.fallbackFilter.geoIp = other.dns.fallbackFilter.geoIp
    dns.fallbackFilter.geoIpCode = other.dns.fallbackFilter.geoIpCode
    dns.fallbackFilter.ipcidr = other.dns.fallbackFilter.ipcidr
    dns.fallbackFilter.domain = other.dns.fallbackFilter.domain
    dns.nameserverPolicy = other.dns.nameserverPolicy
    app.appendSystemDns = other.app.appendSystemDns
    sniffer.enable = other.sniffer.enable
    sniffer.sniff.http.ports = other.sniffer.sniff.http.ports
    sniffer.sniff.http.overrideDestination = other.sniffer.sniff.http.overrideDestination
    sniffer.sniff.tls.ports = other.sniffer.sniff.tls.ports
    sniffer.sniff.tls.overrideDestination = other.sniffer.sniff.tls.overrideDestination
    sniffer.sniff.quic.ports = other.sniffer.sniff.quic.ports
    sniffer.sniff.quic.overrideDestination = other.sniffer.sniff.quic.overrideDestination
    sniffer.forceDnsMapping = other.sniffer.forceDnsMapping
    sniffer.parsePureIp = other.sniffer.parsePureIp
    sniffer.overrideDestination = other.sniffer.overrideDestination
    sniffer.forceDomain = other.sniffer.forceDomain
    sniffer.skipDomain = other.sniffer.skipDomain
    sniffer.skipSrcAddress = other.sniffer.skipSrcAddress
    sniffer.skipDstAddress = other.sniffer.skipDstAddress
    geoxurl.geoip = other.geoxurl.geoip
    geoxurl.mmdb = other.geoxurl.mmdb
    geoxurl.geosite = other.geoxurl.geosite
}
