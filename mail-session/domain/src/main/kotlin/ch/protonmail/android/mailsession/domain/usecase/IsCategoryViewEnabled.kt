/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.domain.usecase

import arrow.core.getOrElse
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Resolves the category view availability for the given user.
 *
 * The value is owned by the Rust SDK, which combines the feature flag with any additional
 * eligibility rule, so it must not be read through the plain feature flag resolver.
 * Falls back to `false` when there is no user session or the SDK call fails.
 */
class IsCategoryViewEnabled @Inject constructor(
    private val userSessionRepository: UserSessionRepository
) {

    suspend operator fun invoke(userId: UserId): Boolean =
        userSessionRepository.isCategoryViewEnabled(userId).getOrElse { false }
}
