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

package ch.protonmail.android.uitest.screen.mailbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.maillabel.presentation.previewdata.MailboxItemLabelsPreviewData
import ch.protonmail.android.maillabel.presentation.ui.LabelsList
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Rule
import kotlin.test.Test

class MailboxItemLabelsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenAllLabelsCanFitTheScreenShowsThemEntirely() {

        // given
        val labels = MailboxItemLabelsPreviewData.ThreeItems

        // when
        setupWithState(labels)

        // then
        for (label in labels) {
            composeTestRule.onNodeWithText(label.name)
                .assertIsDisplayed()
        }
    }

    private fun setupWithState(labels: List<LabelUiModel>) {
        composeTestRule.setContent {
            ProtonTheme {
                LabelsList(labels = labels)
            }
        }
    }
}
