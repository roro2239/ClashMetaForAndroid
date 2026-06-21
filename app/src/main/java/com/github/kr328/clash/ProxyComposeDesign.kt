package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.ProxyState
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixMenuItem
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme

class ProxyComposeDesign(
    context: Context,
    private var overrideMode: TunnelState.Mode?,
    private val groupNames: List<String>,
    private val uiStore: UiStore,
) : Design<ProxyComposeDesign.Request>(context) {
    sealed class Request {
        object ReloadAll : Request()
        object ReLaunch : Request()

        data class PatchMode(val mode: TunnelState.Mode?) : Request()
        data class Reload(val index: Int) : Request()
        data class Select(val index: Int, val name: String) : Request()
        data class UrlTest(val index: Int) : Request()
    }

    private data class GroupState(
        val name: String,
        val now: String,
        val proxies: List<Proxy>,
        val selectable: Boolean,
        val urlTesting: Boolean,
    )

    private var groups by mutableStateOf(
        groupNames.map { GroupState(it, "?", emptyList(), false, false) },
    )
    private var menuExpanded by mutableStateOf(false)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    suspend fun updateGroup(
        position: Int,
        proxies: List<Proxy>,
        selectable: Boolean,
        parent: ProxyState,
        links: Map<String, ProxyState>,
    ) {
        withContext(Dispatchers.Main) {
            groups = groups.mapIndexed { index, group ->
                if (index == position) {
                    group.copy(
                        now = parent.now,
                        proxies = proxies,
                        selectable = selectable,
                        urlTesting = false,
                    )
                } else {
                    group
                }
            }
        }
    }

    suspend fun requestRedrawVisible() {
        withContext(Dispatchers.Main) {
            groups = groups.toList()
        }
    }

    suspend fun showModeSwitchTips() {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, com.github.kr328.clash.design.R.string.mode_switch_tips, Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.proxy),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
            actions = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = MiuixIcons.More,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.properties),
                    )
                }
                ProxyMenu()
            },
        ) { innerPadding, nestedScrollConnection ->
            if (groups.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 12.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp,
                    ),
                ) {
                    item {
                        Text(context.getString(com.github.kr328.clash.design.R.string.proxy_empty_tips))
                    }
                }
            } else {
                ProxyContent(innerPadding, nestedScrollConnection)
            }
        }
    }

    @Composable
    private fun ProxyContent(innerPadding: PaddingValues, nestedScrollConnection: NestedScrollConnection) {
        val pagerState = rememberPagerState(pageCount = { groups.size })
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeChip(null)
                ModeChip(TunnelState.Mode.Rule)
                ModeChip(TunnelState.Mode.Global)
                ModeChip(TunnelState.Mode.Direct)
            }
            TabRow(
                tabs = groups.map { it.name },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    val group = groups[index]
                    uiStore.proxyLastGroup = group.name
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                ProxyGroupPage(
                    index = index,
                    group = groups[index],
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    nestedScrollConnection = nestedScrollConnection,
                )
            }
        }
    }

    @Composable
    private fun ModeChip(mode: TunnelState.Mode?) {
        val label = when (mode) {
            null -> context.getString(com.github.kr328.clash.design.R.string.dont_modify)
            TunnelState.Mode.Direct -> context.getString(com.github.kr328.clash.design.R.string.direct_mode)
            TunnelState.Mode.Global -> context.getString(com.github.kr328.clash.design.R.string.global_mode)
            TunnelState.Mode.Rule -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
            else -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
        }

        val onClick = {
            overrideMode = mode
            requests.trySend(Request.PatchMode(mode))
            Unit
        }

        if (overrideMode == mode) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            TextButton(
                text = label,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    private fun ProxyGroupPage(
        index: Int,
        group: GroupState,
        bottomPadding: androidx.compose.ui.unit.Dp,
        nestedScrollConnection: NestedScrollConnection,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = bottomPadding + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            groups = groups.mapIndexed { i, item ->
                                if (i == index) item.copy(urlTesting = true) else item
                            }
                            requests.trySend(Request.UrlTest(index))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (group.urlTesting) {
                                context.getString(com.github.kr328.clash.design.R.string.loading)
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.delay_test)
                            },
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = group.now,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            items(group.proxies, key = { it.name }) { proxy ->
                ProxyItem(
                    proxy = proxy,
                    selected = proxy.name == group.now,
                    selectable = group.selectable,
                    onClick = {
                        requests.trySend(Request.Select(index, proxy.name))
                        groups = groups.mapIndexed { i, item ->
                            if (i == index) item.copy(now = proxy.name) else item
                        }
                    },
                )
            }
        }
    }

    @Composable
    private fun ProxyItem(proxy: Proxy, selected: Boolean, selectable: Boolean, onClick: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (selectable) onClick else null,
            colors = CardDefaults.defaultColors(
                color = if (selected) {
                    MiuixTheme.colorScheme.secondaryContainer
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = proxy.title.ifEmpty { proxy.name },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = proxy.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${proxy.type} · ${proxy.delay} ms",
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    private fun ProxyMenu() {
        if (!menuExpanded) return

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.properties),
            onDismissRequest = { menuExpanded = false },
        ) {
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.not_selectable),
                trailingContent = {
                    Checkbox(
                        state = if (uiStore.proxyExcludeNotSelectable) ToggleableState.On else ToggleableState.Off,
                        onClick = null,
                    )
                },
                onClick = {
                    uiStore.proxyExcludeNotSelectable = !uiStore.proxyExcludeNotSelectable
                    sendMenuRequest(Request.ReLaunch)
                },
            )
            ProxyLineItem(1, com.github.kr328.clash.design.R.string.single)
            ProxyLineItem(2, com.github.kr328.clash.design.R.string.doubles)
            ProxyLineItem(3, com.github.kr328.clash.design.R.string.multiple)
            ProxySortItem(ProxySort.Default, com.github.kr328.clash.design.R.string.default_)
            ProxySortItem(ProxySort.Title, com.github.kr328.clash.design.R.string.name)
            ProxySortItem(ProxySort.Delay, com.github.kr328.clash.design.R.string.delay)
        }
    }

    @Composable
    private fun ProxyLineItem(line: Int, title: Int) {
        ClashMiuixMenuItem(
            title = context.getString(title),
            trailingContent = {
                Checkbox(
                    state = if (uiStore.proxyLine == line) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                )
            },
            onClick = {
                uiStore.proxyLine = line
                sendMenuRequest(Request.ReloadAll)
            },
        )
    }

    @Composable
    private fun ProxySortItem(sort: ProxySort, title: Int) {
        ClashMiuixMenuItem(
            title = context.getString(title),
            trailingContent = {
                Checkbox(
                    state = if (uiStore.proxySort == sort) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                )
            },
            onClick = {
                uiStore.proxySort = sort
                sendMenuRequest(Request.ReloadAll)
            },
        )
    }

    private fun sendMenuRequest(request: Request) {
        menuExpanded = false
        requests.trySend(request)
    }
}
