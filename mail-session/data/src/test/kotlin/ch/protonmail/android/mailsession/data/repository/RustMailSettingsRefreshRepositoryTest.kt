package ch.protonmail.android.mailsession.data.repository

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class RustMailSettingsRefreshRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userSessionRepository = mockk<UserSessionRepository>()

    private val mailSettingsRefreshRepository = RustMailSettingsRefreshRepository(
        userSessionRepository
    )

    @Test
    fun `refreshes mail settings for the given user's session`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val mailSession = mockk<MailUserSessionWrapper>(relaxUnitFun = true) {
            coEvery { this@mockk.refreshMailSettings() } returns Unit.right()
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession

        // When
        mailSettingsRefreshRepository.refresh(userId)

        // Then
        coVerify(exactly = 1) { mailSession.refreshMailSettings() }
    }

    @Test
    fun `does not refresh mail settings when no session is found for the given user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        mailSettingsRefreshRepository.refresh(userId)

        // Then
        coVerify(exactly = 1) { userSessionRepository.getUserSession(userId) }
    }
}
