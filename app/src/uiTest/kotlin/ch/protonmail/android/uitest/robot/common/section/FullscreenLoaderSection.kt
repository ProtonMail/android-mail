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

import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.util.awaitHidden
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG

@AttachTo(targets = [ComposerRobot::class])
internal class FullscreenLoaderSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(PROTON_PROGRESS_TEST_TAG)

    fun waitUntilGone() {
        rootItem.awaitHidden()
    }
}
