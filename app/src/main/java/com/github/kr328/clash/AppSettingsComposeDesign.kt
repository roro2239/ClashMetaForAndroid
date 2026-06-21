package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.Behavior
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

class AppSettingsComposeDesign(
    context: Context,
    private val uiStore: UiStore,
    private val srvStore: ServiceStore,
    private val behavior: Behavior,
    private val running: Boolean,
    private val onHideIconChange: (hide: Boolean) -> Unit,
) : Design<AppSettingsComposeDesign.Request>(context) {
    enum class Request {
        ReCreateAllActivities
    }

    private var autoRestart by mutableStateOf(behavior.autoRestart)
    private var darkMode by mutableStateOf(uiStore.darkMode)
    private var hideAppIcon by mutableStateOf(uiStore.hideAppIcon)
    private var hideFromRecents by mutableStateOf(uiStore.hideFromRecents)
    private var dynamicNotification by mutableStateOf(srvStore.dynamicNotification)
    private var showDarkModeDialog by mutableStateOf(false)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    @Composable
    private fun PageContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = context.getString(com.github.kr328.clash.design.R.string.app),
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
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
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.behavior)) }
                item {
                    SwitchItem(
                        icon = MiuixIcons.Reset,
                        title = context.getString(com.github.kr328.clash.design.R.string.auto_restart),
                        summary = context.getString(com.github.kr328.clash.design.R.string.allow_clash_auto_restart),
                        checked = autoRestart,
                    ) {
                        autoRestart = it
                        behavior.autoRestart = it
                    }
                }
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.interface_)) }
                item {
                    ClickItem(
                        icon = MiuixIcons.Theme,
                        title = context.getString(com.github.kr328.clash.design.R.string.dark_mode),
                        summary = darkModeText(darkMode),
                    ) {
                        showDarkModeDialog = true
                    }
                }
                item {
                    SwitchItem(
                        icon = MiuixIcons.Hide,
                        title = context.getString(com.github.kr328.clash.design.R.string.hide_app_icon_title),
                        summary = context.getString(com.github.kr328.clash.design.R.string.hide_app_icon_desc),
                        checked = hideAppIcon,
                    ) {
                        hideAppIcon = it
                        uiStore.hideAppIcon = it
                        onHideIconChange(it)
                    }
                }
                item {
                    SwitchItem(
                        icon = MiuixIcons.Recent,
                        title = context.getString(com.github.kr328.clash.design.R.string.hide_from_recents_title),
                        summary = context.getString(com.github.kr328.clash.design.R.string.hide_from_recents_desc),
                        checked = hideFromRecents,
                    ) {
                        hideFromRecents = it
                        uiStore.hideFromRecents = it
                        requests.trySend(Request.ReCreateAllActivities)
                    }
                }
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.service)) }
                item {
                    SwitchItem(
                        icon = MiuixIcons.Settings,
                        title = context.getString(com.github.kr328.clash.design.R.string.show_traffic),
                        summary = context.getString(com.github.kr328.clash.design.R.string.show_traffic_summary),
                        checked = dynamicNotification,
                        enabled = !running,
                    ) {
                        dynamicNotification = it
                        srvStore.dynamicNotification = it
                    }
                }
            }
        }

        if (showDarkModeDialog) {
            DarkModeDialog()
        }
    }

    @Composable
    private fun DarkModeDialog() {
        val values = DarkMode.values()

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.dark_mode),
            dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
            onDismissButton = { showDarkModeDialog = false },
            onDismissRequest = { showDarkModeDialog = false },
        ) {
            LazyColumn {
                items(values.size) { index ->
                    val value = values[index]

                    RadioButtonPreference(
                        title = darkModeText(value),
                        selected = darkMode == value,
                        onClick = {
                            darkMode = value
                            uiStore.darkMode = value
                            showDarkModeDialog = false
                            requests.trySend(Request.ReCreateAllActivities)
                        },
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
        onClick: () -> Unit,
    ) {
        ArrowPreference(
            title = title,
            summary = summary,
            startAction = { Icon(icon, contentDescription = null) },
            onClick = onClick,
        )
    }

    private fun darkModeText(value: DarkMode): String {
        return context.getString(
            when (value) {
                DarkMode.Auto -> com.github.kr328.clash.design.R.string.follow_system_android_10
                DarkMode.ForceLight -> com.github.kr328.clash.design.R.string.always_light
                DarkMode.ForceDark -> com.github.kr328.clash.design.R.string.always_dark
            },
        )
    }
}
