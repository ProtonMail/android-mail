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

package ch.protonmail.android.maillabel.data.local

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.usecase.CreateRustSidebar
import ch.protonmail.android.maillabel.data.usecase.RustGetAllMailLabelId
import ch.protonmail.android.maillabel.data.wrapper.SidebarWrapper
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.label.rust.LocalLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.WatchHandle
import kotlin.test.assertEquals

internal class RustLabelDataSourceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val testCoroutineScope = CoroutineScope(mainDispatcherRule.testDispatcher)
    private val createRustSidebar = mockk<CreateRustSidebar>()
    private val rustGetAllMailLabelId = mockk<RustGetAllMailLabelId>()
    private val rustGetSystemLabelById = mockk<RustGetSystemLabelById>()
    private val rustGetLabelIdBySystemLabel = mockk<RustGetLabelIdBySystemLabel>()

    private val labelDataSource = RustLabelDataSource(
        userSessionRepository,
        createRustSidebar,
        rustGetAllMailLabelId,
        rustGetSystemLabelById,
        rustGetLabelIdBySystemLabel,
        testCoroutineScope,
        mainDispatcherRule.testDispatcher
    )

    @Test
    fun `observe system labels fails and logs error when session is invalid`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            coEvery { userSessionRepository.getUserSession(userId) } returns null

            // When
            labelDataSource.observeSystemLabels(userId).test {
                // Then
                loggingTestRule.assertErrorLogged("rust-label: trying to load labels with a null session")
                awaitComplete()
            }
        }

    @Test
    fun `observe system labels emits initial items when returned by the rust library`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localSystemLabelWithCount)
        val systemCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val labelsWatcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.systemLabels() } returns expected.right()
            coEvery { this@mockk.watchLabels(capture(systemCallbackSlot)) } returns labelsWatcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock

        labelDataSource.observeSystemLabels(userId).test {
            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `observe system labels emits items when rust library callback fires`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localSystemLabelWithCount)
        val systemCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val labelsWatcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.systemLabels() } returns expected.right()
            coEvery { this@mockk.watchLabels(capture(systemCallbackSlot)) } returns labelsWatcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock

        labelDataSource.observeSystemLabels(userId).test {
            awaitItem() // Skip initial state
            // When
            systemCallbackSlot.captured.onUpdate()
            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `observe system labels destroys rust object and callback when not observed anymore`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localSystemLabelWithCount)
        val watcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.systemLabels() } returns expected.right()
            coEvery { this@mockk.watchLabels(any()) } returns watcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock

        labelDataSource.observeSystemLabels(userId).test {
            // When
            cancelAndIgnoreRemainingEvents()

            // Then
            coVerify { sidebarMock.destroy() }
        }
    }

    @Test
    fun `observe message custom labels fails and logs error when session is invalid`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            coEvery { userSessionRepository.getUserSession(userId) } returns null

            // When
            labelDataSource.observeMessageLabels(userId).test {
                // Then
                loggingTestRule.assertErrorLogged("rust-label: trying to load labels with a null session")
                awaitComplete()
            }
        }

    @Test
    fun `observe message custom labels emits items when returned by the rust library`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localMessageLabelWithCount)
        val messageLabelsCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val labelsWatcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.customLabels() } returns expected.right()
            coEvery { this@mockk.watchLabels(capture(messageLabelsCallbackSlot)) } returns labelsWatcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock


        labelDataSource.observeMessageLabels(userId).test {
            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `observe message custom labels emits items when rust library callback fires`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localMessageLabelWithCount)
        val messageLabelsCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val labelsWatcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.customLabels() } returns expected.right()
            coEvery { this@mockk.watchLabels(capture(messageLabelsCallbackSlot)) } returns labelsWatcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock

        labelDataSource.observeMessageLabels(userId).test {
            awaitItem() // Skip initial state
            // When
            messageLabelsCallbackSlot.captured.onUpdate()
            // Then
            assertEquals(expected, awaitItem())
        }
    }


    @Test
    fun `observe custom labels destroys rust object and callback when not observed anymore`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localMessageLabelWithCount)
        val watcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.customLabels() } returns expected.right()
            coEvery { this@mockk.watchLabels(any()) } returns watcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock

        labelDataSource.observeMessageLabels(userId).test {
            // When
            cancelAndIgnoreRemainingEvents()

            // Then
            coVerify { sidebarMock.destroy() }
        }
    }

    @Test
    fun `observe message custom folders fails and logs error when session is invalid`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            coEvery { userSessionRepository.getUserSession(userId) } returns null

            // When
            labelDataSource.observeMessageFolders(userId).test {
                // Then
                loggingTestRule.assertErrorLogged("rust-label: trying to load labels with a null session")
                awaitComplete()
            }
        }

    @Test
    fun `observe message custom folders emits items when returned by the rust library`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localMessageFolderWithCount)
        val messageFoldersCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val watcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.allCustomFolders() } returns expected.right()
            coEvery {
                this@mockk.watchLabels(capture(messageFoldersCallbackSlot))
            } returns watcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock


        labelDataSource.observeMessageFolders(userId).test {
            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `observe message custom folders emits items when rust library callback fires`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localMessageFolderWithCount)
        val messageFoldersCallbackSlot = slot<LiveQueryCallback>()
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val watcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.allCustomFolders() } returns expected.right()
            coEvery {
                this@mockk.watchLabels(capture(messageFoldersCallbackSlot))
            } returns watcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock


        labelDataSource.observeMessageFolders(userId).test {
            awaitItem() // Skip initial state
            // When
            messageFoldersCallbackSlot.captured.onUpdate()
            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `observe custom folders destroys rust object and callback when not observed anymore`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val expected = listOf(LocalLabelTestData.localMessageFolderWithCount)
        val watcherMock = mockk<WatchHandle> {
            coEvery { this@mockk.destroy() } just Runs
        }
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val sidebarMock = mockk<SidebarWrapper> {
            coEvery { this@mockk.allCustomFolders() } returns expected.right()
            coEvery { this@mockk.watchLabels(any()) } returns watcherMock.right()
            coEvery { this@mockk.destroy() } just Runs
        }
        every { createRustSidebar(userSessionMock) } returns sidebarMock
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock

        labelDataSource.observeMessageFolders(userId).test {
            // When
            cancelAndIgnoreRemainingEvents()

            // Then
            coVerify { sidebarMock.destroy() }
        }
    }

    @Test
    fun `resolve system label by local id calls the UC with the user session and label`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val labelId = LabelId("1").toLocalLabelId()
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        coEvery {
            rustGetSystemLabelById(userSessionMock, labelId)
        } returns LocalSystemLabel.INBOX.right()

        // When
        val actual = labelDataSource.resolveSystemLabelByLocalId(userId, labelId)

        // Then
        assertEquals(LocalSystemLabel.INBOX.right(), actual)
        coVerify(exactly = 1) { rustGetSystemLabelById(userSessionMock, labelId) }
        confirmVerified(rustGetLabelIdBySystemLabel)
    }

    @Test
    fun `resolve local id by system label calls the UC with the user session and system label`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val userSessionMock = mockk<MailUserSessionWrapper>()
        val systemLabel = LocalSystemLabel.INBOX
        val expectedLocalLabelId = LocalLabelId(1u)
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionMock
        coEvery {
            rustGetLabelIdBySystemLabel(userSessionMock, systemLabel)
        } returns expectedLocalLabelId.right()

        // When
        val actual = labelDataSource.resolveLocalIdBySystemLabel(userId, systemLabel)

        // Then
        assertEquals(expectedLocalLabelId.right(), actual)
        coVerify(exactly = 1) { rustGetLabelIdBySystemLabel(userSessionMock, systemLabel) }
        confirmVerified(rustGetLabelIdBySystemLabel)
    }
}
