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

package ch.protonmail.android.mailbyoe.presentation.connect

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeConnectErrorType
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportMode
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.GetGmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.RefreshAfterGmailConnection
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionAction
import ch.protonmail.android.mailbyoe.presentation.navigation.ByoeSurfaceArgument
import ch.protonmail.android.mailcommon.domain.model.ApiEnvironment
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.SelectMailLabelId
import ch.protonmail.android.mailsession.domain.model.CookieSessionId
import ch.protonmail.android.mailsession.domain.model.ForkWithKeyPayload
import ch.protonmail.android.mailsession.domain.model.Selector
import ch.protonmail.android.mailsession.domain.model.SessionError
import ch.protonmail.android.mailsession.domain.usecase.ForkSessionWithKeyPayload
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule

internal class GmailConnectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val forkSessionWithKeyPayload = mockk<ForkSessionWithKeyPayload> {
        coEvery { this@mockk.invoke(userId) } returns fork.right()
    }
    private val refreshAfterGmailConnection = mockk<RefreshAfterGmailConnection>(relaxUnitFun = true)
    private val findLocalSystemLabelId = mockk<FindLocalSystemLabelId> {
        coEvery { this@mockk.invoke(userId, SystemLabelId.Inbox) } returns inbox
    }
    private val selectMailLabelId = mockk<SelectMailLabelId>(relaxUnitFun = true)
    private val reportByoeTelemetryEvent = mockk<ReportByoeTelemetryEvent>(relaxUnitFun = true)
    private val upsellingVisibility = MutableStateFlow<UpsellingVisibility>(UpsellingVisibility.Normal.MailPlus)
    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility> {
        every { this@mockk(UpsellingEntryPoint.Feature.ByoeConnectionLimit) } returns upsellingVisibility
    }

    private val getGmailConnectionSettingsAccess = mockk<GetGmailConnectionSettingsAccess> {
        coEvery { this@mockk(any()) } returns GmailConnectionSettingsAccess.Full
    }

    private fun viewModel(
        importEmails: String? = "true",
        surface: ByoeSurface = SURFACE,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            mapOf(
                GmailConnectionScreen.IMPORT_EMAILS_KEY to importEmails,
                GmailConnectionScreen.SURFACE_KEY to ByoeSurfaceArgument.encode(surface)
            )
        )
    ) = GmailConnectionViewModel(
        observePrimaryUserId,
        forkSessionWithKeyPayload,
        refreshAfterGmailConnection,
        reportByoeTelemetryEvent,
        findLocalSystemLabelId,
        selectMailLabelId,
        ApiEnvironment(host = "proton.me", apiPrefix = "mail-api"),
        GmailConnectionReducer(),
        observeUpsellingVisibility,
        getGmailConnectionSettingsAccess,
        savedStateHandle
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the session is forked, when opened, then the connection page is opened`() = runTest {
        // When
        val viewModel = viewModel()

        // Then
        viewModel.state.test {
            val state = assertIs<GmailConnectionState.OpeningPage>(awaitItem())
            val url = state.openPage.consume().orEmpty()
            assertTrue(url.startsWith("https://account.proton.me/lite?action=byoe-mobile"))
            assertTrue(url.contains("&autoconnect=1&importEmails=1&"))
            assertTrue(url.endsWith("#selector=selector&sk=key&pv=3&pt=default"))
        }
    }

    @Test
    fun `given the user turned off the import, when opened, then the page connects without importing`() = runTest {
        // When
        val viewModel = viewModel(importEmails = "false")

        // Then
        viewModel.state.test {
            val state = assertIs<GmailConnectionState.OpeningPage>(awaitItem())
            assertTrue(state.openPage.consume().orEmpty().contains("&autoconnect=1&importEmails=0&"))
        }
    }

    @Test
    fun `given it was opened from settings, when opened, then the page shows the gmail connection settings`() =
        runTest {
            // When
            val viewModel = viewModel(surface = ByoeSurface.Settings)

            // Then
            viewModel.state.test {
                val state = assertIs<GmailConnectionState.OpeningPage>(awaitItem())
                val url = state.openPage.consume().orEmpty()
                assertTrue(url.startsWith("https://account.proton.me/lite?action=byoe-mobile"))
                assertFalse(url.contains("autoconnect"))
                assertFalse(url.contains("importEmails"))
            }
        }

    @Test
    fun `given it was opened from the deep link, when opened, then the page shows the gmail connection settings`() =
        runTest {
            // When
            val viewModel = viewModel(surface = ByoeSurface.DeepLink)

            // Then
            viewModel.state.test {
                val state = assertIs<GmailConnectionState.OpeningPage>(awaitItem())
                val url = state.openPage.consume().orEmpty()
                assertTrue(url.startsWith("https://account.proton.me/lite?action=byoe-mobile"))
                assertFalse(url.contains("autoconnect"))
            }
            verify {
                reportByoeTelemetryEvent(
                    ByoeTelemetryEvent.ConnectStarted(ByoeSurface.DeepLink, ByoeImportMode.Unknown)
                )
            }
        }

    @Test
    fun `given the user has no gmail connection settings, when opened from the deep link, then it closes`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(any()) } returns GmailConnectionSettingsAccess.ListOnly

        // When
        val viewModel = viewModel(surface = ByoeSurface.DeepLink)

        // Then
        viewModel.state.test {
            assertIs<GmailConnectionState.Finished>(awaitItem())
        }
        coVerify(exactly = 0) { forkSessionWithKeyPayload(any()) }
    }

    @Test
    fun `given the fork fails, when opened, then the error is shown`() = runTest {
        // Given
        coEvery { forkSessionWithKeyPayload(userId) } returns SessionError.Local.Unknown.left()

        // When
        val viewModel = viewModel()

        // Then
        viewModel.state.test {
            assertEquals(GmailConnectionState.Failed, awaitItem())
        }
    }

    @Test
    fun `given no signed in user, when opened, then the error is shown`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        val viewModel = viewModel()

        // Then
        viewModel.state.test {
            assertEquals(GmailConnectionState.Failed, awaitItem())
        }
    }

    @Test
    fun `given the account is connected, when the page closes, then the success page shows the address`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Connected(email = EMAIL)))

        // Then
        coVerify(exactly = 1) { refreshAfterGmailConnection(userId) }
        val state = assertIs<GmailConnectionState.Connected>(viewModel.state.value)
        assertEquals(EMAIL, state.email)
        assertTrue(state.importEmails)
    }

    @Test
    fun `given the import was turned off, when connected, then the success page leaves out the import`() = runTest {
        // Given
        val viewModel = viewModel(importEmails = "false")

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Connected(email = EMAIL)))

        // Then
        val state = assertIs<GmailConnectionState.Connected>(viewModel.state.value)
        assertFalse(state.importEmails)
    }

    @Test
    fun `given the success page, when taking the user to the inbox, then the inbox is selected and opened`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Connected(email = EMAIL)))

        // When
        viewModel.submit(GmailConnectionAction.OpenInboxClicked)

        // Then
        verify(exactly = 1) { selectMailLabelId(inbox) }
        val state = assertIs<GmailConnectionState.Connected>(viewModel.state.value)
        assertEquals(Unit, state.openInbox.consume())
    }

    @Test
    fun `given the page was closed by the user, when it closes, then the account is still refreshed`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Cancelled))

        // Then
        coVerify(exactly = 1) { refreshAfterGmailConnection(userId) }
        assertIs<GmailConnectionState.Finished>(viewModel.state.value)
    }

    @Test
    fun `given the user is at the connection limit, when they ask to upgrade, then the upsell opens`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.UpsellRequested))

        // Then
        coVerify(exactly = 0) { refreshAfterGmailConnection(any()) }
        val state = assertIs<GmailConnectionState.UpsellRequested>(viewModel.state.value)
        assertEquals(UpsellingVisibility.Normal.MailPlus, state.openUpsell.consume())
    }

    @Test
    fun `given the plans are still loading, when the user asks to upgrade, then the upsell opens once loaded`() =
        runTest {
            // Given
            upsellingVisibility.value = UpsellingVisibility.Hidden
            val viewModel = viewModel()

            // When
            viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.UpsellRequested))
            advanceTimeBy(1_000)
            upsellingVisibility.value = UpsellingVisibility.Normal.Unlimited

            // Then
            val state = assertIs<GmailConnectionState.UpsellRequested>(viewModel.state.value)
            assertEquals(UpsellingVisibility.Normal.Unlimited, state.openUpsell.consume())
        }

    @Test
    fun `given there is no upsell to offer, when the user asks to upgrade, then the flow closes after the wait`() =
        runTest {
            // Given
            upsellingVisibility.value = UpsellingVisibility.Hidden
            val viewModel = viewModel()

            // When
            viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.UpsellRequested))
            advanceTimeBy(4_000)
            assertIs<GmailConnectionState.OpeningPage>(viewModel.state.value)
            advanceTimeBy(2_000)

            // Then
            assertIs<GmailConnectionState.Finished>(viewModel.state.value)
        }

    @Test
    fun `given the connection failed, when the page closes, then the error is shown`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Failed(code = null)))

        // Then
        coVerify(exactly = 0) { refreshAfterGmailConnection(any()) }
        assertEquals(GmailConnectionState.Failed, viewModel.state.value)
    }

    @Test
    fun `given a failure, when retrying, then the connection page is opened again`() = runTest {
        // Given
        coEvery { forkSessionWithKeyPayload(userId) } returns SessionError.Local.Unknown.left() andThen fork.right()
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.RetryClicked)

        // Then
        assertIs<GmailConnectionState.OpeningPage>(viewModel.state.value)
    }

    @Test
    fun `given the app was killed with the page open, when the screen comes back, then the page is not reopened`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle = savedStateHandle)

            // When
            val restored = viewModel(savedStateHandle = savedStateHandle)

            // Then
            coVerify(exactly = 1) { forkSessionWithKeyPayload(userId) }
            assertEquals(GmailConnectionState.Loading, restored.state.value)
        }

    @Test
    fun `given the app was killed while the page was open, when the page returns, then its result is handled`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle = savedStateHandle)
            val restored = viewModel(savedStateHandle = savedStateHandle)

            // When
            restored.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Cancelled))

            // Then
            coVerify(exactly = 1) { refreshAfterGmailConnection(userId) }
            assertIs<GmailConnectionState.Finished>(restored.state.value)
        }

    @Test
    fun `given the page had already returned, when the screen comes back after the app was killed, then it closes`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle = savedStateHandle)
                .submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Cancelled))

            // When
            val restored = viewModel(savedStateHandle = savedStateHandle)

            // Then
            coVerify(exactly = 1) { forkSessionWithKeyPayload(userId) }
            assertIs<GmailConnectionState.Finished>(restored.state.value)
        }

    @Test
    fun `given the connection had failed, when the screen comes back after the app was killed, then the error shows`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle = savedStateHandle)
                .submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Failed(code = null)))

            // When
            val restored = viewModel(savedStateHandle = savedStateHandle)

            // Then
            coVerify(exactly = 1) { forkSessionWithKeyPayload(userId) }
            assertEquals(GmailConnectionState.Failed, restored.state.value)
        }

    @Test
    fun `given the import is on, when the page opens, then a connection start with import is reported`() = runTest {
        // When
        viewModel(surface = ByoeSurface.Spotlight)

        // Then
        verify(exactly = 1) {
            reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectStarted(ByoeSurface.Spotlight, ByoeImportMode.Enabled))
        }
    }

    @Test
    fun `given the import is off, when the page opens, then a connection start without import is reported`() = runTest {
        // When
        viewModel(importEmails = "false", surface = ByoeSurface.Settings)

        // Then
        verify(exactly = 1) {
            reportByoeTelemetryEvent(
                ByoeTelemetryEvent.ConnectStarted(ByoeSurface.Settings, ByoeImportMode.Disabled)
            )
        }
    }

    @Test
    fun `given the fork fails, when opened, then the start and an unknown failure are reported`() = runTest {
        // Given
        coEvery { forkSessionWithKeyPayload(userId) } returns SessionError.Local.Unknown.left()

        // When
        viewModel()

        // Then
        verify(exactly = 1) {
            reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectStarted(SURFACE, ByoeImportMode.Enabled))
        }
        verify(exactly = 1) {
            reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectFailed(SURFACE, ByoeConnectErrorType.Unknown))
        }
    }

    @Test
    fun `given the account is connected, when the page closes, then a successful connection is reported`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Connected(email = EMAIL)))

        // Then
        verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectSucceeded(SURFACE)) }
    }

    @Test
    fun `given the user closed the page, when it closes, then an abandoned connection is reported`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Cancelled))

        // Then
        verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectAbandoned(SURFACE)) }
    }

    @Test
    fun `given the web flow failed with a code, when the page closes, then the failure is reported with its type`() =
        runTest {
            // Given
            val viewModel = viewModel()

            // When
            viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Failed(code = "already-added")))

            // Then
            verify(exactly = 1) {
                reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectFailed(SURFACE, ByoeConnectErrorType.AlreadyAdded))
            }
        }

    @Test
    fun `given no browser can open the page, when it fails, then the error shows and an unknown failure is reported`() =
        runTest {
            // Given
            val viewModel = viewModel()

            // When
            viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.PageUnavailable))

            // Then
            assertEquals(GmailConnectionState.Failed, viewModel.state.value)
            verify(exactly = 1) {
                reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectFailed(SURFACE, ByoeConnectErrorType.Unknown))
            }
        }

    @Test
    fun `given the connection limit, when the user asks to upgrade, then no connection outcome is reported`() =
        runTest {
            // Given
            val viewModel = viewModel()

            // When
            viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.UpsellRequested))

            // Then
            verify(exactly = 0) { reportByoeTelemetryEvent(ofType<ByoeTelemetryEvent.ConnectSucceeded>()) }
            verify(exactly = 0) { reportByoeTelemetryEvent(ofType<ByoeTelemetryEvent.ConnectAbandoned>()) }
            verify(exactly = 0) { reportByoeTelemetryEvent(ofType<ByoeTelemetryEvent.ConnectFailed>()) }
        }

    @Test
    fun `given the session is still being prepared, when the user leaves, then an abandoned connection is reported`() =
        runTest {
            // Given
            val fork = CompletableDeferred<Either<SessionError, ForkWithKeyPayload>>()
            coEvery { forkSessionWithKeyPayload(userId) } coAnswers { fork.await() }
            val viewModel = viewModel()

            // When
            viewModel.reportAbandonedAttempt()

            // Then
            verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectAbandoned(SURFACE)) }
        }

    @Test
    fun `given the connection has an outcome, when the user leaves, then no abandoned connection is reported`() =
        runTest {
            // Given
            val viewModel = viewModel()
            viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.UpsellRequested))

            // When
            viewModel.reportAbandonedAttempt()

            // Then
            verify(exactly = 0) { reportByoeTelemetryEvent(ofType<ByoeTelemetryEvent.ConnectAbandoned>()) }
        }

    @Test
    fun `given a failure, when retrying, then a new connection start is reported`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Failed(code = null)))

        // When
        viewModel.submit(GmailConnectionAction.RetryClicked)

        // Then
        verify(exactly = 2) {
            reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectStarted(SURFACE, ByoeImportMode.Enabled))
        }
    }

    @Test
    fun `given the app was killed while the page was open, when the page returns, then its outcome is reported`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle(
                mapOf(GmailConnectionScreen.SURFACE_KEY to ByoeSurfaceArgument.encode(ByoeSurface.OnboardingScreen))
            )
            viewModel(savedStateHandle = savedStateHandle)
            val restored = viewModel(savedStateHandle = savedStateHandle)

            // When
            restored.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Connected(email = EMAIL)))

            // Then
            verify(exactly = 1) {
                reportByoeTelemetryEvent(
                    ByoeTelemetryEvent.ConnectStarted(ByoeSurface.OnboardingScreen, ByoeImportMode.Enabled)
                )
            }
            verify(exactly = 1) {
                reportByoeTelemetryEvent(ByoeTelemetryEvent.ConnectSucceeded(ByoeSurface.OnboardingScreen))
            }
        }

    private companion object {

        const val EMAIL = "someone@gmail.com"
        val SURFACE = ByoeSurface.SidebarBanner
        val userId = UserId("user")
        val inbox = MailLabelId.System(LabelId("inbox"))
        val fork = ForkWithKeyPayload(
            selector = Selector("selector"),
            sessionId = CookieSessionId("session"),
            payloadKey = "key",
            payloadVersion = 3,
            payloadType = "default"
        )
    }
}
