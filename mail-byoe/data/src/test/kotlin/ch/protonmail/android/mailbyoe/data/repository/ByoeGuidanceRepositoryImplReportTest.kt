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

import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerCta
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerOrigin
import ch.protonmail.android.mailbyoe.domain.model.ByoeConnectErrorType
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportMode
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.ByoeUpsellCta
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uniffi.mail_uniffi.ByoeGuidance
import uniffi.mail_uniffi.MailUserSession
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.mail_uniffi.ByoeBannerCta as SdkBannerCta
import uniffi.mail_uniffi.ByoeBannerOrigin as SdkBannerOrigin
import uniffi.mail_uniffi.ByoeConnectErrorType as SdkConnectErrorType
import uniffi.mail_uniffi.ByoeDismissMethod as SdkDismissMethod
import uniffi.mail_uniffi.ByoeImportMode as SdkImportMode
import uniffi.mail_uniffi.ByoeSurface as SdkSurface
import uniffi.mail_uniffi.ByoeUpsellCta as SdkUpsellCta

@RunWith(Parameterized::class)
internal class ByoeGuidanceRepositoryImplReportTest(
    private val event: ByoeTelemetryEvent,
    private val expectedCall: suspend ByoeGuidance.() -> Unit
) {

    private val userId = UserId("user")
    private val byoeGuidance = mockk<ByoeGuidance>(relaxUnitFun = true) {
        every { close() } just runs
    }
    private val rustSession = mockk<MailUserSession> {
        every { byoeGuidance() } returns byoeGuidance
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns mockk<MailUserSessionWrapper> {
            every { getRustUserSession() } returns rustSession
        }
    }

    private val repository = ByoeGuidanceRepositoryImpl(
        ExecuteWithUserSession(userSessionRepository, UnconfinedTestDispatcher())
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an event, when reporting it, then the matching SDK call reports it and the guidance is released`() =
        runTest {
            // When
            val result = repository.report(userId, event)

            // Then
            assertEquals(Unit.right(), result)
            coVerify(exactly = 1) { byoeGuidance.expectedCall() }
            verify(exactly = 1) { byoeGuidance.close() }
        }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> = listOf(
            case(ByoeTelemetryEvent.SidebarBannerShown) { reportSidebarBannerShown() },
            case(ByoeTelemetryEvent.OnboardingDrawerShown) { reportOnboardingDrawerShown() },
            case(ByoeTelemetryEvent.OnboardingDrawerDismissed(ByoeDismissMethod.TapOutside)) {
                reportOnboardingDrawerDismissed(SdkDismissMethod.TAP_OUTSIDE)
            },
            case(ByoeTelemetryEvent.SettingsShown) { reportSettingsShown() },
            case(ByoeTelemetryEvent.SettingsView) { reportSettingsView() },
            case(ByoeTelemetryEvent.ConnectStarted(ByoeSurface.OnboardingScreen, ByoeImportMode.Enabled)) {
                reportConnectStarted(SdkSurface.ONBOARDING_SCREEN, SdkImportMode.ENABLED)
            },
            case(ByoeTelemetryEvent.ConnectStarted(ByoeSurface.Settings, ByoeImportMode.Disabled)) {
                reportConnectStarted(SdkSurface.SETTINGS, SdkImportMode.DISABLED)
            },
            case(ByoeTelemetryEvent.ConnectSucceeded(ByoeSurface.Spotlight)) {
                reportConnectSucceeded(SdkSurface.SPOTLIGHT)
            },
            case(ByoeTelemetryEvent.ConnectAbandoned(ByoeSurface.SidebarBanner)) {
                reportConnectAbandoned(SdkSurface.SIDEBAR_BANNER)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.OnboardingDrawer, ByoeConnectErrorType.NoAccess)) {
                reportConnectFailed(SdkSurface.ONBOARDING_DRAWER, SdkConnectErrorType.NO_ACCESS)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.Settings, ByoeConnectErrorType.AlreadyAdded)) {
                reportConnectFailed(SdkSurface.SETTINGS, SdkConnectErrorType.ALREADY_ADDED)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.Settings, ByoeConnectErrorType.CreateFailed)) {
                reportConnectFailed(SdkSurface.SETTINGS, SdkConnectErrorType.CREATE_FAILED)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.Settings, ByoeConnectErrorType.ConvertFailed)) {
                reportConnectFailed(SdkSurface.SETTINGS, SdkConnectErrorType.CONVERT_FAILED)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.Settings, ByoeConnectErrorType.WrongAccount)) {
                reportConnectFailed(SdkSurface.SETTINGS, SdkConnectErrorType.WRONG_ACCOUNT)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.Settings, ByoeConnectErrorType.TokenFailed)) {
                reportConnectFailed(SdkSurface.SETTINGS, SdkConnectErrorType.TOKEN_FAILED)
            },
            case(ByoeTelemetryEvent.ConnectFailed(ByoeSurface.Settings, ByoeConnectErrorType.Unknown)) {
                reportConnectFailed(SdkSurface.SETTINGS, SdkConnectErrorType.UNKNOWN)
            },
            case(ByoeTelemetryEvent.ExploreClicked(ByoeSurface.OnboardingScreen)) {
                reportExploreClicked(SdkSurface.ONBOARDING_SCREEN)
            },
            case(ByoeTelemetryEvent.InboxViewed) { reportInboxViewed() },
            case(ByoeTelemetryEvent.UpsellWallShown) { reportUpsellWallShown() },
            case(ByoeTelemetryEvent.UpgradeBannerShown(ByoeBannerOrigin.InboxStorageLimit)) {
                reportUpgradeBannerShown(SdkBannerOrigin.INBOX_STORAGE_LIMIT)
            },
            case(ByoeTelemetryEvent.UpgradeBannerShown(ByoeBannerOrigin.Settings)) {
                reportUpgradeBannerShown(SdkBannerOrigin.SETTINGS)
            },
            case(
                ByoeTelemetryEvent.UpgradeBannerCtaClicked(ByoeBannerOrigin.InboxStorageLimit, ByoeBannerCta.Upgrade)
            ) {
                reportUpgradeBannerCtaClicked(SdkBannerOrigin.INBOX_STORAGE_LIMIT, SdkBannerCta.UPGRADE)
            },
            case(ByoeTelemetryEvent.UpgradeBannerCtaClicked(ByoeBannerOrigin.InboxStorageLimit, ByoeBannerCta.NotNow)) {
                reportUpgradeBannerCtaClicked(SdkBannerOrigin.INBOX_STORAGE_LIMIT, SdkBannerCta.NOT_NOW)
            },
            case(ByoeTelemetryEvent.UpsellScreenShown) { reportUpsellScreenShown() },
            case(ByoeTelemetryEvent.UpsellCtaClicked(ByoeUpsellCta.Primary)) {
                reportUpsellCtaClicked(SdkUpsellCta.PRIMARY)
            },
            case(ByoeTelemetryEvent.UpsellCtaClicked(ByoeUpsellCta.Secondary)) {
                reportUpsellCtaClicked(SdkUpsellCta.SECONDARY)
            },
            case(ByoeTelemetryEvent.UpsellSucceeded) { reportUpsellSucceeded() },
            case(ByoeTelemetryEvent.UpsellFailed("upgrade_error")) { reportUpsellFailed("upgrade_error") }
        )

        private fun case(event: ByoeTelemetryEvent, expectedCall: suspend ByoeGuidance.() -> Unit) =
            arrayOf<Any>(event, expectedCall)
    }
}
