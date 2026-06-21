package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SimpleComposeDesign(
    context: Context,
    private val title: String,
    entries: List<Entry> = emptyList(),
) : Design<SimpleComposeDesign.Request>(context) {
    sealed class Request {
        data class OpenUrl(val url: String) : Request()
    }

    sealed class Entry {
        data class Tip(val text: String) : Entry()
        data class Category(val title: String) : Entry()
        data class Link(val title: String, val summary: String, val url: String) : Entry()
    }

    private var entries by mutableStateOf(entries)
    private var bodyText by mutableStateOf<String?>(null)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    fun patchEntries(value: List<Entry>) {
        entries = value
    }

    fun patchBodyText(value: String) {
        bodyText = value
    }

    @Composable
    private fun PageContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = title,
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
            val text = bodyText

            if (text != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp,
                    ),
                ) {
                    item {
                        Card {
                            Text(
                                text = text,
                                modifier = Modifier.padding(16.dp),
                                style = MiuixTheme.textStyles.body2,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(entries) { entry ->
                        when (entry) {
                            is Entry.Tip -> TipItem(entry.text)
                            is Entry.Category -> CategoryItem(entry.title)
                            is Entry.Link -> LinkItem(entry)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TipItem(text: String) {
        Card {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                style = MiuixTheme.textStyles.body1,
            )
        }
    }

    @Composable
    private fun CategoryItem(title: String) {
        Text(
            text = title,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.primary,
        )
    }

    @Composable
    private fun LinkItem(entry: Entry.Link) {
        Card(onClick = { requests.trySend(Request.OpenUrl(entry.url)) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = entry.title,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.summary,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = MiuixIcons.Forward,
                    contentDescription = null,
                )
            }
        }
    }
}
