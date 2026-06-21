package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.Behavior
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.store.ServiceStore

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
            PageTheme {
                PageContent()
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.app)) },
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
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.behavior)) }
                item {
                    SwitchItem(
                        icon = Icons.Default.Restore,
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
                        icon = Icons.Default.Brightness4,
                        title = context.getString(com.github.kr328.clash.design.R.string.dark_mode),
                        summary = darkModeText(darkMode),
                    ) {
                        showDarkModeDialog = true
                    }
                }
                item {
                    SwitchItem(
                        icon = Icons.Default.VisibilityOff,
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
                        icon = Icons.Default.StackedBarChart,
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
                        icon = Icons.Default.Domain,
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

        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            confirmButton = {
                TextButton(onClick = { showDarkModeDialog = false }) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.cancel))
                }
            },
            title = { Text(context.getString(com.github.kr328.clash.design.R.string.dark_mode)) },
            text = {
                LazyColumn {
                    items(values.size) { index ->
                        val value = values[index]

                        ListItem(
                            headlineContent = { Text(darkModeText(value)) },
                            trailingContent = {
                                RadioButton(
                                    selected = darkMode == value,
                                    onClick = {
                                        darkMode = value
                                        uiStore.darkMode = value
                                        showDarkModeDialog = false
                                        requests.trySend(Request.ReCreateAllActivities)
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
        onClick: () -> Unit,
    ) {
        Card(onClick = onClick) {
            ListItem(
                leadingContent = { Icon(icon, contentDescription = null) },
                headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            )
        }
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
