package com.github.kr328.clash

import android.app.Activity
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.design.R
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.util.pendingDir
import com.github.kr328.clash.util.withProfile
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRError
import io.github.g00fy2.quickie.QRResult.QRMissingPermission
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.QRResult.QRUserCanceled
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.TimeUnit

class ProfilesActivity : BaseActivity<ProfilesComposeDesign>() {
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override suspend fun main() {
        val design = ProfilesComposeDesign(this)

        if (intent.getBooleanExtra(Intents.EXTRA_OPEN_CREATE_PROFILE, false))
            design.showCreateSheet()
        setContentDesign(design)

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ProfileChanged -> {
                            design.fetch()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        ProfilesComposeDesign.Request.PickFile -> {
                            val uri = startActivityForResult(
                                ActivityResultContracts.GetContent(),
                                "*/*",
                            )

                            if (uri != null) {
                                design.patchPickedFile(uri, queryDisplayName(uri))
                            }
                        }
                        ProfilesComposeDesign.Request.LaunchScanner -> {
                            scanLauncher.launch(null)
                        }
                        is ProfilesComposeDesign.Request.CreateUrl -> {
                            createUrlProfile(design, it)
                        }
                        is ProfilesComposeDesign.Request.CreateFile -> {
                            createFileProfile(design, it)
                        }
                        ProfilesComposeDesign.Request.UpdateAll ->
                            withProfile {
                                try {
                                    queryAll().forEach { p ->
                                        if (p.imported && p.type != Profile.Type.File)
                                            update(p.uuid)
                                    }
                                }
                                finally {
                                    withContext(Dispatchers.Main) {
                                        design.finishUpdateAll();
                                    }
                                }
                            }
                        is ProfilesComposeDesign.Request.Update ->
                            withProfile { update(it.profile.uuid) }
                        is ProfilesComposeDesign.Request.Delete ->
                            withProfile { delete(it.profile.uuid) }
                        is ProfilesComposeDesign.Request.Edit ->
                            startActivity(PropertiesActivity::class.intent.setUUID(it.profile.uuid))
                        is ProfilesComposeDesign.Request.Save -> {
                            val profile = it.profile

                            when {
                                profile.name.isBlank() -> {
                                    design.showToast(R.string.empty_name, ToastDuration.Long)
                                }
                                profile.type != Profile.Type.File && profile.source.isBlank() -> {
                                    design.showToast(R.string.invalid_url, ToastDuration.Long)
                                }
                                else -> {
                                    withProfile {
                                        patch(
                                            profile.uuid,
                                            profile.name,
                                            profile.source,
                                            profile.interval,
                                            null,
                                        )
                                        commit(profile.uuid) {}
                                    }
                                    design.fetch()
                                }
                            }
                        }
                        is ProfilesComposeDesign.Request.Active -> {
                            withProfile {
                                if (it.profile.imported)
                                    setActive(it.profile)
                                else
                                    design.requestSave(it.profile)
                            }
                        }
                        is ProfilesComposeDesign.Request.Duplicate -> {
                            val uuid = withProfile { clone(it.profile.uuid) }

                            startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                        }
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        design.updateElapsed()
                    }
                }
            }
        }
    }

    private suspend fun createUrlProfile(
        design: ProfilesComposeDesign,
        request: ProfilesComposeDesign.Request.CreateUrl,
    ) {
        if (!request.url.isValidProfileUrl()) {
            design.showToast(R.string.invalid_url, ToastDuration.Long)
            return
        }

        withProfile {
            val uuid = create(Profile.Type.Url, request.name, request.url, null)

            if (request.interval > 0) {
                patch(uuid, request.name, request.url, request.interval, null)
            }
            commit(uuid) {}
        }
        design.fetch()
    }

    private suspend fun createFileProfile(
        design: ProfilesComposeDesign,
        request: ProfilesComposeDesign.Request.CreateFile,
    ) {
        val uuid = withProfile {
            create(Profile.Type.File, request.name, "", null)
        }

        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(request.uri).use { input ->
                if (input == null) {
                    throw FileNotFoundException(request.uri.toString())
                }

                val target = pendingDir
                    .resolve(uuid.toString())
                    .resolve("config.yaml")

                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        withProfile {
            commit(uuid) {}
        }
        design.fetch()
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val name = cursor.getString(index)
                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank {
            getString(R.string.new_profile)
        } ?: getString(R.string.new_profile)
    }

    private fun String.isValidProfileUrl(): Boolean {
        val scheme = Uri.parse(this).scheme

        return scheme == "http" || scheme == "https"
    }

    private suspend fun ProfilesComposeDesign.fetch() {
        withProfile {
            patchProfiles(queryAll())
        }
    }

    private fun scanResultHandler(result: QRResult) {
        lifecycleScope.launch {
            when (result) {
                is QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.let { String(it) }.orEmpty()

                    design?.patchScannedUrl(url)
                }

                QRUserCanceled -> {}
                QRMissingPermission -> design?.showExceptionToast(getString(R.string.import_from_qr_no_permission))
                is QRError -> design?.showExceptionToast(getString(R.string.import_from_qr_exception))
            }
        }
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if(uuid == null)
            return;
        launch {
            var name: String? = null;
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            design?.showToast(
                getString(R.string.toast_profile_updated_complete, name),
                ToastDuration.Long
            )
        }
    }
    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if(uuid == null)
            return;
        launch {
            var name: String? = null;
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            design?.showToast(
                getString(R.string.toast_profile_updated_failed, name, reason),
                ToastDuration.Long
            ){
                setAction(R.string.edit) {
                    startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                }
            }
        }
    }
}
