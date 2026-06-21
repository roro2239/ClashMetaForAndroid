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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

class ProfilesComposeDesign(context: Context) : Design<ProfilesComposeDesign.Request>(context) {
    sealed class Request {
        object UpdateAll : Request()
        object Create : Request()
        data class Active(val profile: Profile) : Request()
        data class Update(val profile: Profile) : Request()
        data class Edit(val profile: Profile) : Request()
        data class Duplicate(val profile: Profile) : Request()
        data class Delete(val profile: Profile) : Request()
    }

    private var profiles by mutableStateOf<List<Profile>>(emptyList())
    private var allUpdating by mutableStateOf(false)
    private var now by mutableLongStateOf(System.currentTimeMillis())

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
        }
    }

    suspend fun patchProfiles(profiles: List<Profile>) {
        withContext(Dispatchers.Main) {
            this@ProfilesComposeDesign.profiles = profiles
        }
    }

    suspend fun requestSave(profile: Profile) {
        showToast(com.github.kr328.clash.design.R.string.active_unsaved_tips, ToastDuration.Long) {
            setAction(com.github.kr328.clash.design.R.string.edit) {
                requests.trySend(Request.Edit(profile))
            }
        }
    }

    fun updateElapsed() {
        now = System.currentTimeMillis()
    }

    fun requestUpdateAll() {
        allUpdating = true
        requests.trySend(Request.UpdateAll)
    }

    fun finishUpdateAll() {
        allUpdating = false
    }

    @Composable
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.profile),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
            actions = {
                IconButton(onClick = { requests.trySend(Request.Create) }) {
                    Icon(MiuixIcons.Add, contentDescription = context.getString(com.github.kr328.clash.design.R.string._new))
                }
                IconButton(
                    enabled = !allUpdating && profiles.any { it.imported && it.type != Profile.Type.File },
                    onClick = { requestUpdateAll() },
                ) {
                    Icon(MiuixIcons.Refresh, contentDescription = context.getString(com.github.kr328.clash.design.R.string.update))
                }
            },
        ) { innerPadding, nestedScrollConnection ->
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
                items(profiles, key = { it.uuid }) { profile ->
                    ProfileItem(profile)
                }
            }
        }
    }

    @Composable
    private fun ProfileItem(profile: Profile) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${profile.type.name} · ${(now - profile.updatedAt).elapsedIntervalString(context)}",
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { requests.trySend(Request.Active(profile)) },
                    ) {
                        Text(
                            text = if (profile.active) {
                                context.getString(com.github.kr328.clash.design.R.string.active)
                            } else {
                                context.getString(com.github.kr328.clash.design.R.string.activate)
                            },
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { requests.trySend(Request.Edit(profile)) },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.edit))
                    }
                    if (profile.imported && profile.type != Profile.Type.File) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { requests.trySend(Request.Update(profile)) },
                        ) {
                            Text(text = context.getString(com.github.kr328.clash.design.R.string.update))
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { requests.trySend(Request.Duplicate(profile)) },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.duplicate))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { requests.trySend(Request.Delete(profile)) },
                    ) {
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.delete))
                    }
                }
            }
        }
    }
}
