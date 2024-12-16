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

package ch.protonmail.android.uitest.robot.common.section

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.snackbar.SnackbarEntry
import ch.protonmail.android.uitest.models.snackbar.SnackbarType
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.assertions.hasAnyChildWith
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden

@AttachTo(
    targets = [
        ComposerRobot::class,
        ConversationDetailRobot::class,
        MessageDetailRobot::class,
        MailboxRobot::class
    ]
)
internal class SnackbarSection : ComposeSectionRobot() {

    // There are different hosts, thus they're defined as lazy to avoid
    // spending unnecessary time locating unnecessary nodes.
    private val snackbarHostDefault: SemanticsNodeInteraction by lazy {
        composeTestRule.onAllNodesWithTag(CommonTestTags.SnackbarHost).onFirst()
    }

    private val snackbarHostError: SemanticsNodeInteraction by lazy {
        composeTestRule.onNodeWithTag(CommonTestTags.SnackbarHostError, useUnmergedTree = true)
    }

    private val snackbarHostWarning: SemanticsNodeInteraction by lazy {
        composeTestRule.onNodeWithTag(CommonTestTags.SnackbarHostWarning)
    }

    private val snackbarHostNormal: SemanticsNodeInteraction by lazy {
        composeTestRule.onNodeWithTag(CommonTestTags.SnackbarHostNormal)
    }

    private val snackbarHostSuccess: SemanticsNodeInteraction by lazy {
        composeTestRule.onNodeWithTag(CommonTestTags.SnackbarHostSuccess)
    }

    fun waitUntilDismisses(entry: SnackbarEntry) {
        val host = resolveHost(entry)
        host.awaitHidden()
    }

    @VerifiesOuter
    inner class Verify {

        fun isDisplaying(entry: SnackbarEntry) = apply {
            val host = resolveHost(entry)

            // The actual text node is not an immediate child, so the hierarchy needs to be traversed.
            host.awaitDisplayed(timeout = entry.timeout)
                .hasAnyChildWith(hasText(entry.value))
        }
    }

    private fun resolveHost(entry: SnackbarEntry): SemanticsNodeInteraction {
        return when (entry.type) {
            SnackbarType.Default -> snackbarHostDefault
            SnackbarType.Normal -> snackbarHostNormal
            SnackbarType.Error -> snackbarHostError
            SnackbarType.Warning -> snackbarHostWarning
            SnackbarType.Success -> snackbarHostSuccess
        }
    }
}
