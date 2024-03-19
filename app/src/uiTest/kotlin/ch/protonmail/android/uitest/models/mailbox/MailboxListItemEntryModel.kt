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

package ch.protonmail.android.uitest.models.mailbox

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItemTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.folders.MailFolderEntry
import ch.protonmail.android.uitest.models.folders.MailLabelEntry
import ch.protonmail.android.uitest.util.assertions.assertItemIsRead
import ch.protonmail.android.uitest.util.assertions.assertTintColor
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.child
import kotlin.time.Duration.Companion.seconds

internal class MailboxListItemEntryModel(
    private val position: Int,
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    private val parent: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(
        MailboxScreenTestTags.List,
        useUnmergedTree = true
    )

    private val itemMatcher: SemanticsMatcher
        get() = hasTestTag("${MailboxItemTestTags.ItemRow}$position")

    private val item: SemanticsNodeInteraction = parent.child { itemMatcher }

    private val avatarRootItem = item.child {
        hasTestTag(AvatarTestTags.AvatarRootItem)
    }

    private val avatar = avatarRootItem.child {
        hasTestTag(AvatarTestTags.AvatarText)
    }

    private val avatarDraft = avatarRootItem.child {
        hasTestTag(AvatarTestTags.AvatarDraft)
    }

    private val avatarSelected = avatarRootItem.child {
        hasTestTag(AvatarTestTags.AvatarSelectionMode)
    }

    private val locations = item.child {
        hasTestTag(MailboxItemTestTags.LocationIcons)
    }

    private val labels = item.child {
        hasTestTag(MailboxItemTestTags.LabelsList)
    }

    private val subject = item.child {
        hasTestTag(MailboxItemTestTags.Subject)
    }

    private val date = item.child {
        hasTestTag(MailboxItemTestTags.Date)
    }

    private val count = item.child {
        hasTestTag(MailboxItemTestTags.Count)
    }

    init {
        waitForItemToBeShown()
    }

    // region actions
    fun click() = apply {
        item.performClick()
    }

    fun longClick() = apply {
        item.performTouchInput { longClick() }
    }

    fun selectEntry() = apply {
        val semanticsNode = if (avatarSelected.peekIsDisplayed()) avatarSelected else avatar
        semanticsNode.performClick()
    }

    fun unselectEntry() = apply {
        avatarSelected.performClick()
    }
    // endregion

    // region verification
    fun isSelected() = apply {
        avatarSelected.assertIsDisplayed().assertIsSelected()
    }

    fun isNotSelected() = apply {
        if (avatarSelected.peekIsDisplayed()) {
            avatarSelected.assertIsNotSelected()
            avatar.assertDoesNotExist()
        } else {
            avatarSelected.assertDoesNotExist()
            avatar.assertIsDisplayed()
        }
    }

    fun hasAvatar(initial: AvatarInitial) = apply {
        when (initial) {
            is AvatarInitial.WithText -> avatar.assertTextEquals(initial.text)
            is AvatarInitial.Draft -> avatarDraft.assertIsDisplayed()
        }
    }

    fun hasParticipants(participants: List<ParticipantEntry>) = apply {
        participants.forEachIndexed { index, participant ->
            val model = ParticipantEntryModel(index, item)

            when (participant) {
                is ParticipantEntry.NoSender,
                is ParticipantEntry.NoRecipient -> model.hasNoParticipant(participant.value)

                is ParticipantEntry.WithParticipant -> {
                    model.hasParticipant(participant.value)
                        .isProton(participant.isProton)
                }
            }
        }
    }

    fun hasLocationIcons(entries: List<MailFolderEntry>) = apply {
        for (entry in entries) {
            val folderIcon = locations.onChildAt(entry.index)
            folderIcon.assertTintColor(entry.iconTint)
        }
    }

    fun hasNoLocationIcons() = apply {
        locations.assertDoesNotExist()
    }

    fun hasSubject(text: String) = apply {
        subject.assertTextEquals(text)
    }

    fun hasLabels(entries: List<MailLabelEntry>) = apply {
        for (entry in entries) {
            val label = labels.onChildAt(entry.index)
            label.assertTextEquals(entry.name)
        }
    }

    fun hasNoLabels() = apply {
        labels.assertIsNotDisplayed()
    }

    fun hasDate(text: String) = apply {
        date.assertTextEquals(text)
    }

    fun hasCount(text: String) = apply {
        count.assertTextEquals(text)
    }

    fun hasNoCount() = apply {
        count.assertDoesNotExist()
    }

    fun assertRead() = apply {
        item.assertItemIsRead(expectedValue = true)
    }

    fun assertUnread() = apply {
        item.assertItemIsRead(expectedValue = false)
    }
    // endregion

    // region helpers
    private fun waitForItemToBeShown() = apply {
        parent
            .awaitDisplayed(timeout = 30.seconds)
            .performScrollToNode(itemMatcher)
    }

    private fun SemanticsNodeInteraction.peekIsDisplayed() = runCatching { assertIsDisplayed() }.isSuccess
    // endregion
}
