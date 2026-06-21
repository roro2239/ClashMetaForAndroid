package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design

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
            PageTheme {
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
                    title = {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
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
                                style = MaterialTheme.typography.bodySmall,
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
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    @Composable
    private fun CategoryItem(title: String) {
        Text(
            text = title,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LinkItem(entry: Entry.Link) {
        Card(onClick = { requests.trySend(Request.OpenUrl(entry.url)) }) {
            ListItem(
                headlineContent = {
                    Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(entry.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}
