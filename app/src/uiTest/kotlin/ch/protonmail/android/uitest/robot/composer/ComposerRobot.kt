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

package ch.protonmail.android.uitest.robot.composer

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.uitest.robot.common.section.KeyboardSection
import ch.protonmail.android.uitest.robot.composer.section.ComposerMessageBodySection
import ch.protonmail.android.uitest.robot.composer.section.ComposerParticipantsSection
import ch.protonmail.android.uitest.robot.composer.section.ComposerSubjectSection
import ch.protonmail.android.uitest.robot.composer.section.ComposerTopBarAppSection

internal class ComposerRobot(val composeTestRule: ComposeTestRule) {

    private val rootItem = composeTestRule.onNodeWithTag(ComposerTestTags.RootItem)

    fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    inner class Verify {

        fun composerIsShown() = apply {
            rootItem.assertExists()
        }
    }
}

internal fun ComposerRobot.topAppBarSection(
    block: ComposerTopBarAppSection.() -> Unit
) = ComposerTopBarAppSection(composeTestRule).apply(block)

internal fun ComposerRobot.participantsSection(
    block: ComposerParticipantsSection.() -> Unit
) = ComposerParticipantsSection(composeTestRule).apply(block)

internal fun ComposerRobot.subjectSection(
    block: ComposerSubjectSection.() -> Unit
) = ComposerSubjectSection(composeTestRule).apply(block)

internal fun ComposerRobot.messageSection(
    block: ComposerMessageBodySection.() -> Unit
) = ComposerMessageBodySection(composeTestRule).apply(block)

internal fun ComposerRobot.keyboardSection(
    block: KeyboardSection.() -> Unit
) = KeyboardSection().apply(block)
