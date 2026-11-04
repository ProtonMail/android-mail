/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailcommon.domain.model.DataError
import me.proton.core.domain.entity.UserId

/**
 * BYOE eligibility, resolved by the Rust SDK from the experiment enrolment and the user's existing connections.
 */
interface ByoeGuidanceRepository {

    suspend fun getSettingsAccess(userId: UserId): Either<DataError, GmailConnectionSettingsAccess>

    suspend fun getOnboardingTreatment(userId: UserId): Either<DataError, ByoeTreatment?>

    suspend fun getSpotlightTreatment(userId: UserId): Either<DataError, ByoeTreatment?>

    suspend fun getSidebarBannerTreatment(userId: UserId): Either<DataError, ByoeTreatment?>

    suspend fun markSidebarBannerClosed(userId: UserId): Either<DataError, Unit>

    suspend fun markSpotlightShown(userId: UserId): Either<DataError, Unit>

    suspend fun markGuidanceDismissed(userId: UserId, surface: ByoeSurface): Either<DataError, Unit>

    suspend fun isOnboardingFinishedRecorded(userId: UserId): Either<DataError, Boolean>

    suspend fun recordOnboardingFinished(userId: UserId): Either<DataError, Unit>

    suspend fun recordOnboardingPageShown(userId: UserId): Either<DataError, Unit>
}
