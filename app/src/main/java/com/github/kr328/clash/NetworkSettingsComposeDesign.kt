package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

class NetworkSettingsComposeDesign(
    context: Context,
    private val uiStore: UiStore,
    private val srvStore: ServiceStore,
    private val running: Boolean,
) : Design<NetworkSettingsComposeDesign.Request>(context) {
    enum class Request {
        StartAccessControlList
    }

    private var enableVpn by mutableStateOf(uiStore.enableVpn)
    private var bypassPrivateNetwork by mutableStateOf(srvStore.bypassPrivateNetwork)
    private var dnsHijacking by mutableStateOf(srvStore.dnsHijacking)
    private var allowBypass by mutableStateOf(srvStore.allowBypass)
    private var allowIpv6 by mutableStateOf(srvStore.allowIpv6)
    private var systemProxy by mutableStateOf(srvStore.systemProxy)
    private var tunStackMode by mutableStateOf(srvStore.tunStackMode)
    private var accessControlMode by mutableStateOf(srvStore.accessControlMode)
    private var showTunStackDialog by mutableStateOf(false)
    private var showAccessModeDialog by mutableStateOf(false)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    init {
        if (running) {
            launch {
                showToast(com.github.kr328.clash.design.R.string.options_unavailable, ToastDuration.Indefinite)
            }
        }
    }

    @Composable
    private fun PageContent() {
        val vpnOptionsEnabled = enableVpn && !running

        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.network),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
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
                item {
                    Card {
                        SwitchItem(
                            icon = MiuixIcons.Lock,
                            title = context.getString(com.github.kr328.clash.design.R.string.route_system_traffic),
                            summary = context.getString(com.github.kr328.clash.design.R.string.routing_via_vpn_service),
                            checked = enableVpn,
                            enabled = !running,
                        ) {
                            enableVpn = it
                            uiStore.enableVpn = it
                        }
                    }
                }
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.vpn_service_options)) }
                item {
                    Card {
                        SwitchItem(MiuixIcons.Link, context.getString(com.github.kr328.clash.design.R.string.bypass_private_network), context.getString(com.github.kr328.clash.design.R.string.bypass_private_network_summary), bypassPrivateNetwork, vpnOptionsEnabled) {
                            bypassPrivateNetwork = it
                            srvStore.bypassPrivateNetwork = it
                        }
                        SwitchItem(MiuixIcons.Link, context.getString(com.github.kr328.clash.design.R.string.dns_hijacking), context.getString(com.github.kr328.clash.design.R.string.dns_hijacking_summary), dnsHijacking, vpnOptionsEnabled) {
                            dnsHijacking = it
                            srvStore.dnsHijacking = it
                        }
                        SwitchItem(MiuixIcons.Lock, context.getString(com.github.kr328.clash.design.R.string.allow_bypass), context.getString(com.github.kr328.clash.design.R.string.allow_bypass_summary), allowBypass, vpnOptionsEnabled) {
                            allowBypass = it
                            srvStore.allowBypass = it
                        }
                        SwitchItem(MiuixIcons.Translate, context.getString(com.github.kr328.clash.design.R.string.allow_ipv6), context.getString(com.github.kr328.clash.design.R.string.allow_ipv6_summary), allowIpv6, vpnOptionsEnabled) {
                            allowIpv6 = it
                            srvStore.allowIpv6 = it
                        }
                        if (Build.VERSION.SDK_INT >= 29) {
                            SwitchItem(MiuixIcons.Settings, context.getString(com.github.kr328.clash.design.R.string.system_proxy), context.getString(com.github.kr328.clash.design.R.string.system_proxy_summary), systemProxy, vpnOptionsEnabled) {
                                systemProxy = it
                                srvStore.systemProxy = it
                            }
                        }
                    }
                }
                item {
                    Card {
                        ClickItem(
                            icon = MiuixIcons.Settings,
                            title = context.getString(com.github.kr328.clash.design.R.string.tun_stack_mode),
                            summary = tunStackText(tunStackMode),
                            enabled = vpnOptionsEnabled,
                        ) {
                            showTunStackDialog = true
                        }
                        ClickItem(
                            icon = MiuixIcons.AppRecording,
                            title = context.getString(com.github.kr328.clash.design.R.string.access_control_mode),
                            summary = accessModeText(accessControlMode),
                            enabled = vpnOptionsEnabled,
                        ) {
                            showAccessModeDialog = true
                        }
                        ClickItem(
                            icon = MiuixIcons.AppRecording,
                            title = context.getString(com.github.kr328.clash.design.R.string.access_control_packages),
                            summary = context.getString(com.github.kr328.clash.design.R.string.access_control_packages_summary),
                        ) {
                            requests.trySend(Request.StartAccessControlList)
                        }
                    }
                }
            }
        }

        if (showTunStackDialog) {
            StringSelectDialog(
                title = context.getString(com.github.kr328.clash.design.R.string.tun_stack_mode),
                values = arrayOf("system", "gvisor", "mixed"),
                labels = arrayOf(
                    context.getString(com.github.kr328.clash.design.R.string.tun_stack_system),
                    context.getString(com.github.kr328.clash.design.R.string.tun_stack_gvisor),
                    context.getString(com.github.kr328.clash.design.R.string.tun_stack_mixed),
                ),
                selected = tunStackMode,
                onSelected = {
                    tunStackMode = it
                    srvStore.tunStackMode = it
                    showTunStackDialog = false
                },
                onDismiss = { showTunStackDialog = false },
            )
        }
        if (showAccessModeDialog) {
            AccessModeDialog()
        }
    }

    @Composable
    private fun AccessModeDialog() {
        val values = AccessControlMode.values()

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.access_control_mode),
            dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
            onDismissButton = { showAccessModeDialog = false },
            onDismissRequest = { showAccessModeDialog = false },
        ) {
            LazyColumn {
                items(values.size) { index ->
                    val value = values[index]

                    RadioButtonPreference(
                        title = accessModeText(value),
                        selected = accessControlMode == value,
                        onClick = {
                            accessControlMode = value
                            srvStore.accessControlMode = value
                            showAccessModeDialog = false
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun StringSelectDialog(
        title: String,
        values: Array<String>,
        labels: Array<String>,
        selected: String,
        onSelected: (String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        ClashMiuixDialog(
            title = title,
            dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
            onDismissButton = onDismiss,
            onDismissRequest = onDismiss,
        ) {
            LazyColumn {
                items(values.size) { index ->
                    RadioButtonPreference(
                        title = labels[index],
                        selected = selected == values[index],
                        onClick = { onSelected(values[index]) },
                    )
                }
            }
        }
    }

    @Composable
    private fun CategoryTitle(title: String) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.primary,
        )
    }

    @Composable
    private fun SwitchItem(
        icon: ImageVector,
        title: String,
        summary: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChanged: (Boolean) -> Unit,
    ) {
        SwitchPreference(
            checked = checked,
            onCheckedChange = onChanged,
            title = title,
            summary = summary,
            enabled = enabled,
            startAction = { Icon(icon, contentDescription = null) },
        )
    }

    @Composable
    private fun ClickItem(
        icon: ImageVector,
        title: String,
        summary: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        ArrowPreference(
            title = title,
            summary = summary,
            startAction = { Icon(icon, contentDescription = null) },
            enabled = enabled,
            onClick = onClick,
        )
    }

    private fun tunStackText(value: String): String {
        return context.getString(
            when (value) {
                "gvisor" -> com.github.kr328.clash.design.R.string.tun_stack_gvisor
                "mixed" -> com.github.kr328.clash.design.R.string.tun_stack_mixed
                else -> com.github.kr328.clash.design.R.string.tun_stack_system
            },
        )
    }

    private fun accessModeText(value: AccessControlMode): String {
        return context.getString(
            when (value) {
                AccessControlMode.AcceptAll -> com.github.kr328.clash.design.R.string.allow_all_apps
                AccessControlMode.AcceptSelected -> com.github.kr328.clash.design.R.string.allow_selected_apps
                AccessControlMode.DenySelected -> com.github.kr328.clash.design.R.string.deny_selected_apps
            },
        )
    }
}
