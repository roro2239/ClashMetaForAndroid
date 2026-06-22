package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toString
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
import com.github.kr328.clash.ui.ClashMiuixTheme
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.util.concurrent.TimeUnit

class ProfilesComposeDesign(context: Context) : Design<ProfilesComposeDesign.Request>(context) {
    sealed class Request {
        object UpdateAll : Request()
        object PickFile : Request()
        object LaunchScanner : Request()
        data class CreateUrl(val name: String, val url: String, val interval: Long) : Request()
        data class CreateFile(val name: String, val uri: Uri) : Request()
        data class Active(val profile: Profile) : Request()
        data class Edit(val profile: Profile) : Request()
        data class Save(val profile: Profile) : Request()
    }

    private var profiles by mutableStateOf<List<Profile>>(emptyList())
    private var allUpdating by mutableStateOf(false)
    private var now by mutableLongStateOf(System.currentTimeMillis())
    private var createSheetVisible by mutableStateOf(false)
    private var createType by mutableStateOf(Profile.Type.Url)
    private var createName by mutableStateOf("")
    private var createUrl by mutableStateOf("")
    private var createInterval by mutableStateOf("")
    private var createFileUri by mutableStateOf<Uri?>(null)
    private var createFileName by mutableStateOf("")
    private var editingProfile by mutableStateOf<Profile?>(null)
    private var editingName by mutableStateOf("")
    private var editingSource by mutableStateOf("")
    private var editingInterval by mutableStateOf("")

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides navigationOwner
                ) {
                    PageContent()
                }
            }
        }
    }

    fun showCreateSheet() {
        createSheetVisible = true
    }

    fun patchPickedFile(uri: Uri, name: String) {
        createFileUri = uri
        createFileName = name
        if (createName.isBlank()) {
            createName = name.substringBeforeLast(".").ifBlank {
                context.getString(com.github.kr328.clash.design.R.string.new_profile)
            }
        }
    }

    fun patchScannedUrl(url: String) {
        createType = Profile.Type.Url
        createUrl = url
        createSheetVisible = true
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
                IconButton(onClick = { createSheetVisible = true }) {
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

        CreateProfileSheet()
        EditProfileSheet()
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
                                requests.trySend(Request.Active(profile))
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { openEditSheet(profile) }) {
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
            show = createSheetVisible,
            title = context.getString(com.github.kr328.clash.design.R.string.new_profile),
            startAction = {
                IconButton(onClick = { closeCreateSheet() }) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.cancel),
                    )
                }
            },
            onDismissRequest = { closeCreateSheet() },
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
                    CreateTypeButton(
                        modifier = Modifier.weight(1f),
                        type = Profile.Type.Url,
                        icon = MiuixIcons.Link,
                        title = context.getString(com.github.kr328.clash.design.R.string.subscription),
                    )
                    CreateTypeButton(
                        modifier = Modifier.weight(1f),
                        type = Profile.Type.File,
                        icon = MiuixIcons.File,
                        title = context.getString(com.github.kr328.clash.design.R.string.file),
                    )
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { requests.trySend(Request.LaunchScanner) },
                    ) {
                        Icon(MiuixIcons.Scan, contentDescription = null)
                        Text(text = context.getString(com.github.kr328.clash.design.R.string.qr))
                    }
                }

                TextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = context.getString(com.github.kr328.clash.design.R.string.name),
                    singleLine = true,
                )

                if (createType == Profile.Type.Url) {
                    TextField(
                        value = createUrl,
                        onValueChange = { createUrl = it },
                        label = context.getString(com.github.kr328.clash.design.R.string.profile_url),
                        maxLines = 2,
                    )
                    TextField(
                        value = createInterval,
                        onValueChange = { createInterval = it.filter(Char::isDigit) },
                        label = context.getString(com.github.kr328.clash.design.R.string.auto_update_minutes),
                        singleLine = true,
                    )
                } else {
                    Card(onClick = { requests.trySend(Request.PickFile) }) {
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
                                    text = createFileName.ifBlank {
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
                    onClick = { submitCreateSheet() },
                ) {
                    Icon(MiuixIcons.Ok, contentDescription = null)
                    Text(text = context.getString(com.github.kr328.clash.design.R.string.save))
                }
            }
        }
    }

    @Composable
    private fun CreateTypeButton(
        modifier: Modifier,
        type: Profile.Type,
        icon: ImageVector,
        title: String,
    ) {
        Button(
            modifier = modifier,
            onClick = {
                createType = type
                if (type == Profile.Type.File) {
                    createUrl = ""
                    createInterval = ""
                }
            },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null)
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (createType == type) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
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
            Text(
                text = "${context.getString(com.github.kr328.clash.design.R.string.update)} · ${(now - profile.updatedAt).elapsedIntervalString(context)}",
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    private fun submitCreateSheet() {
        val name = createName.trim().ifBlank {
            context.getString(com.github.kr328.clash.design.R.string.new_profile)
        }

        if (createType == Profile.Type.Url) {
            requests.trySend(
                Request.CreateUrl(
                    name = name,
                    url = createUrl.trim(),
                    interval = createInterval.toLongOrNull()?.let(TimeUnit.MINUTES::toMillis) ?: 0L,
                )
            )
        } else {
            val uri = createFileUri ?: return
            requests.trySend(Request.CreateFile(name, uri))
        }
        closeCreateSheet()
    }

    private fun closeCreateSheet() {
        createSheetVisible = false
        createType = Profile.Type.Url
        createName = ""
        createUrl = ""
        createInterval = ""
        createFileUri = null
        createFileName = ""
    }

    @Composable
    private fun EditProfileSheet() {
        val profile = editingProfile ?: return

        WindowBottomSheet(
            show = true,
            title = context.getString(com.github.kr328.clash.design.R.string.properties),
            startAction = {
                IconButton(onClick = { closeEditSheet() }) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.cancel),
                    )
                }
            },
            endAction = {
                IconButton(onClick = { saveEditSheet(profile) }) {
                    Icon(
                        imageVector = MiuixIcons.Ok,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.save),
                    )
                }
            },
            onDismissRequest = { closeEditSheet() },
            insideMargin = DpSize(16.dp, 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    label = context.getString(com.github.kr328.clash.design.R.string.name),
                    singleLine = true,
                )
                if (profile.type != Profile.Type.File) {
                    TextField(
                        value = editingSource,
                        onValueChange = { editingSource = it },
                        label = context.getString(com.github.kr328.clash.design.R.string.url),
                        singleLine = true,
                    )
                }
                if (profile.type != Profile.Type.File) {
                    TextField(
                        value = editingInterval,
                        onValueChange = { editingInterval = it.filter(Char::isDigit) },
                        label = context.getString(com.github.kr328.clash.design.R.string.auto_update_minutes),
                        singleLine = true,
                    )
                }
            }
        }
    }

    private fun openEditSheet(profile: Profile) {
        editingProfile = profile
        editingName = profile.name
        editingSource = profile.source
        val minutes = TimeUnit.MILLISECONDS.toMinutes(profile.interval)
        editingInterval = if (minutes == 0L) "" else minutes.toString()
    }

    private fun closeEditSheet() {
        editingProfile = null
        editingName = ""
        editingSource = ""
        editingInterval = ""
    }

    private fun saveEditSheet(profile: Profile) {
        val interval = editingInterval.toLongOrNull()?.let(TimeUnit.MINUTES::toMillis) ?: 0L
        requests.trySend(
            Request.Save(
                profile.copy(
                    name = editingName.trim(),
                    source = if (profile.type == Profile.Type.File) profile.source else editingSource.trim(),
                    interval = interval,
                    ageSecretKey = null,
                )
            )
        )
        closeEditSheet()
    }

}
