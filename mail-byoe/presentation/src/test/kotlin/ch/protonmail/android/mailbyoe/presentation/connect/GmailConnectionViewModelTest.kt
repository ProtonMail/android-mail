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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult
import ch.protonmail.android.mailbyoe.domain.usecase.RefreshAfterGmailConnection
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionAction
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
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = GmailConnectionViewModel(
        observePrimaryUserId,
        forkSessionWithKeyPayload,
        refreshAfterGmailConnection,
        findLocalSystemLabelId,
        selectMailLabelId,
        ApiEnvironment(host = "proton.me", apiPrefix = "mail-api"),
        GmailConnectionReducer(),
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
            assertTrue(url.endsWith("#selector=selector&sk=key&pv=3&pt=default"))
        }
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
    fun `given the app was killed while the page was open, when the screen comes back, then the page is not opened again`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle)

            // When
            val restored = viewModel(savedStateHandle)

            // Then
            coVerify(exactly = 1) { forkSessionWithKeyPayload(userId) }
            assertEquals(GmailConnectionState.Loading, restored.state.value)
        }

    @Test
    fun `given the app was killed while the page was open, when the page returns, then its result is handled`() =
        runTest {
            // Given
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle)
            val restored = viewModel(savedStateHandle)

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
            viewModel(savedStateHandle).submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Cancelled))

            // When
            val restored = viewModel(savedStateHandle)

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

    private companion object {

        const val EMAIL = "someone@gmail.com"
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
