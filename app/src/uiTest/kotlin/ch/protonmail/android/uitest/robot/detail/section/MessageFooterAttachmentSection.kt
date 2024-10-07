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

package ch.protonmail.android.uitest.robot.detail.section

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentFooterTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.attachments.AttachmentDetailItemEntry
import ch.protonmail.android.uitest.robot.detail.model.attachments.AttachmentDetailItemEntryModel
import ch.protonmail.android.uitest.robot.detail.model.attachments.AttachmentDetailSummaryEntry
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.child

@AttachTo(
    targets = [
        ComposerRobot::class,
        ConversationDetailRobot::class,
        MessageDetailRobot::class
    ],
    identifier = "attachmentsSection"
)
internal class MessageFooterAttachmentSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(
        testTag = AttachmentFooterTestTags.Root,
        useUnmergedTree = true
    )

    private val paperClipIcon = rootItem.child {
        hasTestTag(AttachmentFooterTestTags.PaperClipIcon)
    }

    private val summaryText = rootItem.child {
        hasTestTag(AttachmentFooterTestTags.SummaryText)
    }

    private val summarySize = rootItem.child {
        hasTestTag(AttachmentFooterTestTags.SummarySize)
    }

    init {
        scrollTo()
        // If the section is expanding in a conversation detail, tapping via interactors might fail.
        composeTestRule.waitForIdle()
    }

    fun tapItem(position: Int = 0) = withItemEntryModel(position) {
        tapItem()
    }

    private fun scrollTo() {
        Espresso.closeSoftKeyboard()
        rootItem.awaitDisplayed().performScrollTo()
    }

    @VerifiesOuter
    inner class Verify {

        fun hasLoaderDisplayedForItem(position: Int = 0) = withItemEntryModel(position) {
            hasLoaderIcon()
        }

        fun hasLoaderNotDisplayedForItem(position: Int = 0) = withItemEntryModel(position) {
            hasNoLoaderIcon()
        }

        fun hasSummaryDetails(details: AttachmentDetailSummaryEntry) {
            paperClipIcon.assertIsDisplayed()
            summaryText.assertTextEquals(details.summary)
            summarySize.assertTextEquals(details.size)
        }

        fun hasAttachments(vararg entries: AttachmentDetailItemEntry) {
            for (entry in entries) {
                withItemEntryModel(entry.index) {
                    waitUntilShown()
                        .hasIcon()
                        .hasName(entry.fileName)
                        .hasSize(entry.fileSize)
                        .also {
                            if (entry.hasDeleteIcon) it.hasDeleteIcon() else it.hasNoDeleteIcon()
                        }
                }
            }
        }
    }

    private fun withItemEntryModel(position: Int, block: AttachmentDetailItemEntryModel.() -> Unit) {
        val model = AttachmentDetailItemEntryModel(position, rootItem)
        block(model)
    }
}
