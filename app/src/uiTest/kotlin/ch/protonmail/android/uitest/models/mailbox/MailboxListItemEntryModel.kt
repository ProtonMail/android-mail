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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItemTestTags
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.folders.MailFolderEntry
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.assertions.assertTextColor
import ch.protonmail.android.uitest.util.assertions.assertTintColor
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.extensions.peek

internal class MailboxListItemEntryModel(
    position: Int,
    private val composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    init {
        waitForItemToBeShown()
    }

    private val rootItem: SemanticsNodeInteraction = composeTestRule
        .onAllNodesWithTag(MailboxItemTestTags.ItemRow, useUnmergedTree = true)[position]

    private val avatar = rootItem.child {
        hasTestTag(AvatarTestTags.Avatar)
    }

    private val avatarDraft = rootItem.child {
        hasTestTag(AvatarTestTags.AvatarDraft)
    }

    private val participants = rootItem.child {
        hasTestTag(MailboxItemTestTags.Participants)
    }

    private val locations = rootItem.child {
        hasTestTag(MailboxItemTestTags.LocationIcons)
    }

    private val subject = rootItem.child {
        hasTestTag(MailboxItemTestTags.Subject)
    }

    private val date = rootItem.child {
        hasTestTag(MailboxItemTestTags.Date)
    }

    private val count = rootItem.child {
        hasTestTag(MailboxItemTestTags.Count)
    }

    // region actions
    fun click() = apply {
        rootItem.performClick()
    }
    // endregion

    // region verification
    fun hasAvatar(initial: AvatarInitial) = apply {
        when (initial) {
            is AvatarInitial.WithText -> avatar.assertTextEquals(initial.text)
            is AvatarInitial.Draft -> avatarDraft.assertIsDisplayed()
        }
    }

    fun hasParticipants(text: String) = apply {
        participants.assertTextEquals(text)
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
        participants.assertTextColor(MessageReadTextColorHex)
        subject.assertTextColor(MessageReadTextColorHex)
        date.assertTextColor(MessageReadTextColorHex)

        if (count.peek()) count.assertTextColor(MessageReadTextColorHex)
    }

    fun assertUnread() = apply {
        participants.assertTextColor(MessageUnreadTextColorHex)
        subject.assertTextColor(MessageUnreadTextColorHex)
        date.assertTextColor(MessageUnreadTextColorHex)

        if (count.peek()) count.assertTextColor(MessageUnreadTextColorHex)
    }
    // endregion

    // region helpers
    @OptIn(ExperimentalTestApi::class)
    private fun waitForItemToBeShown() = apply {
        composeTestRule.waitUntilAtLeastOneExists(
            matcher = hasTestTag(MailboxItemTestTags.ItemRow),
            timeoutMillis = 30_000
        )
    }
    // endregion

    private companion object {

        const val MessageReadTextColorHex = 0xFF706D6B // Equivalent to ProtonPalette.Cinder
        const val MessageUnreadTextColorHex = 0xFF0C0C14 // Equivalent to ProtonPalette.DoveGray
    }
}
