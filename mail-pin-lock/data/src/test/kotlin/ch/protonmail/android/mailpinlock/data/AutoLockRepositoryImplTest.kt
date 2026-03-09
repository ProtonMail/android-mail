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

package ch.protonmail.android.mailpinlock.data

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalAppSettings
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailcommon.domain.model.autolock.SetAutoLockPinError
import ch.protonmail.android.mailcommon.domain.model.autolock.VerifyAutoLockPinError
import ch.protonmail.android.mailpinlock.domain.BiometricsSystemStateRepository
import ch.protonmail.android.mailpinlock.model.AutoLockBiometricsState
import ch.protonmail.android.mailpinlock.model.AutoLockInterval
import ch.protonmail.android.mailpinlock.model.BiometricsSystemState
import ch.protonmail.android.mailpinlock.model.Protection
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsettings.data.local.RustAppSettingsDataSource
import ch.protonmail.android.mailsettings.data.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.MobileSignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SwipeNextPreference
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.domain.repository.MobileSignatureRepository
import ch.protonmail.android.mailsettings.domain.repository.SwipeNextRepository
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.AppAppearance
import uniffi.mail_uniffi.AppProtection
import uniffi.mail_uniffi.AutoLock
import uniffi.mail_uniffi.MailSession
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoLockRepositoryImplTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val mockMailSession = mockk<MailSession>()
    private val mockMailSessionWrapper = mockk<MailSessionWrapper> {
        every { this@mockk.getRustMailSession() } returns mockMailSession
    }
    private val appSettingsDataSource = mockk<RustAppSettingsDataSource> {
        coEvery { this@mockk.updateAppSettings(mockMailSessionWrapper, any()) } returns Unit.right()
    }
    private val appLockDataSource = mockk<AppLockDataSource>()
    private val appLanguageRepository = mockk<AppLanguageRepository> {
        every { this@mockk.observe() } returns flowOf(AppLanguage.FRENCH)
    }

    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { this@mockk.getMailSession() } returns mockMailSessionWrapper
    }

    private val expectedBiometrics = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled
    private val biometricsStateRepository = mockk<BiometricsSystemStateRepository> {
        every { this@mockk.observe() } returns flowOf(BiometricsSystemState.BiometricEnrolled)
    }
    private val userId = UserId("user-123")

    private val userSessionRepository = mockk<UserSessionRepository> {
        every { observePrimaryUserId() } returns flowOf(userId)
    }
    private val mobileSignatureRepository = mockk<MobileSignatureRepository> {
        every { observeMobileSignature(userId) } returns flowOf(MobileSignaturePreference.Empty)
    }

    private val swipeNextRepository = mockk<SwipeNextRepository> {
        coEvery { observeSwipeNext(userId) } returns flowOf(SwipeNextPreference.NotEnabled.right())
    }


    private val appSettingsRepository: AppSettingsRepository = spyk(
        AppSettingsRepository(
            mailSessionRepository = mailSessionRepository,
            userSessionRepository = userSessionRepository,
            rustAppSettingsDataSource = appSettingsDataSource,
            appLanguageRepository = appLanguageRepository,
            mobileSignatureRepository = mobileSignatureRepository,
            swipeNextRepository = swipeNextRepository
        )
    )

    private val autoLockRepositoryImpl: AutoLockRepositoryImpl =
        AutoLockRepositoryImpl(
            biometricsSystemStateRepository = biometricsStateRepository,
            appSettingsRepository = appSettingsRepository,
            mailSessionRepository = mailSessionRepository,
            appLockDataSource = appLockDataSource
        )

    private val mockAppSettings = LocalAppSettings(
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

    @Test
    fun `returns protection when observed`() = runTest {
        // Given
        coEvery {
            appSettingsDataSource.getAppSettings(any())
        } returns mockAppSettings.right()
        // When
        autoLockRepositoryImpl.observeAppLock().test {
            // Then
            assertEquals(expectedAppSettings.autolockProtection, awaitItem().protectionType)
        }
    }

    @Test
    fun `returns interval when observed`() = runTest {
        // Given
        coEvery {
            appSettingsDataSource.getAppSettings(any())
        } returns mockAppSettings.right()
        // When
        autoLockRepositoryImpl.observeAppLock().test {
            // Then
            assertEquals(expectedAppSettings.autolockInterval, awaitItem().autolockInterval)
        }
    }

    @Test
    fun `returns biometrics when observed`() = runTest {
        // Given
        coEvery { appLockDataSource.shouldAutoLock(mockMailSession) } returns false.right()
        coEvery {
            appSettingsDataSource.getAppSettings(any())
        } returns mockAppSettings.right()
        // When
        autoLockRepositoryImpl.observeAppLock().test {
            // Then
            val item = awaitItem()
            assertEquals(expectedBiometrics, item.biometricsState)
        }
    }

    @Test
    fun `when interval is updated then observer is also updated`() = runTest {
        val expectedUpdatedInterval = AutoLockInterval.FifteenMinutes
        val updatedAppSettings = mockAppSettings.copy(autoLock = AutoLock.Minutes(15L.toUByte()))
        // Given
        coEvery {
            appSettingsDataSource.getAppSettings(mockMailSessionWrapper)
        } returns mockAppSettings.right() andThen updatedAppSettings.right()
        // When

        autoLockRepositoryImpl.observeAppLock().test {
            // Then
            assertEquals(expectedAppSettings.autolockInterval, awaitItem().autolockInterval)

            autoLockRepositoryImpl.updateAutoLockInterval(expectedUpdatedInterval)

            assertEquals(expectedUpdatedInterval, awaitItem().autolockInterval)
        }
    }

    @Test
    fun `when shouldAutoLock then return result`() = runTest {
        // Given
        coEvery { appLockDataSource.shouldAutoLock(mockMailSession) } returns true.right()

        // When
        val result = autoLockRepositoryImpl.shouldAutoLock()

        // Then
        assert(result.isRight())
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `when shouldAutoLock is false the return result`() = runTest {
        // Given
        coEvery { appLockDataSource.shouldAutoLock(mockMailSession) } returns false.right()

        // When
        val result = autoLockRepositoryImpl.shouldAutoLock()

        // Then
        assert(result.isRight())
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `when shouldAutoLock fails then return error`() = runTest {
        // Given
        coEvery { appLockDataSource.shouldAutoLock(mockMailSession) } returns DataError.Local.CryptoError.left()

        // When
        val result = autoLockRepositoryImpl.shouldAutoLock()

        // Then
        assert(result.isLeft())
    }

    @Test
    fun `when biometric passes, then the mail session is notified`() = runTest {
        // Given
        coEvery { mockMailSessionWrapper.signalBiometricsCheckPassed() } just runs

        // When
        autoLockRepositoryImpl.signalBiometricsCheckPassed()

        // Then
        coVerify { mockMailSessionWrapper.signalBiometricsCheckPassed() }
        confirmVerified(mockMailSessionWrapper)
    }

    @Test
    fun `when setting auto lock, call is proxied to mail session and settings are refreshed on success`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("1234")
        val localAutoLockPin = listOf(1u, 2u, 3u, 4u)
        coEvery { mockMailSessionWrapper.setAutoLockPinCode(localAutoLockPin) } returns Unit.right()

        // When
        val result = autoLockRepositoryImpl.setAutoLockPinCode(autoLockPin)

        // Then
        assertTrue { result.isRight() }
        coVerify(exactly = 1) { mockMailSessionWrapper.setAutoLockPinCode(localAutoLockPin) }
        coVerify(exactly = 1) { appSettingsRepository.refreshSettings() }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when setting auto lock, call is proxied to mailsession and with no settings refresh on failure`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("1234")
        val localAutoLockPin = listOf(1u, 2u, 3u, 4u)

        val expectedError = SetAutoLockPinError.PinIsMalformed.left()
        coEvery { mockMailSessionWrapper.setAutoLockPinCode(localAutoLockPin) } returns expectedError

        // When
        val result = autoLockRepositoryImpl.setAutoLockPinCode(autoLockPin)

        // Then
        assertEquals(result, expectedError)
        coVerify(exactly = 1) { mockMailSessionWrapper.setAutoLockPinCode(localAutoLockPin) }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when verifying auto lock, call is proxied to mail session and settings are refreshed on success`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("1234")
        val localAutoLockPin = listOf(1u, 2u, 3u, 4u)
        coEvery { mockMailSessionWrapper.verifyPinCode(localAutoLockPin) } returns Unit.right()

        // When
        val result = autoLockRepositoryImpl.verifyAutoLockPinCode(autoLockPin)

        // Then
        assertTrue { result.isRight() }
        coVerify(exactly = 1) { mockMailSessionWrapper.verifyPinCode(localAutoLockPin) }
        coVerify(exactly = 1) { appSettingsRepository.refreshSettings() }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when verifying auto lock, call is proxied to mailsession and with no settings refresh on failure`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("1234")
        val localAutoLockPin = listOf(1u, 2u, 3u, 4u)

        val expectedError = VerifyAutoLockPinError.IncorrectPin.left()
        coEvery { mockMailSessionWrapper.verifyPinCode(localAutoLockPin) } returns expectedError

        // When
        val result = autoLockRepositoryImpl.verifyAutoLockPinCode(autoLockPin)

        // Then
        assertEquals(result, expectedError)
        coVerify(exactly = 1) { mockMailSessionWrapper.verifyPinCode(localAutoLockPin) }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when deleting auto lock, call is proxied to mail session and settings are refreshed on success`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("1234")
        val localAutoLockPin = listOf(1u, 2u, 3u, 4u)
        coEvery { mockMailSessionWrapper.deleteAutoLockPinCode(localAutoLockPin) } returns Unit.right()

        // When
        val result = autoLockRepositoryImpl.deleteAutoLockPinCode(autoLockPin)

        // Then
        assertTrue { result.isRight() }
        coVerify(exactly = 1) { mockMailSessionWrapper.deleteAutoLockPinCode(localAutoLockPin) }
        coVerify(exactly = 1) { appSettingsRepository.refreshSettings() }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when deleting auto lock, call is proxied to mailsession and with no settings refresh on failure`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("1234")
        val localAutoLockPin = listOf(1u, 2u, 3u, 4u)

        val expectedError = VerifyAutoLockPinError.IncorrectPin.left()
        coEvery { mockMailSessionWrapper.deleteAutoLockPinCode(localAutoLockPin) } returns expectedError

        // When
        val result = autoLockRepositoryImpl.deleteAutoLockPinCode(autoLockPin)

        // Then
        assertEquals(result, expectedError)
        coVerify(exactly = 1) { mockMailSessionWrapper.deleteAutoLockPinCode(localAutoLockPin) }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when setting biometric protection, call is proxied correctly to the mailsession (success)`() = runTest {
        // Given
        val expected = Unit.right()
        coEvery { mockMailSessionWrapper.setBiometricAppProtection() } returns expected

        // When
        val result = autoLockRepositoryImpl.setBiometricProtection(true)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { mockMailSessionWrapper.setBiometricAppProtection() }
        coVerify(exactly = 1) { appSettingsRepository.refreshSettings() }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when setting biometric protection, call is proxied correctly to the mailsession (failure)`() = runTest {
        // Given
        val expected = DataError.Local.CryptoError.left()
        coEvery { mockMailSessionWrapper.setBiometricAppProtection() } returns expected

        // When
        val result = autoLockRepositoryImpl.setBiometricProtection(true)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { mockMailSessionWrapper.setBiometricAppProtection() }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }

    @Test
    fun `when removing biometric protection, call is proxied correctly to the mailsession (success)`() = runTest {
        // Given
        val expected = DataError.Local.CryptoError.left()
        coEvery { mockMailSessionWrapper.unsetBiometricAppProtection() } returns expected

        // When
        val result = autoLockRepositoryImpl.setBiometricProtection(false)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { mockMailSessionWrapper.unsetBiometricAppProtection() }
        confirmVerified(mockMailSessionWrapper, appSettingsRepository)
    }


    @Test
    fun `when querying for remaining attempts succeeds, the data is propagated`() = runTest {
        // Given
        val expected = 1u.right()
        coEvery { mockMailSessionWrapper.getRemainingAttempts() } returns expected

        // When
        val result = autoLockRepositoryImpl.getRemainingAttempts()

        // Then
        assertEquals(expected.getOrNull()?.toInt(), result.getOrNull())
        coVerify(exactly = 1) { mockMailSessionWrapper.getRemainingAttempts() }
        confirmVerified(mockMailSessionWrapper)
    }

    @Test
    fun `when querying for remaining attempts errors, the error is propagated`() = runTest {
        // Given
        val expected = DataError.Local.CryptoError.left()
        coEvery { mockMailSessionWrapper.getRemainingAttempts() } returns expected

        // When
        val result = autoLockRepositoryImpl.getRemainingAttempts()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { mockMailSessionWrapper.getRemainingAttempts() }
        confirmVerified(mockMailSessionWrapper)
    }
}
