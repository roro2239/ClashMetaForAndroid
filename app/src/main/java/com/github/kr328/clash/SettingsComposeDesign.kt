package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SettingsComposeDesign(context: Context) : Design<SettingsComposeDesign.Request>(context) {
    enum class Request {
        StartApp,
        StartNetwork,
        StartOverride,
        StartMetaFeature,
    }

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
                    title = context.getString(com.github.kr328.clash.design.R.string.settings),
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
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                item {
                    SettingsItem(com.github.kr328.clash.design.R.string.app, Request.StartApp)
                }
                item {
                    SettingsItem(com.github.kr328.clash.design.R.string.network, Request.StartNetwork)
                }
                item {
                    SettingsItem(com.github.kr328.clash.design.R.string.override, Request.StartOverride)
                }
                item {
                    SettingsItem(com.github.kr328.clash.design.R.string.meta_features, Request.StartMetaFeature)
                }
            }
        }
    }

    @Composable
    private fun SettingsItem(titleRes: Int, request: Request) {
        Surface(onClick = { requests.trySend(request) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Settings,
                    contentDescription = null,
                )
                Text(
                    text = context.getString(titleRes),
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }
        HorizontalDivider()
    }
}
