package ch.protonmail.android.mailsettings.data.local

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.mailsettings.data.usecase.CreateRustUserMailSettings
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.mailsettings.rust.LocalMailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.NoHandle
import uniffi.mail_uniffi.SettingsWatcher
import uniffi.mail_uniffi.WatchHandle
import kotlin.test.assertEquals

class RustMailSettingsDataSourceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val createRustMailSettings = mockk<CreateRustUserMailSettings>()

    private val mailSettingsDataSource = RustMailSettingsDataSource(
        userSessionRepository,
        createRustMailSettings
    )

    @Test
    fun `observe mail settings fails and logs error when session is invalid`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            coEvery { userSessionRepository.getUserSession(userId) } returns null

            // When
            mailSettingsDataSource.observeMailSettings(userId).test {
                // Then
                loggingTestRule.assertErrorLogged("rust-settings: trying to load settings with a null session")
            }
        }

    @Test
    fun `observe mail settings emits items when updated by the rust library`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = LocalMailSettingsTestData.mailSettings
        val expectedUpdated = LocalMailSettingsTestData.mailSettings.copy(displayName = "updated display name")
        val mailSettingsCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock

        val mockWatchHandle = object : WatchHandle(NoHandle) {
            override fun disconnect() {
                // NOP
            }
        }
        val watcherMock = SettingsWatcher(
            settings = expected,
            watchHandle = mockWatchHandle
        )

        val watcherMockUpdated = watcherMock.copy(settings = expectedUpdated)

        // first value, then updated value
        coEvery {
            createRustMailSettings(userSessionMock, capture(mailSettingsCallbackSlot))
        } returns watcherMock.right() andThen watcherMockUpdated.right()

        mailSettingsDataSource.observeMailSettings(userId).test {
            // Given
            assertEquals(expected, awaitItem()) // Initial value
            // When
            mailSettingsCallbackSlot.captured.onUpdate()
            // Then Updated value
            assertEquals(expectedUpdated, awaitItem())
        }
    }

    @Test
    fun `observe mail settings emits initial value from the rust library`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = LocalMailSettingsTestData.mailSettings
        val mailSettingsCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val watcherMock = mockk<SettingsWatcher> {
            every { settings } returns expected
        }
        coEvery {
            createRustMailSettings(userSessionMock, capture(mailSettingsCallbackSlot))
        } returns watcherMock.right()

        mailSettingsDataSource.observeMailSettings(userId).test {

            // Then
            assertEquals(expected, awaitItem())
        }
    }


}
