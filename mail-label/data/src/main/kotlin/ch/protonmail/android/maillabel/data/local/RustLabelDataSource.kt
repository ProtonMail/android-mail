/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maillabel.data.local

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.MailLabelRustCoroutineScope
import ch.protonmail.android.maillabel.data.usecase.CreateRustSidebar
import ch.protonmail.android.maillabel.data.usecase.RustGetAllMailLabelId
import ch.protonmail.android.maillabel.data.wrapper.SidebarWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.SidebarCustomFolder
import uniffi.mail_uniffi.SidebarCustomLabel
import uniffi.mail_uniffi.SidebarSystemLabel
import uniffi.mail_uniffi.WatchHandle
import javax.inject.Inject

class RustLabelDataSource @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val createRustSidebar: CreateRustSidebar,
    private val rustGetAllMailLabelId: RustGetAllMailLabelId,
    private val rustGetSystemLabelById: RustGetSystemLabelById,
    private val rustGetLabelIdBySystemLabel: RustGetLabelIdBySystemLabel,
    @MailLabelRustCoroutineScope private val coroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : LabelDataSource {

    private suspend fun getRustSidebarInstance(userId: UserId): SidebarWrapper? {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-label: trying to load labels with a null session")
            return null
        }
        return createRustSidebar(session)
    }

    private fun <T> observeLabels(userId: UserId, fetchLabels: suspend (SidebarWrapper) -> List<T>?): Flow<List<T>> =
        callbackFlow {
            Timber.d("rust-label: initializing labels live query")

            var sidebar = getRustSidebarInstance(userId)
            if (sidebar == null) {
                close()
                return@callbackFlow
            }

            val mutex = Mutex()
            var labelsWatchHandle: WatchHandle? = null

            suspend fun withSidebar(action: suspend (SidebarWrapper) -> Unit) {
                mutex.withLock {
                    sidebar?.let { action(it) }
                }
            }

            val labelsUpdatedCallback = object : LiveQueryCallback {
                override fun onUpdate() {
                    coroutineScope.launch {
                        withSidebar { safeSidebar ->
                            fetchLabels(safeSidebar)?.let {
                                send(it)
                            }
                        }
                    }
                }
            }

            sidebar.watchLabels(labelsUpdatedCallback)
                .onLeft {
                    close()
                    Timber.e("rust-label: failed to watch labels! $it")
                }
                .onRight { watcher ->
                    labelsWatchHandle = watcher
                    coroutineScope.launch {
                        withSidebar { safeSidebar ->
                            fetchLabels(safeSidebar)?.let {
                                send(it)
                                Timber.d("rust-label: Setting initial value for labels")
                            }
                        }
                    }
                }

            awaitClose {
                coroutineScope.launch {
                    mutex.withLock {
                        labelsWatchHandle?.destroy()
                        sidebar?.destroy()
                        sidebar = null
                        Timber.d("rust-label: watcher for labels destroyed")

                    }
                }
            }
        }.flowOn(ioDispatcher)

    override fun observeSystemLabels(userId: UserId): Flow<List<SidebarSystemLabel>> = observeLabels(
        userId = userId
    ) { sidebar ->
        sidebar.systemLabels().getOrNull()
    }.flowOn(ioDispatcher)

    override fun observeMessageLabels(userId: UserId): Flow<List<SidebarCustomLabel>> = observeLabels(
        userId = userId
    ) { sidebar ->
        sidebar.customLabels().getOrNull()
    }.flowOn(ioDispatcher)

    override fun observeMessageFolders(userId: UserId): Flow<List<SidebarCustomFolder>> = observeLabels(
        userId = userId
    ) { sidebar ->
        sidebar.allCustomFolders().getOrNull()
    }.flowOn(ioDispatcher)

    override suspend fun getAllMailLabelId(userId: UserId): Either<DataError, LocalLabelId> =
        withContext(ioDispatcher) {
            val session = userSessionRepository.getUserSession(userId)
            if (session == null) {
                Timber.e("rust-label: trying to get all mail label id with null session.")
                return@withContext DataError.Local.NoUserSession.left()
            }
            return@withContext rustGetAllMailLabelId(session)
        }

    override suspend fun resolveSystemLabelByLocalId(
        userId: UserId,
        labelId: LocalLabelId
    ): Either<DataError, LocalSystemLabel> {
        return withContext(ioDispatcher) {
            val session = userSessionRepository.getUserSession(userId)

            if (session == null) {
                Timber.e("rust-label: trying to resolve system label by local id with null session.")
                return@withContext DataError.Local.NoUserSession.left()
            }

            rustGetSystemLabelById(session, labelId)
        }
    }

    override suspend fun resolveLocalIdBySystemLabel(
        userId: UserId,
        systemLabel: LocalSystemLabel
    ): Either<DataError, LocalLabelId> {
        return withContext(ioDispatcher) {
            val session = userSessionRepository.getUserSession(userId)

            if (session == null) {
                Timber.e("rust-label: trying to resolve local id by system label with null session.")
                return@withContext DataError.Local.NoUserSession.left()
            }

            rustGetLabelIdBySystemLabel(session, systemLabel)
        }
    }
}
