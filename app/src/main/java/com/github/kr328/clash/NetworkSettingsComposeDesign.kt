package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import kotlinx.coroutines.launch

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
            PageTheme {
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
        val vpnOptionsEnabled = enableVpn && !running

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.network)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
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
                item {
                    SwitchItem(
                        icon = Icons.Default.VpnLock,
                        title = context.getString(com.github.kr328.clash.design.R.string.route_system_traffic),
                        summary = context.getString(com.github.kr328.clash.design.R.string.routing_via_vpn_service),
                        checked = enableVpn,
                        enabled = !running,
                    ) {
                        enableVpn = it
                        uiStore.enableVpn = it
                    }
                }
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.vpn_service_options)) }
                item {
                    SwitchItem(Icons.Default.NetworkCheck, context.getString(com.github.kr328.clash.design.R.string.bypass_private_network), context.getString(com.github.kr328.clash.design.R.string.bypass_private_network_summary), bypassPrivateNetwork, vpnOptionsEnabled) {
                        bypassPrivateNetwork = it
                        srvStore.bypassPrivateNetwork = it
                    }
                }
                item {
                    SwitchItem(Icons.Default.Dns, context.getString(com.github.kr328.clash.design.R.string.dns_hijacking), context.getString(com.github.kr328.clash.design.R.string.dns_hijacking_summary), dnsHijacking, vpnOptionsEnabled) {
                        dnsHijacking = it
                        srvStore.dnsHijacking = it
                    }
                }
                item {
                    SwitchItem(Icons.Default.Security, context.getString(com.github.kr328.clash.design.R.string.allow_bypass), context.getString(com.github.kr328.clash.design.R.string.allow_bypass_summary), allowBypass, vpnOptionsEnabled) {
                        allowBypass = it
                        srvStore.allowBypass = it
                    }
                }
                item {
                    SwitchItem(Icons.Default.Language, context.getString(com.github.kr328.clash.design.R.string.allow_ipv6), context.getString(com.github.kr328.clash.design.R.string.allow_ipv6_summary), allowIpv6, vpnOptionsEnabled) {
                        allowIpv6 = it
                        srvStore.allowIpv6 = it
                    }
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    item {
                        SwitchItem(Icons.Default.SettingsEthernet, context.getString(com.github.kr328.clash.design.R.string.system_proxy), context.getString(com.github.kr328.clash.design.R.string.system_proxy_summary), systemProxy, vpnOptionsEnabled) {
                            systemProxy = it
                            srvStore.systemProxy = it
                        }
                    }
                }
                item {
                    ClickItem(
                        icon = Icons.Default.SettingsEthernet,
                        title = context.getString(com.github.kr328.clash.design.R.string.tun_stack_mode),
                        summary = tunStackText(tunStackMode),
                        enabled = vpnOptionsEnabled,
                    ) {
                        showTunStackDialog = true
                    }
                }
                item {
                    ClickItem(
                        icon = Icons.Default.Apps,
                        title = context.getString(com.github.kr328.clash.design.R.string.access_control_mode),
                        summary = accessModeText(accessControlMode),
                        enabled = vpnOptionsEnabled,
                    ) {
                        showAccessModeDialog = true
                    }
                }
                item {
                    ClickItem(
                        icon = Icons.Default.Apps,
                        title = context.getString(com.github.kr328.clash.design.R.string.access_control_packages),
                        summary = context.getString(com.github.kr328.clash.design.R.string.access_control_packages_summary),
                    ) {
                        requests.trySend(Request.StartAccessControlList)
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

        AlertDialog(
            onDismissRequest = { showAccessModeDialog = false },
            confirmButton = {
                TextButton(onClick = { showAccessModeDialog = false }) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.cancel))
                }
            },
            title = { Text(context.getString(com.github.kr328.clash.design.R.string.access_control_mode)) },
            text = {
                LazyColumn {
                    items(values.size) { index ->
                        val value = values[index]

                        ListItem(
                            headlineContent = { Text(accessModeText(value)) },
                            trailingContent = {
                                RadioButton(
                                    selected = accessControlMode == value,
                                    onClick = {
                                        accessControlMode = value
                                        srvStore.accessControlMode = value
                                        showAccessModeDialog = false
                                    },
                                )
                            },
                        )
                    }
                }
            },
        )
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
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.cancel))
                }
            },
            title = { Text(title) },
            text = {
                LazyColumn {
                    items(values.size) { index ->
                        ListItem(
                            headlineContent = { Text(labels[index]) },
                            trailingContent = {
                                RadioButton(
                                    selected = selected == values[index],
                                    onClick = { onSelected(values[index]) },
                                )
                            },
                        )
                    }
                }
            },
        )
    }

    @Composable
    private fun CategoryTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SwitchItem(
        icon: ImageVector,
        title: String,
        summary: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChanged: (Boolean) -> Unit,
    ) {
        Card(
            enabled = enabled,
            onClick = { onChanged(!checked) },
        ) {
            ListItem(
                leadingContent = { Icon(icon, contentDescription = null) },
                headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                trailingContent = {
                    Switch(
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = onChanged,
                    )
                },
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ClickItem(
        icon: ImageVector,
        title: String,
        summary: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        Card(
            enabled = enabled,
            onClick = onClick,
        ) {
            ListItem(
                leadingContent = { Icon(icon, contentDescription = null) },
                headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            )
        }
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
