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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
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
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.settings),
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
        Card(
            modifier = Modifier.padding(bottom = 10.dp),
            onClick = { requests.trySend(request) },
        ) {
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
    }
}
