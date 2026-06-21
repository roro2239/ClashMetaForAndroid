package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.util.elapsedIntervalString

class ProvidersComposeDesign(
    context: Context,
    providers: List<Provider>,
) : Design<ProvidersComposeDesign.Request>(context) {
    sealed class Request {
        data class Update(val index: Int, val provider: Provider) : Request()
    }

    private data class ProviderItemState(
        val provider: Provider,
        val updatedAt: Long,
        val updating: Boolean,
    )

    private var providers by mutableStateOf(
        providers.map { ProviderItemState(it, it.updatedAt, false) }
    )
    private var now by mutableLongStateOf(System.currentTimeMillis())

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            PageTheme {
                PageContent()
            }
        }
    }

    fun updateElapsed() {
        now = System.currentTimeMillis()
    }

    fun notifyUpdated(index: Int) {
        providers = providers.mapIndexed { i, state ->
            if (i == index) state.copy(updating = false) else state
        }
    }

    fun notifyChanged(index: Int) {
        providers = providers.mapIndexed { i, state ->
            if (i == index) {
                state.copy(updating = false, updatedAt = System.currentTimeMillis())
            } else {
                state
            }
        }
    }

    fun requestUpdateAll() {
        providers = providers.mapIndexed { index, state ->
            if (!state.updating && state.provider.vehicleType != Provider.VehicleType.Inline) {
                requests.trySend(Request.Update(index, state.provider))
                state.copy(updating = true)
            } else {
                state
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
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.providers)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { requestUpdateAll() }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.update),
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
                itemsIndexed(providers, key = { _, item -> "${item.provider.type}:${item.provider.name}" }) { index, state ->
                    ProviderItem(index, state)
                }
            }
        }
    }

    @Composable
    private fun ProviderItem(index: Int, state: ProviderItemState) {
        Card {
            ListItem(
                headlineContent = {
                    Text(
                        text = state.provider.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = providerSummary(state),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    if (state.provider.vehicleType != Provider.VehicleType.Inline) {
                        Button(
                            enabled = !state.updating,
                            onClick = {
                                providers = providers.mapIndexed { i, item ->
                                    if (i == index) item.copy(updating = true) else item
                                }
                                requests.trySend(Request.Update(index, state.provider))
                            },
                        ) {
                            Text(
                                if (state.updating) {
                                    context.getString(com.github.kr328.clash.design.R.string.loading)
                                } else {
                                    context.getString(com.github.kr328.clash.design.R.string.update)
                                }
                            )
                        }
                    }
                },
            )
        }
    }

    private fun providerSummary(state: ProviderItemState): String {
        val type = context.getString(
            com.github.kr328.clash.design.R.string.format_provider_type,
            state.provider.type.name,
            state.provider.vehicleType.name,
        )

        if (state.provider.vehicleType == Provider.VehicleType.Inline) return type

        return "$type · ${(now - state.updatedAt).elapsedIntervalString(context)}"
    }
}
