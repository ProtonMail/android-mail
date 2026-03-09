/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.data

import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.repository.getPrimarySession
import me.proton.android.core.payment.domain.IconResourceManager
import uniffi.mail_uniffi.MailUserSessionGetPaymentsResourcesIconsResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconResourceManagerRust @Inject constructor(
    private val sessionRepository: UserSessionRepository
) : IconResourceManager {

    override suspend fun getBitmap(iconName: String): ByteArray? {
        val session = sessionRepository.getPrimarySession() ?: return null
        return when (val result = session.getPaymentsResourcesIcons(iconName)) {
            is MailUserSessionGetPaymentsResourcesIconsResult.Error -> null
            is MailUserSessionGetPaymentsResourcesIconsResult.Ok -> result.v1
        }
    }
}
