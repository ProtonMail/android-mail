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

package ch.protonmail.android.mailcomposer.domain.usecase

import java.time.Duration
import java.time.Instant
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshComposerSenderAddresses @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val userManager: UserManager
) {

    private val refreshesDone = mutableMapOf<UserId, Long>()

    suspend operator fun invoke() {
        val userId = observePrimaryUserId().first() ?: return
        val existingRefresh = refreshesDone[userId]
        val time = Instant.now().toEpochMilli()
        if (existingRefresh != null && existingRefresh + ValidityDurationMs > time) {
            return
        }
        userManager.getAddresses(userId, refresh = true)
        refreshesDone[userId] = Instant.now().toEpochMilli()
    }

    private companion object {
        val ValidityDurationMs = Duration.ofMinutes(5).toMillis()
    }
}
