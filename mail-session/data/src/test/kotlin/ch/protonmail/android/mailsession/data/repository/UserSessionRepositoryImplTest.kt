package ch.protonmail.android.mailsession.data.repository

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalUser
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.user.RustUserDataSource
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.model.CookieSessionId
import ch.protonmail.android.mailsession.domain.model.Fork
import ch.protonmail.android.mailsession.domain.model.Selector
import ch.protonmail.android.mailsession.domain.model.SessionError
import ch.protonmail.android.mailsession.domain.model.User
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.testdata.user.LocalUserTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import me.proton.android.core.account.domain.usecase.ObserveStoredAccounts
import org.junit.Rule
import uniffi.proton_mail_uniffi.Fork as RustFork
import uniffi.proton_mail_uniffi.StoredAccount
import uniffi.proton_mail_uniffi.StoredSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserSessionRepositoryImplTest {

    @get:Rule
    val loggingRule = LoggingTestRule()

    private val mailSessionRepository = mockk<MailSessionRepository>()
    private val observeStoredAccounts = mockk<ObserveStoredAccounts>()
    private val rustUserDataSource = mockk<RustUserDataSource>()

    private val userSessionRepository = UserSessionRepositoryImpl(
        mailSessionRepository,
        rustUserDataSource,
        observeStoredAccounts
    )

    @Test
    fun `getUserSession returns an existing ready user session without reinitialization`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expectedMailUserSession = mockk<MailUserSessionWrapper>()
        val mailSession = mailSessionWithReadyInitializedSession(expectedMailUserSession)

        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val actual = userSessionRepository.getUserSession(userId)

        // Then
        assertEquals(expectedMailUserSession, actual)
        coVerify(exactly = 1) { mailSession.initializedUserContextFromSession(any()) }
        coVerify(exactly = 0) { mailSession.userContextFromSession(any()) } // no fallback needed
    }

    @Test
    fun `getUserSession initializes a user session when no ready session is available`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expectedMailUserSession = mockk<MailUserSessionWrapper>()
        val mailSession = mailSessionWithFallbackToFullInit(expectedMailUserSession)

        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val actual = userSessionRepository.getUserSession(userId)

        // Then
        assertEquals(expectedMailUserSession, actual)
        coVerify(exactly = 1) { mailSession.initializedUserContextFromSession(any()) }
        coVerify(exactly = 1) { mailSession.userContextFromSession(any()) } // fallback used
    }

    @Test
    fun `getUserSession returns null when no stored account exists`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mailSessionWithNoStoredAccount()

        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val actual = userSessionRepository.getUserSession(userId)

        // Then
        assertNull(actual)
        coVerify(exactly = 0) { mailSession.getAccountSessions(any()) }
        coVerify(exactly = 0) { mailSession.initializedUserContextFromSession(any()) }
        coVerify(exactly = 0) { mailSession.userContextFromSession(any()) }
    }

    @Test
    fun `getUserSession returns null when account sessions cannot be retrieved`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mailSessionWithAccountSessionsError()

        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val actual = userSessionRepository.getUserSession(userId)

        // Then
        assertNull(actual)
        coVerify(exactly = 1) { mailSession.getAccountSessions(any()) }
        coVerify(exactly = 0) { mailSession.initializedUserContextFromSession(any()) }
        coVerify(exactly = 0) { mailSession.userContextFromSession(any()) }
    }

    @Test
    fun `getUserSession returns null when there are no active sessions`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mailSessionWithNoAccountSessions()

        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val actual = userSessionRepository.getUserSession(userId)

        // Then
        assertNull(actual)
        coVerify(exactly = 1) { mailSession.getAccountSessions(any()) }
        coVerify(exactly = 0) { mailSession.initializedUserContextFromSession(any()) }
        coVerify(exactly = 0) { mailSession.userContextFromSession(any()) }
    }

    @Test
    fun `fork session should return fork data on success`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expectedFork = RustFork("selector", "forked-session-id")
        val expectedMailUserSession = mockk<MailUserSessionWrapper> {
            coEvery { fork() } returns expectedFork.right()
        }
        val mailSession = mailSessionWithReadyInitializedSession(expectedMailUserSession)
        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val result = userSessionRepository.forkSession(userId)

        // Then
        assert(result.isRight())
        assertEquals(
            Fork(Selector("selector"), CookieSessionId("forked-session-id")),
            result.getOrNull()
        )
        coVerify { expectedMailUserSession.fork() }
    }

    @Test
    fun `forkSession should return SessionError when session is null`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mailSessionWithNoStoredAccount()
        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val result = userSessionRepository.forkSession(userId)

        // Then
        assert(result.isLeft())
        assertEquals(SessionError.Local.Unknown, result.swap().getOrNull())
    }

    @Test
    fun `forkSession should return SessionError when fork operation fails`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expectedMailUserSession = mockk<MailUserSessionWrapper> {
            coEvery { fork() } returns DataError.Local.NoUserSession.left()
        }
        val mailSession = mailSessionWithReadyInitializedSession(expectedMailUserSession)
        coEvery { mailSessionRepository.getMailSession() } returns mailSession

        // When
        val result = userSessionRepository.forkSession(userId)

        // Then
        assertEquals(SessionError.Local.Unknown.left(), result)
        coVerify { expectedMailUserSession.fork() }
    }

    @Test
    fun `observe user returns the user entity and subsequent updates`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expectedUser = User(
            userId = userId,
            displayName = "userDisplayName",
            name = "username",
            email = "userEmail",
            services = 0,
            subscribed = 0,
            delinquent = 0,
            createTimeUtc = 0L,
            usedSpace = 0L,
            maxSpace = 0L
        )
        val expectedMailUserSession = mockk<MailUserSessionWrapper>()
        val mailSession = mailSessionWithReadyInitializedSession(expectedMailUserSession)
        val localUser = LocalUserTestData.build(subscribed = 0, services = 0)
        val updatedLocalUser = LocalUserTestData.build(subscribed = 1, services = 1)
        val flow = MutableSharedFlow<Either<DataError, LocalUser>>()
        coEvery { mailSessionRepository.getMailSession() } returns mailSession
        every { rustUserDataSource.observeUser(expectedMailUserSession) } returns flow

        // When + Then
        userSessionRepository.observeUser(userId).test {
            flow.emit(localUser.right())
            assertEquals(expectedUser.right(), awaitItem())

            flow.emit(updatedLocalUser.right())
            assertEquals(expectedUser.copy(subscribed = 1, services = 1).right(), awaitItem())
        }
    }

    @Test
    fun `observe user returns error when data can't be fetched`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expectedMailUserSession = mockk<MailUserSessionWrapper>()
        val mailSession = mailSessionWithReadyInitializedSession(expectedMailUserSession)
        val expectedError = DataError.Local.CryptoError.left()
        val flow = MutableSharedFlow<Either<DataError, LocalUser>>()
        coEvery { mailSessionRepository.getMailSession() } returns mailSession
        every { rustUserDataSource.observeUser(expectedMailUserSession) } returns flow

        // When + Then
        userSessionRepository.observeUser(userId).test {
            flow.emit(expectedError)
            assertEquals(expectedError, awaitItem())
        }
    }

    private fun mailSessionWithNoStoredAccount() = mockk<MailSessionWrapper> {
        coEvery { getAccount(any()) } returns DataError.Local.NoDataCached.left()
    }

    private fun mailSessionWithAccountSessionsError() = mockk<MailSessionWrapper> {
        val storedAccount = mockk<StoredAccount>()
        coEvery { getAccount(any()) } returns storedAccount.right()
        coEvery { getAccountSessions(storedAccount) } returns DataError.Local.CryptoError.left()
    }

    private fun mailSessionWithNoAccountSessions() = mockk<MailSessionWrapper> {
        val storedAccount = mockk<StoredAccount>()
        coEvery { getAccount(any()) } returns storedAccount.right()
        coEvery { getAccountSessions(storedAccount) } returns emptyList<StoredSession>().right()
    }

    private fun mailSessionWithReadyInitializedSession(expected: MailUserSessionWrapper) = mockk<MailSessionWrapper> {
        val storedAccount = mockk<StoredAccount>()
        val storedSession = mockk<StoredSession>()

        coEvery { getAccount(any()) } returns storedAccount.right()
        coEvery { getAccountSessions(storedAccount) } returns listOf(storedSession).right()

        coEvery { initializedUserContextFromSession(storedSession) } returns expected.right()
        // should NOT be called in this path, but provide a default to avoid accidental crashes
        coEvery { userContextFromSession(storedSession) } returns expected.right()
    }

    private fun mailSessionWithFallbackToFullInit(expected: MailUserSessionWrapper) = mockk<MailSessionWrapper> {
        val storedAccount = mockk<StoredAccount>()
        val storedSession = mockk<StoredSession>()

        coEvery { getAccount(any()) } returns storedAccount.right()
        coEvery { getAccountSessions(storedAccount) } returns listOf(storedSession).right()

        coEvery { initializedUserContextFromSession(storedSession) } returns (null as MailUserSessionWrapper?).right()
        coEvery { userContextFromSession(storedSession) } returns expected.right()
    }
}
