package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
            ClashMiuixTheme {
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
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.providers),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
            actions = {
                IconButton(onClick = { requestUpdateAll() }) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.update),
                    )
                }
            },
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
                itemsIndexed(providers, key = { _, item -> "${item.provider.type}:${item.provider.name}" }) { index, state ->
                    ProviderItem(index, state)
                }
            }
        }
    }

    @Composable
    private fun ProviderItem(index: Int, state: ProviderItemState) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.provider.name,
                        style = MiuixTheme.textStyles.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = providerSummary(state),
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
                            text = if (state.updating) {
                                context.getString(com.github.kr328.clash.design.R.string.loading)
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.update)
                            }
                        )
                    }
                }
            }
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
