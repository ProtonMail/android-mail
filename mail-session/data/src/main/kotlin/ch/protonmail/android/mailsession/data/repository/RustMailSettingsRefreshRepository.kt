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

package ch.protonmail.android.mailsession.data.repository

import ch.protonmail.android.mailsession.domain.repository.MailSettingsRefreshRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class RustMailSettingsRefreshRepository @Inject constructor(
    private val userSessionRepository: UserSessionRepository
) : MailSettingsRefreshRepository {

    override suspend fun refresh(userId: UserId) {
        val userSession = userSessionRepository.getUserSession(userId)
        if (userSession == null) {
            Timber.w("rust-settings: mail settings refresh triggered for $userId but no session found. Stopping...")
            return
        }
        userSession.refreshMailSettings()
            .onRight { Timber.d("rust-settings: refreshed mail settings for $userId") }
            .onLeft { Timber.e("rust-settings: failed to refresh mail settings for $userId: $it") }
    }
}
