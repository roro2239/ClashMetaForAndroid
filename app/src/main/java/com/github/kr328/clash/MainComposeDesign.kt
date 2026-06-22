package com.github.kr328.clash

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import android.content.Context
import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toString
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixMenuItem
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import java.util.concurrent.TimeUnit

private const val regionalIndicatorBase = 0x1F1E6
private const val regionalIndicatorEnd = 0x1F1FF

private val countryNameToCode = mapOf(
    "中国" to "CN",
    "香港" to "HK",
    "澳门" to "MO",
    "台湾" to "TW",
    "美国" to "US",
    "英国" to "GB",
    "法国" to "FR",
    "德国" to "DE",
    "日本" to "JP",
    "韩国" to "KR",
    "新加坡" to "SG",
    "澳大利亚" to "AU",
    "澳洲" to "AU",
    "加拿大" to "CA",
    "俄罗斯" to "RU",
    "印度" to "IN",
    "印尼" to "ID",
    "印度尼西亚" to "ID",
    "马来西亚" to "MY",
    "泰国" to "TH",
    "越南" to "VN",
    "菲律宾" to "PH",
    "柬埔寨" to "KH",
    "老挝" to "LA",
    "缅甸" to "MM",
    "孟加拉" to "BD",
    "巴基斯坦" to "PK",
    "斯里兰卡" to "LK",
    "尼泊尔" to "NP",
    "不丹" to "BT",
    "马尔代夫" to "MV",
    "哈萨克斯坦" to "KZ",
    "乌兹别克斯坦" to "UZ",
    "土库曼斯坦" to "TM",
    "塔吉克斯坦" to "TJ",
    "吉尔吉斯斯坦" to "KG",
    "伊朗" to "IR",
    "伊拉克" to "IQ",
    "沙特" to "SA",
    "阿联酋" to "AE",
    "卡塔尔" to "QA",
    "科威特" to "KW",
    "巴林" to "BH",
    "阿曼" to "OM",
    "也门" to "YE",
    "约旦" to "JO",
    "黎巴嫩" to "LB",
    "以色列" to "IL",
    "巴勒斯坦" to "PS",
    "土耳其" to "TR",
    "格鲁吉亚" to "GE",
    "阿塞拜疆" to "AZ",
    "亚美尼亚" to "AM",
    "乌克兰" to "UA",
    "白俄罗斯" to "BY",
    "波兰" to "PL",
    "捷克" to "CZ",
    "斯洛伐克" to "SK",
    "匈牙利" to "HU",
    "罗马尼亚" to "RO",
    "保加利亚" to "BG",
    "塞尔维亚" to "RS",
    "克罗地亚" to "HR",
    "斯洛文尼亚" to "SI",
    "波黑" to "BA",
    "黑山" to "ME",
    "北马其顿" to "MK",
    "阿尔巴尼亚" to "AL",
    "希腊" to "GR",
    "意大利" to "IT",
    "西班牙" to "ES",
    "葡萄牙" to "PT",
    "荷兰" to "NL",
    "比利时" to "BE",
    "卢森堡" to "LU",
    "瑞士" to "CH",
    "奥地利" to "AT",
    "瑞典" to "SE",
    "挪威" to "NO",
    "丹麦" to "DK",
    "芬兰" to "FI",
    "冰岛" to "IS",
    "爱尔兰" to "IE",
    "爱沙尼亚" to "EE",
    "拉脱维亚" to "LV",
    "立陶宛" to "LT",
    "墨西哥" to "MX",
    "巴西" to "BR",
    "阿根廷" to "AR",
    "智利" to "CL",
    "哥伦比亚" to "CO",
    "秘鲁" to "PE",
    "委内瑞拉" to "VE",
    "厄瓜多尔" to "EC",
    "乌拉圭" to "UY",
    "巴拉圭" to "PY",
    "玻利维亚" to "BO",
    "哥斯达黎加" to "CR",
    "巴拿马" to "PA",
    "古巴" to "CU",
    "牙买加" to "JM",
    "多米尼加" to "DO",
    "埃及" to "EG",
    "南非" to "ZA",
    "尼日利亚" to "NG",
    "肯尼亚" to "KE",
    "摩洛哥" to "MA",
    "突尼斯" to "TN",
    "阿尔及利亚" to "DZ",
    "利比亚" to "LY",
    "埃塞俄比亚" to "ET",
    "加纳" to "GH",
    "坦桑尼亚" to "TZ",
    "乌干达" to "UG",
    "卢旺达" to "RW",
    "津巴布韦" to "ZW",
    "博茨瓦纳" to "BW",
    "纳米比亚" to "NA",
    "新西兰" to "NZ",
    "斐济" to "FJ",
    "巴布亚新几内亚" to "PG",
    "朝鲜" to "KP",
    "蒙古" to "MN",
    "文莱" to "BN",
    "东帝汶" to "TL",
    "China" to "CN",
    "Hong Kong" to "HK",
    "Macau" to "MO",
    "Taiwan" to "TW",
    "United States" to "US",
    "USA" to "US",
    "America" to "US",
    "United Kingdom" to "GB",
    "UK" to "GB",
    "England" to "GB",
    "Britain" to "GB",
    "France" to "FR",
    "Germany" to "DE",
    "Deutschland" to "DE",
    "Japan" to "JP",
    "Korea" to "KR",
    "South Korea" to "KR",
    "Singapore" to "SG",
    "Australia" to "AU",
    "Canada" to "CA",
    "Russia" to "RU",
    "India" to "IN",
    "Indonesia" to "ID",
    "Malaysia" to "MY",
    "Thailand" to "TH",
    "Vietnam" to "VN",
    "Philippines" to "PH",
    "Cambodia" to "KH",
    "Laos" to "LA",
    "Myanmar" to "MM",
    "Burma" to "MM",
    "Bangladesh" to "BD",
    "Pakistan" to "PK",
    "Sri Lanka" to "LK",
    "Nepal" to "NP",
    "Bhutan" to "BT",
    "Maldives" to "MV",
    "Kazakhstan" to "KZ",
    "Uzbekistan" to "UZ",
    "Turkmenistan" to "TM",
    "Tajikistan" to "TJ",
    "Kyrgyzstan" to "KG",
    "Iran" to "IR",
    "Iraq" to "IQ",
    "Saudi Arabia" to "SA",
    "UAE" to "AE",
    "Qatar" to "QA",
    "Kuwait" to "KW",
    "Bahrain" to "BH",
    "Oman" to "OM",
    "Yemen" to "YE",
    "Jordan" to "JO",
    "Lebanon" to "LB",
    "Israel" to "IL",
    "Palestine" to "PS",
    "Turkey" to "TR",
    "Georgia" to "GE",
    "Azerbaijan" to "AZ",
    "Armenia" to "AM",
    "Ukraine" to "UA",
    "Belarus" to "BY",
    "Poland" to "PL",
    "Czech" to "CZ",
    "Slovakia" to "SK",
    "Hungary" to "HU",
    "Romania" to "RO",
    "Bulgaria" to "BG",
    "Serbia" to "RS",
    "Croatia" to "HR",
    "Slovenia" to "SI",
    "Bosnia" to "BA",
    "Montenegro" to "ME",
    "Macedonia" to "MK",
    "Albania" to "AL",
    "Greece" to "GR",
    "Italy" to "IT",
    "Spain" to "ES",
    "Portugal" to "PT",
    "Netherlands" to "NL",
    "Belgium" to "BE",
    "Luxembourg" to "LU",
    "Switzerland" to "CH",
    "Austria" to "AT",
    "Sweden" to "SE",
    "Norway" to "NO",
    "Denmark" to "DK",
    "Finland" to "FI",
    "Iceland" to "IS",
    "Ireland" to "IE",
    "Estonia" to "EE",
    "Latvia" to "LV",
    "Lithuania" to "LT",
    "Mexico" to "MX",
    "Brazil" to "BR",
    "Argentina" to "AR",
    "Chile" to "CL",
    "Colombia" to "CO",
    "Peru" to "PE",
    "Venezuela" to "VE",
    "Ecuador" to "EC",
    "Uruguay" to "UY",
    "Paraguay" to "PY",
    "Bolivia" to "BO",
    "Costa Rica" to "CR",
    "Panama" to "PA",
    "Cuba" to "CU",
    "Jamaica" to "JM",
    "Dominican" to "DO",
    "Egypt" to "EG",
    "South Africa" to "ZA",
    "Nigeria" to "NG",
    "Kenya" to "KE",
    "Morocco" to "MA",
    "Tunisia" to "TN",
    "Algeria" to "DZ",
    "Libya" to "LY",
    "Ethiopia" to "ET",
    "Ghana" to "GH",
    "Tanzania" to "TZ",
    "Uganda" to "UG",
    "Rwanda" to "RW",
    "Zimbabwe" to "ZW",
    "Botswana" to "BW",
    "Namibia" to "NA",
    "New Zealand" to "NZ",
    "Fiji" to "FJ",
    "Papua New Guinea" to "PG",
    "North Korea" to "KP",
    "Mongolia" to "MN",
    "Brunei" to "BN",
    "Timor" to "TL",
)

private val countryNameRegex = Regex(
    countryNameToCode.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) },
    RegexOption.IGNORE_CASE,
)

class MainComposeDesign(context: Context) : Design<MainComposeDesign.Request>(context) {
    sealed class Request {
        object ToggleStatus : Request()
        object OpenProviders : Request()
        object OpenLogs : Request()
        object OpenHelp : Request()
        object OpenAbout : Request()
        object PickProfileFile : Request()
        object LaunchProfileScanner : Request()
        data class CreateProfileUrl(val name: String, val url: String, val interval: Long) : Request()
        data class CreateProfileFile(val name: String, val uri: Uri) : Request()
        object UpdateAllProfiles : Request()
        object StartAppSettings : Request()
        object StartNetworkSettings : Request()
        object StartOverrideSettings : Request()
        object StartMetaFeatureSettings : Request()
        object ReloadAccessControlApps : Request()
        object AccessControlRuleChanged : Request()
        data class SelectProxy(val groupIndex: Int, val name: String) : Request()
        data class UrlTest(val groupIndex: Int) : Request()
        data class PatchProxySort(val sort: ProxySort) : Request()
        data class PatchMode(val mode: TunnelState.Mode?) : Request()
        data class ActiveProfile(val profile: Profile) : Request()
        data class EditProfile(val profile: Profile) : Request()
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
        Apps(MiuixIcons.Settings),
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
    private var proxyPageIndex by mutableIntStateOf(0)
    private var proxySort by mutableStateOf(ProxySort.Default)
    private var proxySortMenuExpanded by mutableStateOf(false)
    var profiles by mutableStateOf<List<Profile>>(emptyList())
        private set
    var allProfilesUpdating by mutableStateOf(false)
        private set
    private var aboutVersionName by mutableStateOf<String?>(null)
    private var createProfileVisible by mutableStateOf(false)
    private var createProfileType by mutableStateOf(Profile.Type.Url)
    private var createProfileName by mutableStateOf("")
    private var createProfileUrl by mutableStateOf("")
    private var createProfileInterval by mutableStateOf("")
    private var createProfileFileUri by mutableStateOf<Uri?>(null)
    private var createProfileFileName by mutableStateOf("")
    val accessControlDesign = AccessControlComposeDesign(
        context = context,
        uiStore = com.github.kr328.clash.design.store.UiStore(context),
        appRules = mutableMapOf(),
    )

    override val root: View = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides navigationOwner
                ) {
                    MainContent()
                }
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

    fun patchProxySort(sort: ProxySort) {
        proxySort = sort
    }

    fun patchProxyTesting(index: Int, testing: Boolean) {
        proxyGroups = proxyGroups.mapIndexed { i, group ->
            if (i == index) group.copy(urlTesting = testing) else group
        }
    }

    fun patchProfiles(value: List<Profile>) {
        profiles = value
    }

    suspend fun patchAccessControlApps(value: List<AppInfo>) {
        accessControlDesign.patchApps(value)
    }

    suspend fun rebindAccessControlApps() {
        accessControlDesign.rebindAll()
    }

    fun patchAllProfilesUpdating(value: Boolean) {
        allProfilesUpdating = value
    }

    fun patchPickedProfileFile(uri: Uri, name: String) {
        createProfileType = Profile.Type.File
        createProfileFileUri = uri
        createProfileFileName = name
        if (createProfileName.isBlank()) {
            createProfileName = name.substringBeforeLast(".").ifBlank {
                context.getString(com.github.kr328.clash.design.R.string.new_profile)
            }
        }
    }

    fun patchScannedProfileUrl(url: String) {
        createProfileVisible = true
        createProfileType = Profile.Type.Url
        createProfileUrl = url
    }

    fun showAbout(versionName: String) {
        aboutVersionName = versionName
    }

    private fun send(request: Request) {
        requests.trySend(request)
    }

    @Composable
    private fun MainContent() {
        val scope = rememberCoroutineScope()
        val scrollBehavior = MiuixScrollBehavior()
        val pagerState = rememberPagerState(
            initialPage = selectedPage,
            pageCount = { Destination.entries.size },
        )
        var targetPage by remember { mutableIntStateOf(selectedPage) }

        LaunchedEffect(selectedPage) {
            targetPage = selectedPage
            pagerState.scrollToPage(selectedPage)
        }

        fun navigateTo(index: Int) {
            val page = index.coerceIn(0, Destination.entries.lastIndex)
            if (targetPage == page) return

            scope.launch {
                targetPage = page
                pagerState.animateScrollToPage(page)
                selectedPage = page
            }
        }

        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val pagePadding = PaddingValues(
            top = 0.dp,
            bottom = navigationBarPadding + 96.dp,
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = Destination.entries[targetPage].label(),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        when (Destination.entries[targetPage]) {
                            Destination.Proxy -> ProxyTitleActions()
                            Destination.Apps -> accessControlDesign.MenuAction()
                            else -> Unit
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                val mergedPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = pagePadding.calculateBottomPadding(),
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                ) { page ->
                    when (Destination.entries[page]) {
                        Destination.Home -> HomePage(mergedPadding, scrollBehavior.nestedScrollConnection)
                        Destination.Proxy -> ProxyPage(mergedPadding, scrollBehavior.nestedScrollConnection)
                        Destination.Profiles -> ProfilesPage(mergedPadding, scrollBehavior.nestedScrollConnection)
                        Destination.Apps -> accessControlDesign.PageContent(mergedPadding, scrollBehavior.nestedScrollConnection)
                        Destination.Settings -> SettingsPage(mergedPadding, scrollBehavior.nestedScrollConnection)
                    }
                }

                FloatingNavigationBar(
                    selectedPage = targetPage,
                    bottomPadding = navigationBarPadding,
                    onSelected = ::navigateTo,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        LaunchedEffect(accessControlDesign) {
            send(Request.ReloadAccessControlApps)
            for (request in accessControlDesign.requests) {
                when (request) {
                    AccessControlComposeDesign.Request.ReloadApps -> send(Request.ReloadAccessControlApps)
                    AccessControlComposeDesign.Request.RuleChanged -> send(Request.AccessControlRuleChanged)
                }
            }
        }

        CreateProfileSheet()
        ProxySortMenu()

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
    private fun FloatingNavigationBar(
        selectedPage: Int,
        bottomPadding: Dp,
        onSelected: (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val tabWidth = 64.dp
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedPage.toFloat(),
            label = "floatingNavigationIndicatorOffset",
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding + 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(tabWidth * Destination.entries.size.toFloat() + 8.dp)
                    .height(64.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Destination.entries.forEachIndexed { index, destination ->
                        val selected = selectedPage == index
                        val itemColor by animateColorAsState(
                            targetValue = if (selected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                            label = "floatingNavigationItemColor",
                        )
                        Column(
                            modifier = Modifier
                                .width(tabWidth)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable { onSelected(index) },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label(),
                                tint = itemColor,
                            )
                            Text(
                                text = destination.label(),
                                style = MiuixTheme.textStyles.footnote1,
                                color = itemColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ProxyTitleActions() {
        val index = proxyPageIndex.coerceIn(0, (proxyGroups.size - 1).coerceAtLeast(0))
        val testing = proxyGroups.getOrNull(index)?.urlTesting == true

        IconButton(
            enabled = proxyGroups.isNotEmpty() && !testing,
            onClick = {
                send(Request.PatchProxySort(ProxySort.Delay))
                send(Request.UrlTest(index))
            },
        ) {
            Icon(
                imageVector = MiuixIcons.Refresh,
                contentDescription = context.getString(com.github.kr328.clash.design.R.string.delay_test),
            )
        }
        IconButton(onClick = { proxySortMenuExpanded = true }) {
            Icon(
                imageVector = MiuixIcons.More,
                contentDescription = "排序",
            )
        }
    }

    @Composable
    private fun ProxySortMenu() {
        if (!proxySortMenuExpanded) return

        ClashMiuixDialog(
            title = "排序",
            onDismissRequest = { proxySortMenuExpanded = false },
        ) {
            ProxySortItem(ProxySort.Default, "默认排序")
            ProxySortItem(ProxySort.Title, "名称排序")
            ProxySortItem(ProxySort.Delay, "延迟排序")
        }
    }

    @Composable
    private fun ProxySortItem(sort: ProxySort, title: String) {
        ClashMiuixMenuItem(
            title = title,
            trailingContent = {
                Checkbox(
                    state = if (proxySort == sort) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                )
            },
            onClick = {
                proxySortMenuExpanded = false
                send(Request.PatchProxySort(sort))
            },
        )
    }

    @Composable
    private fun HomePage(innerPadding: PaddingValues, nestedScrollConnection: NestedScrollConnection) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HomeStatusCard()
            }

            item {
                HomeInfoGrid()
            }

            item {
                ModeSelectorCard()
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
    private fun HomeStatusCard() {
        val running = homeState.clashRunning
        val statusText = if (running) {
            context.getString(com.github.kr328.clash.design.R.string.running)
        } else {
            context.getString(com.github.kr328.clash.design.R.string.stopped)
        }
        val summaryText = if (running) {
            context.getString(
                com.github.kr328.clash.design.R.string.format_traffic_forwarded,
                homeState.forwarded,
            )
        } else {
            context.getString(com.github.kr328.clash.design.R.string.tap_to_start)
        }
        val actionText = if (running) {
            context.getString(com.github.kr328.clash.design.R.string.stop)
        } else {
            context.getString(com.github.kr328.clash.design.R.string.start)
        }

        Card(
            colors = CardDefaults.defaultColors(
                color = if (running) {
                    MiuixTheme.colorScheme.primaryContainer
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = statusText,
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = summaryText,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = { send(Request.ToggleStatus) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = actionText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    private fun HomeInfoGrid() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HomeInfoCard(
                modifier = Modifier.weight(1f),
                title = context.getString(com.github.kr328.clash.design.R.string.profile),
                summary = homeState.profileName
                    ?: context.getString(com.github.kr328.clash.design.R.string.not_selected),
                onClick = { selectPage(Destination.Profiles.ordinal) },
            )
            HomeInfoCard(
                modifier = Modifier.weight(1f),
                title = context.getString(com.github.kr328.clash.design.R.string.proxy),
                summary = homeState.mode.ifBlank {
                    context.getString(com.github.kr328.clash.design.R.string.not_selected)
                },
                onClick = { selectPage(Destination.Proxy.ordinal) },
            )
        }
    }

    @Composable
    private fun HomeInfoCard(
        modifier: Modifier = Modifier,
        title: String,
        summary: String,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = modifier,
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    private fun ProxyPage(innerPadding: PaddingValues, nestedScrollConnection: NestedScrollConnection) {
        if (proxyGroups.isEmpty()) {
            EmptyPage(
                innerPadding = innerPadding,
                nestedScrollConnection = nestedScrollConnection,
                summary = context.getString(com.github.kr328.clash.design.R.string.proxy_empty_tips),
            )
            return
        }

        val pagerState = rememberPagerState(pageCount = { proxyGroups.size })
        val scope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage, proxyGroups.size) {
            proxyPageIndex = pagerState.currentPage.coerceIn(0, proxyGroups.lastIndex.coerceAtLeast(0))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            ProxyGroupTabs(
                groups = proxyGroups,
                selectedIndex = pagerState.currentPage,
                onSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                ProxyGroupPage(
                    index = index,
                    group = proxyGroups[index],
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    nestedScrollConnection = nestedScrollConnection,
                )
            }
        }
    }

    @Composable
    private fun ProxyGroupTabs(
        groups: List<ProxyGroupState>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(groups, key = { _, group -> group.name }) { index, group ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.surfaceContainer
                            },
                        )
                        .clickable { onSelected(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (selected) {
                            MiuixTheme.colorScheme.onPrimary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    @Composable
    private fun ModeSelectorCard() {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = context.getString(com.github.kr328.clash.design.R.string.mode),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModeButton(
                        modifier = Modifier.weight(1f),
                        mode = TunnelState.Mode.Rule,
                    )
                    ModeButton(
                        modifier = Modifier.weight(1f),
                        mode = TunnelState.Mode.Global,
                    )
                    ModeButton(
                        modifier = Modifier.weight(1f),
                        mode = TunnelState.Mode.Direct,
                    )
                }
            }
        }
    }

    @Composable
    private fun ModeButton(
        modifier: Modifier = Modifier,
        mode: TunnelState.Mode,
    ) {
        val selected = overrideMode == mode
        val label = when (mode) {
            TunnelState.Mode.Direct -> context.getString(com.github.kr328.clash.design.R.string.direct_mode)
            TunnelState.Mode.Global -> context.getString(com.github.kr328.clash.design.R.string.global_mode)
            TunnelState.Mode.Rule -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
            else -> context.getString(com.github.kr328.clash.design.R.string.rule_mode)
        }

        Button(
            modifier = modifier,
            onClick = { send(Request.PatchMode(mode)) },
            colors = ButtonDefaults.buttonColors(
                color = if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
                contentColor = if (selected) {
                    MiuixTheme.colorScheme.onPrimary
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    @Composable
    private fun ProxyGroupPage(
        index: Int,
        group: ProxyGroupState,
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
        val presentation = proxy.displayPresentation()
        Card(
            onClick = if (selectable) onClick else null,
            colors = CardDefaults.defaultColors(
                color = if (selected) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                }
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary),
                    )
                }
                ProxyRegionBadge(countryCode = presentation.countryCode, type = proxy.type)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = presentation.displayName,
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = proxy.type,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    proxy.delayLabel()?.let { delay ->
                        Text(
                            text = delay,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ProxyRegionBadge(countryCode: String?, type: String) {
        val flag = countryCode?.toFlagEmoji()
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(44.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = flag ?: type.take(2).uppercase(),
                style = if (flag == null) MiuixTheme.textStyles.footnote1 else MiuixTheme.textStyles.title4,
                color = if (flag == null) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }

    private data class FlaggedName(val countryCode: String?, val displayName: String)

    private data class ProxyDisplayPresentation(val countryCode: String?, val displayName: String)

    private fun Proxy.displayPresentation(): ProxyDisplayPresentation {
        val primary = extractFlaggedName(title.ifBlank { name })
        val fallback = extractFlaggedName(name)
        val subtitleFlag = extractFlaggedName(subtitle)

        return ProxyDisplayPresentation(
            countryCode = primary.countryCode ?: fallback.countryCode ?: subtitleFlag.countryCode,
            displayName = primary.displayName.ifBlank { fallback.displayName.ifBlank { name } },
        )
    }

    private fun extractFlaggedName(rawName: String): FlaggedName {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return FlaggedName(countryCode = null, displayName = rawName)

        val flagResult = findFlagEmojiCountryCode(trimmed)
        if (flagResult != null) {
            val (countryCode, flagRange) = flagResult
            val before = trimmed.substring(0, flagRange.first).trimEnd { it.isProxyNameSeparator() }
            val after = trimmed.substring(flagRange.last + 1).trimStart { it.isProxyNameSeparator() }
            val displayName = when {
                before.isNotEmpty() && after.isNotEmpty() -> "$before $after"
                before.isNotEmpty() -> before
                else -> after
            }.ifEmpty { trimmed }

            return FlaggedName(countryCode = countryCode, displayName = displayName)
        }

        val (countryCode, displayName) = extractCountryCodeFromName(trimmed)
        if (countryCode != null) {
            return FlaggedName(countryCode = countryCode, displayName = displayName)
        }

        return FlaggedName(countryCode = null, displayName = trimmed)
    }

    private fun extractCountryCodeFromName(name: String): Pair<String?, String> {
        val match = countryNameRegex.find(name) ?: return null to name
        val countryCode = countryNameToCode[match.value]
            ?: countryNameToCode.entries.find { it.key.equals(match.value, ignoreCase = true) }?.value
            ?: return null to name
        val displayName = buildString {
            append(name.substring(0, match.range.first))
            append(name.substring(match.range.last + 1))
        }.trim { it.isProxyNameSeparator() }

        return countryCode to displayName.ifEmpty { name }
    }

    private fun findFlagEmojiCountryCode(text: String): Pair<String, IntRange>? {
        var index = 0
        while (index < text.length - 1) {
            val first = text.codePointAt(index)
            val firstChars = Character.charCount(first)
            if (!first.isRegionalIndicator()) {
                index += firstChars
                continue
            }

            val secondIndex = index + firstChars
            if (secondIndex >= text.length) break

            val second = text.codePointAt(secondIndex)
            if (!second.isRegionalIndicator()) {
                index += firstChars
                continue
            }

            val countryCode = buildString(2) {
                append(('A'.code + (first - regionalIndicatorBase)).toChar())
                append(('A'.code + (second - regionalIndicatorBase)).toChar())
            }

            return countryCode to index..(secondIndex + Character.charCount(second) - 1)
        }

        return null
    }

    private fun Int.isRegionalIndicator(): Boolean {
        return this in regionalIndicatorBase..regionalIndicatorEnd
    }

    private fun Char.isProxyNameSeparator(): Boolean {
        return isWhitespace() || this == '-' || this == '|' || this == '·' || this == '•' || this == '—' || this == ':'
    }

    private fun String.toFlagEmoji(): String? {
        val code = uppercase()
        if (code.length != 2 || code.any { it !in 'A'..'Z' }) return null

        return buildString {
            code.forEach { appendCodePoint(regionalIndicatorBase + (it.code - 'A'.code)) }
        }
    }

    private fun Proxy.delayLabel(): String? {
        if (delay <= 0) return null

        return "$delay ms"
    }

    @Composable
    private fun ProfilesPage(innerPadding: PaddingValues, nestedScrollConnection: NestedScrollConnection) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { createProfileVisible = true },
                    ) {
                        Icon(MiuixIcons.Add, contentDescription = null)
                        Text(text = context.getString(com.github.kr328.clash.design.R.string._new))
                    }
                    IconButton(
                        enabled = !allProfilesUpdating && profiles.any { it.imported && it.type != Profile.Type.File },
                        onClick = {
                            allProfilesUpdating = true
                            send(Request.UpdateAllProfiles)
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = context.getString(com.github.kr328.clash.design.R.string.update),
                        )
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        ProfileMetaText(profile)
                    }
                    Switch(
                        checked = profile.active,
                        onCheckedChange = {
                            if (!profile.active) {
                                send(Request.ActiveProfile(profile))
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { send(Request.EditProfile(profile)) }) {
                        Icon(
                            imageVector = MiuixIcons.Edit,
                            contentDescription = context.getString(com.github.kr328.clash.design.R.string.edit),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CreateProfileSheet() {
        WindowBottomSheet(
            show = createProfileVisible,
            title = context.getString(com.github.kr328.clash.design.R.string.new_profile),
            startAction = {
                IconButton(onClick = { closeCreateProfile() }) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.cancel),
                    )
                }
            },
            onDismissRequest = { closeCreateProfile() },
            insideMargin = DpSize(16.dp, 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CreateProfileTypeButton(
                        modifier = Modifier.weight(1f),
                        type = Profile.Type.Url,
                        icon = MiuixIcons.Link,
                        title = context.getString(com.github.kr328.clash.design.R.string.subscription),
                    )
                    CreateProfileTypeButton(
                        modifier = Modifier.weight(1f),
                        type = Profile.Type.File,
                        icon = MiuixIcons.File,
                        title = context.getString(com.github.kr328.clash.design.R.string.file),
                    )
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { send(Request.LaunchProfileScanner) },
                    ) {
                        Icon(MiuixIcons.Scan, contentDescription = null)
                        Text(
                            text = context.getString(com.github.kr328.clash.design.R.string.qr),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                TextField(
                    value = createProfileName,
                    onValueChange = { createProfileName = it },
                    label = context.getString(com.github.kr328.clash.design.R.string.name),
                    singleLine = true,
                )

                if (createProfileType == Profile.Type.Url) {
                    TextField(
                        value = createProfileUrl,
                        onValueChange = { createProfileUrl = it },
                        label = context.getString(com.github.kr328.clash.design.R.string.profile_url),
                        maxLines = 2,
                    )
                    TextField(
                        value = createProfileInterval,
                        onValueChange = { createProfileInterval = it.filter(Char::isDigit) },
                        label = context.getString(com.github.kr328.clash.design.R.string.auto_update_minutes),
                        singleLine = true,
                    )
                } else {
                    Card(onClick = { send(Request.PickProfileFile) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(MiuixIcons.File, contentDescription = null)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = context.getString(com.github.kr328.clash.design.R.string.select_file),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = createProfileFileName.ifBlank {
                                        context.getString(com.github.kr328.clash.design.R.string.not_selected)
                                    },
                                    style = MiuixTheme.textStyles.body2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { submitCreateProfile() },
                ) {
                    Icon(MiuixIcons.Ok, contentDescription = null)
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.save))
                }
            }
        }
    }

    @Composable
    private fun CreateProfileTypeButton(
        modifier: Modifier,
        type: Profile.Type,
        icon: ImageVector,
        title: String,
    ) {
        Button(
            modifier = modifier,
            onClick = {
                createProfileType = type
                if (type == Profile.Type.File) {
                    createProfileUrl = ""
                    createProfileInterval = ""
                }
            },
        ) {
            Icon(icon, contentDescription = null)
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (createProfileType == type) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }

    @Composable
    private fun ProfileMetaText(profile: Profile) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = when (profile.type) {
                    Profile.Type.Url -> context.getString(com.github.kr328.clash.design.R.string.subscription)
                    else -> profile.type.toString(context)
                },
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (profile.type != Profile.Type.File && profile.source.isNotBlank()) {
                Text(
                    text = profile.source,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    private fun submitCreateProfile() {
        val name = createProfileName.trim().ifBlank {
            context.getString(com.github.kr328.clash.design.R.string.new_profile)
        }

        if (createProfileType == Profile.Type.Url) {
            send(
                Request.CreateProfileUrl(
                    name = name,
                    url = createProfileUrl.trim(),
                    interval = createProfileInterval.toLongOrNull()?.let(TimeUnit.MINUTES::toMillis) ?: 0L,
                )
            )
        } else {
            val uri = createProfileFileUri ?: return
            send(Request.CreateProfileFile(name, uri))
        }
        closeCreateProfile()
    }

    private fun closeCreateProfile() {
        createProfileVisible = false
        createProfileType = Profile.Type.Url
        createProfileName = ""
        createProfileUrl = ""
        createProfileInterval = ""
        createProfileFileUri = null
        createProfileFileName = ""
    }

    @Composable
    private fun SettingsPage(innerPadding: PaddingValues, nestedScrollConnection: NestedScrollConnection) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingsItem(
                    icon = MiuixIcons.Settings,
                    title = context.getString(com.github.kr328.clash.design.R.string.app),
                    summary = "应用行为与服务显示",
                ) {
                    send(Request.StartAppSettings)
                }
            }
            item {
                SettingsItem(
                    icon = MiuixIcons.VerticalSplit,
                    title = context.getString(com.github.kr328.clash.design.R.string.network),
                    summary = "VPN、代理与访问控制",
                ) {
                    send(Request.StartNetworkSettings)
                }
            }
            item {
                SettingsItem(
                    icon = MiuixIcons.More,
                    title = context.getString(com.github.kr328.clash.design.R.string.override),
                    summary = "配置覆写与高级参数",
                ) {
                    send(Request.StartOverrideSettings)
                }
            }
            item {
                SettingsItem(
                    icon = MiuixIcons.Contacts,
                    title = context.getString(com.github.kr328.clash.design.R.string.meta_features),
                    summary = "Meta 特性与地理数据",
                ) {
                    send(Request.StartMetaFeatureSettings)
                }
            }
        }
    }

    @Composable
    private fun ActionItem(title: String, onClick: () -> Unit) {
        SettingsItem(
            icon = MiuixIcons.More,
            title = title,
            summary = "",
            onClick = onClick,
        )
    }

    @Composable
    private fun SettingsItem(
        icon: ImageVector,
        title: String,
        summary: String,
        onClick: () -> Unit,
    ) {
        Card(onClick = onClick) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyPage(
        innerPadding: PaddingValues,
        nestedScrollConnection: NestedScrollConnection,
        summary: String,
    ) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(text = summary, style = MiuixTheme.textStyles.body1)
            }
        }
    }

    private fun Destination.label(): String {
        return when (this) {
            Destination.Home -> context.getString(com.github.kr328.clash.design.R.string.home)
            Destination.Proxy -> context.getString(com.github.kr328.clash.design.R.string.proxy)
            Destination.Profiles -> context.getString(com.github.kr328.clash.design.R.string.profile)
            Destination.Apps -> context.getString(com.github.kr328.clash.design.R.string.app)
            Destination.Settings -> context.getString(com.github.kr328.clash.design.R.string.settings)
        }
    }
}
