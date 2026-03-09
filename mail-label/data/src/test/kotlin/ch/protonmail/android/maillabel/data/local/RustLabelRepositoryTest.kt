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
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.data.mapper.toLabelType
import ch.protonmail.android.maillabel.data.mapper.toSystemLabel
import ch.protonmail.android.maillabel.data.repository.RustLabelRepository
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.LabelWithSystemLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.label.rust.LocalLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uniffi.mail_uniffi.LabelDescription
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
class RustLabelRepositoryTest {

    private val labelDataSource = mockk<LabelDataSource>()

    private val labelRepository = RustLabelRepository(labelDataSource)

    @Test
    fun `observe system labels from rust data source`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val localLabelWithCount = LocalLabelTestData.localSystemLabelWithCount
        val labelDescription = localLabelWithCount.description as LabelDescription.System
        val expectedLabel = LabelWithSystemLabelId(
            Label(
                labelId = localLabelWithCount.id.toLabelId(),
                parentId = null,
                name = localLabelWithCount.name,
                type = labelDescription.toLabelType(),
                path = "",
                color = "",
                order = localLabelWithCount.displayOrder.toInt(),
                isNotified = localLabelWithCount.notify,
                isExpanded = null,
                isSticky = localLabelWithCount.sticky
            ),
            labelDescription.v1?.toSystemLabel() ?: SystemLabelId.AllMail
        )

        every { labelDataSource.observeSystemLabels(userId) } returns flowOf(listOf(localLabelWithCount))

        // When
        labelRepository.observeSystemLabels(userId).test {
            // Then
            assertEquals(listOf(expectedLabel), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message labels from rust data source`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val localLabelWithCount = LocalLabelTestData.localMessageLabelWithCount
        val expectedLabel = LabelTestData.messageLabel

        every { labelDataSource.observeMessageLabels(userId) } returns flowOf(listOf(localLabelWithCount))

        // When
        labelRepository.observeCustomLabels(userId).test {
            // Then
            assertEquals(listOf(expectedLabel), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message folder from rust data source`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val localLabelWithCount = LocalLabelTestData.localMessageFolderWithCount
        val expectedLabel = LabelTestData.messageFolder

        every { labelDataSource.observeMessageFolders(userId) } returns flowOf(listOf(localLabelWithCount))

        // When
        labelRepository.observeCustomFolders(userId).test {
            // Then
            assertEquals(listOf(expectedLabel), awaitItem())
            awaitComplete()
        }
    }

    @RunWith(Parameterized::class)
    class SystemLabelTest(
        @Suppress("unused") private val testName: String,
        private val input: TestInput
    ) {

        private val labelDataSource = mockk<LabelDataSource>()

        private val labelRepository = RustLabelRepository(labelDataSource)

        @Test
        fun `test label with system label id is mapped correctly`() = runTest {
            // Given
            val userId = UserIdTestData.userId
            val localLabelWithCount = LocalLabelTestData.buildSystem(input.localSystemLabel)
            every { labelDataSource.observeSystemLabels(userId) } returns flowOf(listOf(localLabelWithCount))

            // When
            labelRepository.observeSystemLabels(userId).test {

                // Then
                assertEquals(input.expected, awaitItem().first().systemLabelId)
                awaitComplete()
            }
        }

        companion object {

            val inputs = listOf(
                TestInput(
                    LocalSystemLabel.INBOX,
                    SystemLabelId.Inbox
                ),
                TestInput(
                    LocalSystemLabel.ARCHIVE,
                    SystemLabelId.Archive
                ),
                TestInput(
                    LocalSystemLabel.ALL_DRAFTS,
                    SystemLabelId.AllDrafts
                ),
                TestInput(
                    LocalSystemLabel.ALL_SENT,
                    SystemLabelId.AllSent
                ),
                TestInput(
                    LocalSystemLabel.TRASH,
                    SystemLabelId.Trash
                ),
                TestInput(
                    LocalSystemLabel.SPAM,
                    SystemLabelId.Spam
                ),
                TestInput(
                    LocalSystemLabel.ALL_MAIL,
                    SystemLabelId.AllMail
                ),
                TestInput(
                    LocalSystemLabel.SENT,
                    SystemLabelId.Sent
                ),
                TestInput(
                    LocalSystemLabel.DRAFTS,
                    SystemLabelId.Drafts
                ),
                TestInput(
                    LocalSystemLabel.OUTBOX,
                    SystemLabelId.Outbox
                ),
                TestInput(
                    LocalSystemLabel.STARRED,
                    SystemLabelId.Starred
                ),
                TestInput(
                    LocalSystemLabel.SCHEDULED,
                    SystemLabelId.AllScheduled
                ),
                TestInput(
                    LocalSystemLabel.SNOOZED,
                    SystemLabelId.Snoozed
                )
            )

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = inputs
                .map { testInput ->
                    val testName = """
                        Raw system label Id: ${testInput.localSystemLabel}
                        Expected: ${testInput.expected}
                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }

        data class TestInput(
            val localSystemLabel: LocalSystemLabel,
            val expected: SystemLabelId
        )
    }


}
