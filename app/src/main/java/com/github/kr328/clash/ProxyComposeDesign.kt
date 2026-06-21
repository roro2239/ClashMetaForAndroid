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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.ProxyState
import com.github.kr328.clash.design.store.UiStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            PageTheme {
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
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.proxy)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = context.getString(com.github.kr328.clash.design.R.string.properties))
                        }
                        ProxyMenu()
                    },
                )
            },
        ) { innerPadding ->
            if (groups.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 20.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp,
                    ),
                ) {
                    item {
                        Text(context.getString(com.github.kr328.clash.design.R.string.proxy_empty_tips))
                    }
                }
            } else {
                ProxyContent(innerPadding)
            }
        }
    }

    @Composable
    private fun ProxyContent(innerPadding: PaddingValues) {
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
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                groups.forEachIndexed { index, group ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            uiStore.proxyLastGroup = group.name
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                ProxyGroupPage(index, groups[index], innerPadding.calculateBottomPadding())
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

        FilterChip(
            selected = overrideMode == mode,
            onClick = {
                overrideMode = mode
                requests.trySend(Request.PatchMode(mode))
            },
            label = { Text(label) },
        )
    }

    @Composable
    private fun ProxyGroupPage(index: Int, group: GroupState, bottomPadding: androidx.compose.ui.unit.Dp) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    AssistChip(
                        onClick = {
                            groups = groups.mapIndexed { i, item ->
                                if (i == index) item.copy(urlTesting = true) else item
                            }
                            requests.trySend(Request.UrlTest(index))
                        },
                        label = {
                            Text(
                                if (group.urlTesting) {
                                    context.getString(com.github.kr328.clash.design.R.string.loading)
                                } else {
                                    context.getString(com.github.kr328.clash.design.R.string.delay_test)
                                },
                            )
                        },
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(group.now, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ProxyItem(proxy: Proxy, selected: Boolean, selectable: Boolean, onClick: () -> Unit) {
        Card(
            onClick = onClick,
            enabled = selectable,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
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
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${proxy.type} · ${proxy.delay} ms",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    private fun ProxyMenu() {
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.not_selectable)) },
                trailingIcon = { Checkbox(checked = uiStore.proxyExcludeNotSelectable, onCheckedChange = null) },
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
        DropdownMenuItem(
            text = { Text(context.getString(title)) },
            trailingIcon = { Checkbox(checked = uiStore.proxyLine == line, onCheckedChange = null) },
            onClick = {
                uiStore.proxyLine = line
                sendMenuRequest(Request.ReloadAll)
            },
        )
    }

    @Composable
    private fun ProxySortItem(sort: ProxySort, title: Int) {
        DropdownMenuItem(
            text = { Text(context.getString(title)) },
            trailingIcon = { Checkbox(checked = uiStore.proxySort == sort, onCheckedChange = null) },
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
