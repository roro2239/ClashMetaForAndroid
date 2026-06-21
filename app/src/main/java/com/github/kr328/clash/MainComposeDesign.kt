package com.github.kr328.clash

import android.content.Context
import android.view.View
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.service.model.Profile
import kotlinx.coroutines.launch

class MainComposeDesign(context: Context) : Design<MainComposeDesign.Request>(context) {
    sealed class Request {
        object ToggleStatus : Request()
        object OpenProviders : Request()
        object OpenLogs : Request()
        object OpenHelp : Request()
        object OpenAbout : Request()
        object CreateProfile : Request()
        object UpdateAllProfiles : Request()
        object StartAppSettings : Request()
        object StartNetworkSettings : Request()
        object StartOverrideSettings : Request()
        object StartMetaFeatureSettings : Request()
        data class SelectProxy(val groupIndex: Int, val name: String) : Request()
        data class UrlTest(val groupIndex: Int) : Request()
        data class PatchMode(val mode: TunnelState.Mode?) : Request()
        data class UpdateProfile(val profile: Profile) : Request()
        data class ActiveProfile(val profile: Profile) : Request()
        data class EditProfile(val profile: Profile) : Request()
        data class DuplicateProfile(val profile: Profile) : Request()
        data class DeleteProfile(val profile: Profile) : Request()
    }

    data class HomeState(
        val clashRunning: Boolean = false,
        val forwarded: String = "",
        val mode: String = "",
        val profileName: String? = null,
        val hasProviders: Boolean = false,
    )

    data class ProxyGroupState(
        val name: String,
        val proxies: List<Proxy> = emptyList(),
        val now: String = "?",
        val selectable: Boolean = false,
        val urlTesting: Boolean = false,
    )

    private enum class Destination(val icon: ImageVector) {
        Home(Icons.Default.Home),
        Proxy(Icons.Default.SwapHoriz),
        Profiles(Icons.AutoMirrored.Filled.List),
        Settings(Icons.Default.Settings);
    }

    var selectedPage by mutableIntStateOf(0)
        private set
    var homeState by mutableStateOf(HomeState())
        private set
    var overrideMode by mutableStateOf<TunnelState.Mode?>(null)
        private set
    var proxyGroups by mutableStateOf<List<ProxyGroupState>>(emptyList())
        private set
    var profiles by mutableStateOf<List<Profile>>(emptyList())
        private set
    var allProfilesUpdating by mutableStateOf(false)
        private set
    private var aboutVersionName by mutableStateOf<String?>(null)

    override val root: View = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            MainMaterialTheme {
                MainContent()
            }
        }
    }

    fun selectPage(page: Int) {
        selectedPage = page.coerceIn(0, Destination.entries.lastIndex)
    }

    fun returnHome(): Boolean {
        if (selectedPage == 0) return false

        selectedPage = 0
        return true
    }

    fun patchHome(state: HomeState) {
        homeState = state
    }

    fun patchOverrideMode(mode: TunnelState.Mode?) {
        overrideMode = mode
    }

    fun patchProxyGroups(names: List<String>) {
        proxyGroups = names.map { name -> ProxyGroupState(name = name) }
    }

    fun patchProxyGroup(index: Int, proxies: List<Proxy>, selectable: Boolean, now: String) {
        proxyGroups = proxyGroups.mapIndexed { i, group ->
            if (i == index) {
                group.copy(
                    proxies = proxies,
                    selectable = selectable,
                    now = now,
                    urlTesting = false,
                )
            } else {
                group
            }
        }
    }

    fun patchProxySelection(index: Int, name: String) {
        proxyGroups = proxyGroups.mapIndexed { i, group ->
            if (i == index) group.copy(now = name) else group
        }
    }

    fun patchProxyTesting(index: Int, testing: Boolean) {
        proxyGroups = proxyGroups.mapIndexed { i, group ->
            if (i == index) group.copy(urlTesting = testing) else group
        }
    }

    fun patchProfiles(value: List<Profile>) {
        profiles = value
    }

    fun patchAllProfilesUpdating(value: Boolean) {
        allProfilesUpdating = value
    }

    fun showAbout(versionName: String) {
        aboutVersionName = versionName
    }

    private fun send(request: Request) {
        requests.trySend(request)
    }

    @Composable
    private fun MainMaterialTheme(content: @Composable () -> Unit) {
        val dark = androidx.compose.foundation.isSystemInDarkTheme()
        val colors = if (dark) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

        MaterialTheme(colorScheme = colors, content = content)
    }

    @Composable
    private fun MainContent() {
        val pagerState = rememberPagerState(
            initialPage = selectedPage,
            pageCount = { Destination.entries.size },
        )

        LaunchedEffect(selectedPage) {
            pagerState.scrollToPage(selectedPage)
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedPage == index,
                            onClick = {
                                selectedPage = index
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label(),
                                )
                            },
                            label = { Text(destination.label()) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                userScrollEnabled = false,
            ) { page ->
                when (Destination.entries[page]) {
                    Destination.Home -> HomePage(innerPadding)
                    Destination.Proxy -> ProxyPage(innerPadding)
                    Destination.Profiles -> ProfilesPage(innerPadding)
                    Destination.Settings -> SettingsPage(innerPadding)
                }
            }
        }

        val about = aboutVersionName
        if (about != null) {
            AlertDialog(
                onDismissRequest = { aboutVersionName = null },
                confirmButton = {
                    TextButton(onClick = { aboutVersionName = null }) {
                        Text(context.getString(android.R.string.ok))
                    }
                },
                title = {
                    Text(context.getString(com.github.kr328.clash.design.R.string.about))
                },
                text = {
                    Text(about)
                },
            )
        }
    }

    @Composable
    private fun HomePage(innerPadding: PaddingValues) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = innerPadding.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = context.getString(com.github.kr328.clash.design.R.string.application_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (homeState.clashRunning) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = if (homeState.clashRunning) {
                                context.getString(com.github.kr328.clash.design.R.string.running)
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.stopped)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (homeState.clashRunning) {
                                context.getString(
                                    com.github.kr328.clash.design.R.string.format_traffic_forwarded,
                                    homeState.forwarded,
                                )
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.tap_to_start)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = { send(Request.ToggleStatus) }) {
                            Text(
                                if (homeState.clashRunning) {
                                    context.getString(com.github.kr328.clash.design.R.string.stop)
                                } else {
                                    context.getString(com.github.kr328.clash.design.R.string.start)
                                }
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(
                    title = context.getString(com.github.kr328.clash.design.R.string.profile),
                    summary = homeState.profileName
                        ?: context.getString(com.github.kr328.clash.design.R.string.not_selected),
                    onClick = { selectPage(Destination.Profiles.ordinal) },
                )
            }

            item {
                InfoCard(
                    title = context.getString(com.github.kr328.clash.design.R.string.proxy),
                    summary = homeState.mode,
                    onClick = { selectPage(Destination.Proxy.ordinal) },
                )
            }

            if (homeState.clashRunning && homeState.hasProviders) {
                item {
                    ActionItem(
                        title = context.getString(com.github.kr328.clash.design.R.string.providers),
                        onClick = { send(Request.OpenProviders) },
                    )
                }
            }

        }
    }

    @Composable
    private fun ProxyPage(innerPadding: PaddingValues) {
        if (proxyGroups.isEmpty()) {
            EmptyPage(
                innerPadding = innerPadding,
                title = context.getString(com.github.kr328.clash.design.R.string.proxy),
                summary = context.getString(com.github.kr328.clash.design.R.string.proxy_empty_tips),
            )
            return
        }

        val pagerState = rememberPagerState(pageCount = { proxyGroups.size })
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            PageHeader(context.getString(com.github.kr328.clash.design.R.string.proxy))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeChip(TunnelState.Mode.Rule)
                ModeChip(TunnelState.Mode.Global)
                ModeChip(TunnelState.Mode.Direct)
            }

            ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                proxyGroups.forEachIndexed { index, group ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = group.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                ProxyGroupPage(
                    index = index,
                    group = proxyGroups[index],
                    bottomPadding = innerPadding.calculateBottomPadding(),
                )
            }
        }
    }

    @Composable
    private fun ModeChip(mode: TunnelState.Mode) {
        val label = when (mode) {
            TunnelState.Mode.Direct -> context.getString(com.github.kr328.clash.design.R.string.direct_mode)
            TunnelState.Mode.Global -> context.getString(com.github.kr328.clash.design.R.string.global_mode)
            TunnelState.Mode.Rule -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
            else -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
        }

        FilterChip(
            selected = overrideMode == mode,
            onClick = { send(Request.PatchMode(mode)) },
            label = { Text(label) },
        )
    }

    @Composable
    private fun ProxyGroupPage(index: Int, group: ProxyGroupState, bottomPadding: androidx.compose.ui.unit.Dp) {
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
                        onClick = { send(Request.UrlTest(index)) },
                        label = {
                            Text(
                                if (group.urlTesting) {
                                    context.getString(com.github.kr328.clash.design.R.string.loading)
                                } else {
                                    context.getString(com.github.kr328.clash.design.R.string.delay_test)
                                }
                            )
                        },
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = group.now,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            items(group.proxies, key = { it.name }) { proxy ->
                ProxyItem(
                    proxy = proxy,
                    selected = proxy.name == group.now,
                    selectable = group.selectable,
                    onClick = { send(Request.SelectProxy(index, proxy.name)) },
                )
            }
        }
    }

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
                }
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
    private fun ProfilesPage(innerPadding: PaddingValues) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = innerPadding.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PageHeader(context.getString(com.github.kr328.clash.design.R.string.profile))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.CreateProfile) },
                    ) {
                        Text(context.getString(com.github.kr328.clash.design.R.string._new))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !allProfilesUpdating,
                        onClick = {
                            allProfilesUpdating = true
                            send(Request.UpdateAllProfiles)
                        },
                    ) {
                        Text(context.getString(com.github.kr328.clash.design.R.string.update))
                    }
                }
            }

            items(profiles, key = { it.uuid }) { profile ->
                ProfileItem(profile)
            }
        }
    }

    @Composable
    private fun ProfileItem(profile: Profile) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(profile.name, fontWeight = FontWeight.SemiBold)
                Text(profile.type.name, style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.ActiveProfile(profile)) },
                    ) {
                        Text(
                            if (profile.active) {
                                context.getString(com.github.kr328.clash.design.R.string.active)
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.activate)
                            }
                        )
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.EditProfile(profile)) },
                    ) {
                        Text(context.getString(com.github.kr328.clash.design.R.string.edit))
                    }
                    if (profile.imported && profile.type != Profile.Type.File) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { send(Request.UpdateProfile(profile)) },
                        ) {
                            Text(context.getString(com.github.kr328.clash.design.R.string.update))
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.DuplicateProfile(profile)) },
                    ) {
                        Text(context.getString(com.github.kr328.clash.design.R.string.duplicate))
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.DeleteProfile(profile)) },
                    ) {
                        Text(context.getString(com.github.kr328.clash.design.R.string.delete))
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsPage(innerPadding: PaddingValues) {
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
                PageHeader(context.getString(com.github.kr328.clash.design.R.string.settings))
            }
            item {
                SettingsItem(context.getString(com.github.kr328.clash.design.R.string.app)) {
                    send(Request.StartAppSettings)
                }
            }
            item {
                SettingsItem(context.getString(com.github.kr328.clash.design.R.string.network)) {
                    send(Request.StartNetworkSettings)
                }
            }
            item {
                SettingsItem(context.getString(com.github.kr328.clash.design.R.string.override)) {
                    send(Request.StartOverrideSettings)
                }
            }
            item {
                SettingsItem(context.getString(com.github.kr328.clash.design.R.string.meta_features)) {
                    send(Request.StartMetaFeatureSettings)
                }
            }
            item {
                SettingsItem(context.getString(com.github.kr328.clash.design.R.string.logs)) {
                    send(Request.OpenLogs)
                }
            }
            item {
                SettingsItem(context.getString(com.github.kr328.clash.design.R.string.help)) {
                    send(Request.OpenHelp)
                }
            }
        }
    }

    @Composable
    private fun PageHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun InfoCard(title: String, summary: String, onClick: () -> Unit) {
        Card(onClick = onClick) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    private fun ActionItem(title: String, onClick: () -> Unit) {
        SettingsItem(title = title, onClick = onClick)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsItem(title: String, onClick: () -> Unit) {
        Surface(onClick = onClick) {
            ListItem(
                headlineContent = { Text(title) },
                leadingContent = { Icon(Icons.Default.Dns, contentDescription = null) },
            )
        }
        HorizontalDivider()
    }

    @Composable
    private fun EmptyPage(innerPadding: PaddingValues, title: String, summary: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    top = innerPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PageHeader(title)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }

    private fun Destination.label(): String {
        return when (this) {
            Destination.Home -> context.getString(com.github.kr328.clash.design.R.string.home)
            Destination.Proxy -> context.getString(com.github.kr328.clash.design.R.string.proxy)
            Destination.Profiles -> context.getString(com.github.kr328.clash.design.R.string.profile)
            Destination.Settings -> context.getString(com.github.kr328.clash.design.R.string.settings)
        }
    }
}
