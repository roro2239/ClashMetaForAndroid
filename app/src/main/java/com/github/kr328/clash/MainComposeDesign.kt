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
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
        Home(MiuixIcons.VerticalSplit),
        Proxy(MiuixIcons.More),
        Profiles(MiuixIcons.Contacts),
        Settings(MiuixIcons.Settings);
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
            ClashMiuixTheme {
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
                NavigationBar(mode = NavigationBarDisplayMode.IconWithSelectedLabel) {
                    Destination.entries.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedPage == index,
                            onClick = {
                                selectedPage = index
                            },
                            icon = destination.icon,
                            label = destination.label(),
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
            ClashMiuixDialog(
                title = context.getString(com.github.kr328.clash.design.R.string.about),
                message = about,
                confirmText = context.getString(android.R.string.ok),
                onConfirm = { aboutVersionName = null },
                onDismissRequest = { aboutVersionName = null },
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
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                Card(
                    colors = CardDefaults.defaultColors(
                        color = if (homeState.clashRunning) {
                            MiuixTheme.colorScheme.primaryContainer
                        } else {
                            MiuixTheme.colorScheme.surfaceContainer
                        }
                    ),
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
                            style = MiuixTheme.textStyles.title2,
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
                            style = MiuixTheme.textStyles.body1,
                        )
                        Button(onClick = { send(Request.ToggleStatus) }) {
                            Text(
                                text = if (homeState.clashRunning) {
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
                ModeButton(TunnelState.Mode.Rule)
                ModeButton(TunnelState.Mode.Global)
                ModeButton(TunnelState.Mode.Direct)
            }

            TabRow(
                tabs = proxyGroups.map { it.name },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )

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
    private fun ModeButton(mode: TunnelState.Mode) {
        val label = when (mode) {
            TunnelState.Mode.Direct -> context.getString(com.github.kr328.clash.design.R.string.direct_mode)
            TunnelState.Mode.Global -> context.getString(com.github.kr328.clash.design.R.string.global_mode)
            TunnelState.Mode.Rule -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
            else -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
        }

        Button(
            onClick = { send(Request.PatchMode(mode)) },
            colors = ButtonDefaults.buttonColors(
                color = if (overrideMode == mode) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
                contentColor = if (overrideMode == mode) {
                    MiuixTheme.colorScheme.onPrimary
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text(text = label)
        }
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
                    Button(
                        onClick = { send(Request.UrlTest(index)) },
                    ) {
                            Text(
                                text = if (group.urlTesting) {
                                    context.getString(com.github.kr328.clash.design.R.string.loading)
                                } else {
                                    context.getString(com.github.kr328.clash.design.R.string.delay_test)
                                }
                            )
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainer,
                            contentColor = MiuixTheme.colorScheme.onSurface,
                        ),
                    ) {
                            Text(
                                text = group.now,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                    }
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
            colors = CardDefaults.defaultColors(
                color = if (selected) {
                    MiuixTheme.colorScheme.secondaryContainer
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
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
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${proxy.type} · ${proxy.delay} ms",
                    style = MiuixTheme.textStyles.footnote1,
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
                        Text(text = context.getString(com.github.kr328.clash.design.R.string._new))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !allProfilesUpdating,
                        onClick = {
                            allProfilesUpdating = true
                            send(Request.UpdateAllProfiles)
                        },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.update))
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
                Text(text = profile.name, fontWeight = FontWeight.SemiBold)
                Text(text = profile.type.name, style = MiuixTheme.textStyles.body2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.ActiveProfile(profile)) },
                    ) {
                        Text(
                            text = if (profile.active) {
                                context.getString(com.github.kr328.clash.design.R.string.active)
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.activate)
                            }
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.EditProfile(profile)) },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.edit))
                    }
                    if (profile.imported && profile.type != Profile.Type.File) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { send(Request.UpdateProfile(profile)) },
                        ) {
                            Text(text = context.getString(com.github.kr328.clash.design.R.string.update))
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.DuplicateProfile(profile)) },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.duplicate))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { send(Request.DeleteProfile(profile)) },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.delete))
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
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    @Composable
    private fun InfoCard(title: String, summary: String, onClick: () -> Unit) {
        Card(onClick = onClick) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Text(text = summary, style = MiuixTheme.textStyles.body1)
            }
        }
    }

    @Composable
    private fun ActionItem(title: String, onClick: () -> Unit) {
        SettingsItem(title = title, onClick = onClick)
    }

    @Composable
    private fun SettingsItem(title: String, onClick: () -> Unit) {
        Surface(onClick = onClick) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MiuixTheme.textStyles.body1)
            }
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
            Text(text = summary, style = MiuixTheme.textStyles.body1)
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
