package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.ProfileProvider

class NewProfileComposeDesign(context: Context) : Design<NewProfileComposeDesign.Request>(context) {
    sealed class Request {
        data class Create(val provider: ProfileProvider) : Request()
        data class OpenDetail(val provider: ProfileProvider.External) : Request()
        data class LaunchScanner(val provider: ProfileProvider.QR) : Request()
    }

    private var providers by mutableStateOf<List<ProfileProvider>>(emptyList())

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            PageTheme {
                PageContent()
            }
        }
    }

    fun patchProviders(providers: List<ProfileProvider>) {
        this.providers = providers
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
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.new_profile)) },
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
                items(providers, key = { provider -> provider.name }) { provider ->
                    ProviderItem(provider)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ProviderItem(provider: ProfileProvider) {
        Card(onClick = { requestCreate(provider) }) {
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = when (provider) {
                            is ProfileProvider.File -> Icons.Default.FolderOpen
                            is ProfileProvider.QR -> Icons.Default.QrCodeScanner
                            else -> Icons.Default.Public
                        },
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(provider.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(provider.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = {
                    if (provider is ProfileProvider.External) {
                        IconButton(onClick = { requests.trySend(Request.OpenDetail(provider)) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.properties),
                            )
                        }
                    }
                },
            )
        }
    }

    private fun requestCreate(provider: ProfileProvider) {
        if (provider is ProfileProvider.QR) {
            requests.trySend(Request.LaunchScanner(provider))
        } else {
            requests.trySend(Request.Create(provider))
        }
    }
}
