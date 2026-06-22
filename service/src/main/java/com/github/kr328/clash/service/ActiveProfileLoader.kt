package com.github.kr328.clash.service

import android.content.Context
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.Imported
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.importedDir
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

object ActiveProfileLoader {
    private val mutex = Mutex()
    @Volatile
    private var loaded: UUID? = null

    fun invalidate() {
        loaded = null
    }

    suspend fun ensure(context: Context, force: Boolean = false): Imported {
        return mutex.withLock {
            val current = ServiceStore(context).activeProfile
                ?: throw NullPointerException("No profile selected")

            val active = ImportedDao().queryByUUID(current)
                ?: throw NullPointerException("No profile selected")

            if (force || current != loaded) {
                Clash.setAgeSecretKey(active.ageSecretKey?.takeIf { it.isNotBlank() })
                Clash.load(context.importedDir.resolve(active.uuid.toString())).await()

                val remove = SelectionDao().querySelections(active.uuid)
                    .filterNot { Clash.patchSelector(it.proxy, it.selected) }
                    .map { it.proxy }

                SelectionDao().removeSelections(active.uuid, remove)

                StatusProvider.currentProfile = active.name
                loaded = current
            }

            active
        }
    }
}
