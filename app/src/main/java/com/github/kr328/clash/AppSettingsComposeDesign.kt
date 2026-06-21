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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.Behavior
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

class AppSettingsComposeDesign(
    context: Context,
    private val srvStore: ServiceStore,
    private val behavior: Behavior,
    private val running: Boolean,
) : Design<AppSettingsComposeDesign.Request>(context) {
    enum class Request {
        ReCreateAllActivities
    }

    private var autoRestart by mutableStateOf(behavior.autoRestart)
    private var dynamicNotification by mutableStateOf(srvStore.dynamicNotification)

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
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.app),
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
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.behavior)) }
                item {
                    Card {
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
                }
                item { CategoryTitle(context.getString(com.github.kr328.clash.design.R.string.service)) }
                item {
                    Card {
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

}
