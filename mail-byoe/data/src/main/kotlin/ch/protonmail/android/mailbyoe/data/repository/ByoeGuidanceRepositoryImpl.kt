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

package ch.protonmail.android.mailbyoe.data.repository

import arrow.core.Either
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ByoeDismissMethod
import uniffi.mail_uniffi.ByoeGuidance
import uniffi.mail_uniffi.ByoeGuidanceTreatment
import uniffi.mail_uniffi.ByoeSettingsAccess
import uniffi.mail_uniffi.OnboardingStep
import javax.inject.Inject

class ByoeGuidanceRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession
) : ByoeGuidanceRepository {

    override suspend fun getSettingsAccess(userId: UserId): Either<DataError, GmailConnectionSettingsAccess> =
        withByoeGuidance(userId) { guidance ->
            guidance.settings().also { Timber.d("byoe: Rust guidance settings = $it") }.toSettingsAccess()
        }

    override suspend fun getOnboardingTreatment(userId: UserId): Either<DataError, ByoeTreatment?> =
        withByoeGuidance(userId) { guidance ->
            guidance.onboarding().also { Timber.d("byoe: Rust guidance onboarding = $it") }?.toTreatment()
        }

    override suspend fun getSpotlightTreatment(userId: UserId): Either<DataError, ByoeTreatment?> =
        withByoeGuidance(userId) { guidance ->
            guidance.spotlight().also { Timber.d("byoe: Rust guidance spotlight = $it") }?.toTreatment()
        }

    override suspend fun getSidebarBannerTreatment(userId: UserId): Either<DataError, ByoeTreatment?> =
        withByoeGuidance(userId) { guidance ->
            guidance.sidebarBanner().also { Timber.d("byoe: Rust guidance sidebar banner = $it") }?.toTreatment()
        }

    override suspend fun markSidebarBannerClosed(userId: UserId): Either<DataError, Unit> =
        withByoeGuidance(userId) { guidance -> guidance.markSidebarBannerClosed(ByoeDismissMethod.BUTTON) }

    override suspend fun markSpotlightShown(userId: UserId): Either<DataError, Unit> =
        withByoeGuidance(userId) { guidance -> guidance.markSpotlightShown() }

    override suspend fun markGuidanceDismissed(userId: UserId, surface: ByoeSurface): Either<DataError, Unit> =
        withByoeGuidance(userId) { guidance ->
            when (surface) {
                ByoeSurface.Onboarding -> guidance.markOnboardingDismissed(ByoeDismissMethod.BUTTON)
                ByoeSurface.Spotlight -> guidance.markSpotlightDismissed(ByoeDismissMethod.BUTTON)
                ByoeSurface.SidebarBanner,
                ByoeSurface.Settings -> Timber.w("byoe: no guidance to dismiss on $surface")
            }
        }

    override suspend fun isOnboardingFinishedRecorded(userId: UserId): Either<DataError, Boolean> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                store.isOnboardingStepShown(OnboardingStep.FINISHED)
            }
        }

    override suspend fun recordOnboardingFinished(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                store.markOnboardingStep(OnboardingStep.FINISHED)
            }
        }

    override suspend fun recordOnboardingPageShown(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                store.markOnboardingStep(OnboardingStep.BYOE)
            }
        }

    private suspend fun <T> withByoeGuidance(
        userId: UserId,
        action: suspend (ByoeGuidance) -> T
    ): Either<DataError, T> = executeWithUserSession(userId) { session ->
        session.getRustUserSession().byoeGuidance().use { action(it) }
    }
}

private fun ByoeSettingsAccess.toSettingsAccess() = when (this) {
    ByoeSettingsAccess.HIDDEN -> GmailConnectionSettingsAccess.Hidden
    ByoeSettingsAccess.LIST_ONLY -> GmailConnectionSettingsAccess.ListOnly
    ByoeSettingsAccess.FULL -> GmailConnectionSettingsAccess.Full
}

private fun ByoeGuidanceTreatment.toTreatment() = when (this) {
    ByoeGuidanceTreatment.TREATMENT1 -> ByoeTreatment.Treatment1
    ByoeGuidanceTreatment.TREATMENT2 -> ByoeTreatment.Treatment2
}
