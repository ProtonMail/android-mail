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

package ch.protonmail.android.mailsettings.data.repository

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailpinlock.model.AutoLockInterval
import ch.protonmail.android.mailpinlock.model.Protection
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsettings.data.local.RustAppSettingsDataSource
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.MobileSignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SwipeNextPreference
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.domain.repository.MobileSignatureRepository
import ch.protonmail.android.mailsettings.domain.repository.SwipeNextRepository
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.AppAppearance
import uniffi.mail_uniffi.AppProtection
import uniffi.mail_uniffi.AutoLock

internal class AppSettingsRepositoryTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val appLanguageRepository = mockk<AppLanguageRepository> {
        every { this@mockk.observe() } returns flowOf(AppLanguage.FRENCH)
    }

    private lateinit var appSettingsRepository: AppSettingsRepository

    private val mockAppSettings = uniffi.mail_uniffi.AppSettings(
        AppAppearance.LIGHT_MODE,
        AppProtection.PIN,
        AutoLock.Always,
        useCombineContacts = true,
        useAlternativeRouting = true
    )

    private val expectedAppSettings = AppSettings(
        autolockInterval = AutoLockInterval.Immediately,
        autolockProtection = Protection.Pin,
        hasAlternativeRouting = true,
        customAppLanguage = AppLanguage.FRENCH.langName,
        hasCombinedContactsEnabled = true,
        theme = Theme.LIGHT,
        mobileSignaturePreference = MobileSignaturePreference.Empty,
        swipeNextPreference = SwipeNextPreference.NotEnabled
    )

    private val userId = UserId("user-123")

    private val userSessionRepository = mockk<UserSessionRepository> {
        every { observePrimaryUserId() } returns flowOf(userId)
    }
    private val mobileSignatureRepository = mockk<MobileSignatureRepository> {
        every { observeMobileSignature(userId) } returns flowOf(MobileSignaturePreference.Empty)
    }

    private val swipeNextRepository = mockk<SwipeNextRepository> {
        coEvery { this@mockk.getSwipeNext(userId) } returns SwipeNextPreference.NotEnabled.right()
        coEvery { this@mockk.observeSwipeNext(userId) } returns flowOf(SwipeNextPreference.NotEnabled.right())
    }

    private val mockMailSessionWrapper = mockk<MailSessionWrapper>()

    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { this@mockk.getMailSession() } returns mockMailSessionWrapper
    }

    private val appSettingsDataSource = mockk<RustAppSettingsDataSource> {
        coEvery { this@mockk.updateAppSettings(mockMailSessionWrapper, any()) } returns Unit.right()
        coEvery { this@mockk.getAppSettings(mockMailSessionWrapper) } returns mockAppSettings.right()
    }

    @Before
    fun setUp() {
        appSettingsRepository = AppSettingsRepository(
            mailSessionRepository = mailSessionRepository,
            userSessionRepository = userSessionRepository,
            rustAppSettingsDataSource = appSettingsDataSource,
            appLanguageRepository = appLanguageRepository,
            mobileSignatureRepository = mobileSignatureRepository,
            swipeNextRepository = swipeNextRepository
        )
    }


    @Test
    fun `returns theme when observed`() = runTest {
        // Given
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right()
        // When
        appSettingsRepository.observeTheme().test {
            // Then
            assertEquals(expectedAppSettings.theme, awaitItem())
        }
    }

    @Test
    fun `returns language when observed`() = runTest {
        // Given
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right()
        // When
        appSettingsRepository.observeAppSettings().test {
            // Then
            assertEquals(expectedAppSettings.customAppLanguage, awaitItem().customAppLanguage)
        }
    }

    @Test
    fun `returns swipe preference when observed`() = runTest {
        // Given
        val expectedSwipeNextPreference = SwipeNextPreference.Enabled
        coEvery { swipeNextRepository.observeSwipeNext(userId) } returns flowOf(expectedSwipeNextPreference.right())
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right()
        // When
        appSettingsRepository.observeAppSettings().test {
            // Then
            assertEquals(expectedSwipeNextPreference, awaitItem().swipeNextPreference)
        }
    }

    @Test
    fun `when theme is updated then theme observer is also updated`() = runTest {
        // Given
        val expectedInitialTheme = Theme.LIGHT
        val updatedAppSettings = mockAppSettings.copy(AppAppearance.DARK_MODE)
        val expectedUpdatedTheme = Theme.DARK
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right() andThen updatedAppSettings.right()
        // When
        appSettingsRepository.observeTheme().test {
            assertEquals(expectedInitialTheme, awaitItem())

            appSettingsRepository.updateTheme(expectedUpdatedTheme)

            assertEquals(expectedUpdatedTheme, awaitItem())
        }
    }


    @Test
    fun `when theme is updated then appSettings observer is also updated`() = runTest {
        // Given
        val expectedInitialAppSettings = expectedAppSettings
        val expectedUpdatedTheme = Theme.DARK
        val expectedUpdatedAppSettings = expectedAppSettings.copy(theme = expectedUpdatedTheme)

        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right() andThen mockAppSettings.copy(AppAppearance.DARK_MODE).right()
        // When
        appSettingsRepository.observeAppSettings().test {
            assertEquals(expectedInitialAppSettings, awaitItem())

            appSettingsRepository.updateTheme(expectedUpdatedTheme)

            assertEquals(expectedUpdatedAppSettings, awaitItem())
        }
    }

    @Test
    fun `when interval is updated then appSettings observer is also updated`() = runTest {
        // Given
        val expectedInitialAppSettings = expectedAppSettings
        val expectedUpdatedInterval = AutoLockInterval.FifteenMinutes
        val expectedUpdatedAppSettings = expectedAppSettings.copy(autolockInterval = expectedUpdatedInterval)
        val minutes = 15L
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right() andThen mockAppSettings.copy(autoLock = AutoLock.Minutes(minutes.toUByte()))
            .right()
        // When
        appSettingsRepository.observeAppSettings().test {
            assertEquals(expectedInitialAppSettings, awaitItem())

            appSettingsRepository.updateInterval(interval = expectedUpdatedInterval)

            assertEquals(expectedUpdatedAppSettings, awaitItem())
        }
    }

    @Test
    fun `when error retrieving settings then return default settings and log error`() = runTest {
        // Given
        val error = DataError.Local.CryptoError
        val expectedSettings = AppSettings.default()
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns error.left()
        // When
        appSettingsRepository.observeAppSettings().test {
            assertEquals(expectedSettings, awaitItem())
            loggingTestRule.assertErrorLogged(
                "Unable to get app settings $error, returning default settings: $expectedSettings"
            )
        }
    }

    @Test
    fun `when alternativeRouting is updated then appSettings observer is also updated`() = runTest {
        // Given
        val expectedInitialAppSettings = expectedAppSettings
        val expectedUpdatedRouting = false
        val expectedUpdatedAppSettings = expectedAppSettings.copy(hasAlternativeRouting = false)

        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right() andThen
            mockAppSettings.copy(useAlternativeRouting = expectedUpdatedAppSettings.hasAlternativeRouting)
                .right()
        // When
        appSettingsRepository.observeAppSettings().test {
            assertEquals(expectedInitialAppSettings, awaitItem())

            appSettingsRepository.updateAlternativeRouting(expectedUpdatedRouting)

            assertEquals(expectedUpdatedAppSettings, awaitItem())
        }
    }

    @Test
    fun `when useDeviceContacts is updated then appSettings observer is also updated`() = runTest {
        // Given
        val expectedInitialAppSettings = expectedAppSettings
        val expectedUpdatedDeviceContacts = false
        val expectedUpdatedAppSettings = expectedAppSettings.copy(hasCombinedContactsEnabled = false)

        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right() andThen
            mockAppSettings.copy(useCombineContacts = expectedUpdatedAppSettings.hasCombinedContactsEnabled)
                .right()
        // When
        appSettingsRepository.observeAppSettings().test {
            assertEquals(expectedInitialAppSettings, awaitItem())

            appSettingsRepository.updateUseCombineContacts(expectedUpdatedDeviceContacts)

            assertEquals(expectedUpdatedAppSettings, awaitItem())
        }
    }
}
