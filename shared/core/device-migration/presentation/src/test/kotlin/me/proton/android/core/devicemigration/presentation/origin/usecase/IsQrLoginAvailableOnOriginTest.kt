package me.proton.android.core.devicemigration.presentation.origin.usecase

import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.biometric.data.StrongAuthenticatorsResolver
import me.proton.core.biometric.domain.CheckBiometricAuthAvailability
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionUserSettingsResult
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsQrLoginAvailableOnOriginTest {

    @MockK
    private lateinit var checkBiometricAuthAvailability: CheckBiometricAuthAvailability

    @MockK
    private lateinit var strongAuthenticatorsResolver: StrongAuthenticatorsResolver

    @MockK
    private lateinit var userSessionRepository: UserSessionRepository

    private lateinit var tested: IsQrLoginAvailableOnOrigin

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        tested = IsQrLoginAvailableOnOrigin(
            checkBiometricAuthAvailability,
            strongAuthenticatorsResolver,
            userSessionRepository
        )
    }

    @Test
    fun `qr login is available`() = runTest {
        // GIVEN
        every { checkBiometricAuthAvailability(any(), any()) } returns CheckBiometricAuthAvailability.Result.Success
        mockUserSettings(edmOptOut = false)

        // WHEN
        val result = tested()

        // THEN
        assertTrue(result)
    }

    @Test
    fun `biometrics not enrolled`() = runTest {
        // GIVEN
        every { checkBiometricAuthAvailability(any(), any()) } returns
            CheckBiometricAuthAvailability.Result.Failure.NotEnrolled
        mockUserSettings(edmOptOut = false)

        // WHEN
        val result = tested()

        // THEN
        assertFalse(result)
    }

    @Test
    fun `user opted out`() = runTest {
        // GIVEN
        every { checkBiometricAuthAvailability(any(), any()) } returns CheckBiometricAuthAvailability.Result.Success
        mockUserSettings(edmOptOut = true)

        // WHEN
        val result = tested()

        // THEN
        assertFalse(result)
    }

    private fun mockUserSettings(edmOptOut: Boolean) {
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserId("user-id"))
        coEvery { userSessionRepository.getUserSession(any()) } returns MailUserSessionWrapper(
            mockk<MailUserSession> {
                coEvery { userSettings() } returns mockk<MailUserSessionUserSettingsResult.Ok> {
                    every { v1.flags.edmOptOut } returns edmOptOut
                }
            }
        )
    }
}
