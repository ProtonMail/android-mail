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

package ch.protonmail.android.mailsettings.data.local

import ch.protonmail.android.mailcommon.data.mapper.LocalMailSettings
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsettings.data.usecase.CreateRustUserMailSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.LiveQueryCallback
import javax.inject.Inject

class RustMailSettingsDataSource @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val createRustMailSettings: CreateRustUserMailSettings
) : MailSettingsDataSource {

    override fun observeMailSettings(userId: UserId): Flow<LocalMailSettings> {
        val restartTrigger = MutableSharedFlow<Unit>(replay = 1)
        restartTrigger.tryEmit(Unit)
        return restartTrigger.flatMapLatest {
            callbackFlow {
                Timber.d("rust-settings: initializing mail settings live query")
                val session = userSessionRepository.getUserSession(userId)
                if (session == null) {
                    Timber.e("rust-settings: trying to load settings with a null session")
                    close()
                    return@callbackFlow
                }

                val settingsCallback = object : LiveQueryCallback {
                    override fun onUpdate() {
                        // value is updated; we must re-create createRustMailSettings for the latest value
                        restartTrigger.tryEmit(Unit)
                        Timber.d("rust-settings: mail settings updated")
                    }
                }

                val watcherResult = createRustMailSettings(session, settingsCallback)
                    .onLeft {
                        close()
                        Timber.e("rust-settings: failed creating settings watcher $it")
                    }
                    .onRight { settingsWatcher ->
                        Timber.d(
                            "rust-settings: Setting initial value for mail settings and watching for updates"
                        )
                        send(settingsWatcher.settings)
                    }

                awaitClose {
                    watcherResult.getOrNull()?.watchHandle?.disconnect()
                    watcherResult.getOrNull()?.destroy()
                }
            }
        }
    }
}
