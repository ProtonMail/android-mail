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

package ch.protonmail.android.uitest.screen.sidebar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.mailsidebar.presentation.Sidebar
import ch.protonmail.android.mailsidebar.presentation.SidebarMenuTestTags
import ch.protonmail.android.mailsidebar.presentation.SidebarState
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import ch.protonmail.android.maillabel.R as label
import me.proton.core.presentation.compose.R as core

private const val APP_VERSION_FOOTER = "Proton Mail 6.0.0-alpha+test"

@RegressionTest
@HiltAndroidTest
internal class SidebarScreenTest : HiltInstrumentedTest() {

    @Test
    fun subscriptionIsShownWhenSidebarStateIsDisplaySubscription() {
        setupScreenWithState(showSubscriptionSidebarState())

        scrollToSidebarBottom()

        composeTestRule
            .onNodeWithText(core.string.presentation_menu_item_title_subscription)
            .assertIsDisplayed()
    }

    @Test
    fun subscriptionIsHiddenWhenSidebarStateIsHideSubscription() {
        setupScreenWithState(hideSubscriptionSidebarState())

        scrollToSidebarBottom()
        composeTestRule
            .onNodeWithText(core.string.presentation_menu_item_title_subscription)
            .assertDoesNotExist()
    }

    @Test
    fun labelsAreOnlyDisplayingTitleEmptyItemsAndAddItem() {
        setupScreenWithState(emptyLabelsSidebarState())

        listOf(
            label.string.label_title_labels,
            label.string.label_title_folders,
            label.string.label_title_create_folder,
            label.string.label_title_create_label
        ).forEach {
            composeTestRule
                .onNodeWithText(it)
                .assertIsDisplayed()
        }
    }

    @Test
    fun labelsAndFoldersAreDisplayed() {
        setupScreenWithState(someLabelsSidebarState())

        listOf(
            "Folder1",
            "Folder2",
            "Folder3",
            "Label1",
            "Label2",
            "Label3"
        ).forEach {
            composeTestRule
                .onNodeWithText(it)
                .assertIsDisplayed()
        }
    }

    private fun scrollToSidebarBottom(): SemanticsNodeInteraction {
        return composeTestRule
            .onNodeWithTag(SidebarMenuTestTags.Root)
            .onChild()
            .performScrollToNode(hasText(APP_VERSION_FOOTER, true))
    }

    private fun showSubscriptionSidebarState() = buildSidebarState(isSubscriptionVisible = true)
    private fun hideSubscriptionSidebarState() = buildSidebarState(isSubscriptionVisible = false)
    private fun emptyLabelsSidebarState() = buildSidebarState(mailLabels = MailLabelsUiModel.Loading)
    private fun someLabelsSidebarState() = buildSidebarState(
        mailLabels = MailLabelsUiModel(
            systems = emptyList(),
            folders = listOf(
                buildMailLabelFolderUiModel("Folder1"),
                buildMailLabelFolderUiModel("Folder2"),
                buildMailLabelFolderUiModel("Folder3")
            ),
            labels = listOf(
                buildMailLabelLabelUiModel("Label1"),
                buildMailLabelLabelUiModel("Label2"),
                buildMailLabelLabelUiModel("Label3")
            )
        )
    )

    private fun buildMailLabelFolderUiModel(text: String) = MailLabelUiModel.Custom(
        id = MailLabelId.Custom.Folder(LabelId(text)),
        key = text,
        text = TextUiModel.Text(text),
        icon = R.drawable.ic_proton_folder_filled,
        iconTint = Color(0),
        isSelected = false,
        count = 0,
        isVisible = true,
        isExpanded = false,
        iconPaddingStart = 0.dp
    )

    private fun buildMailLabelLabelUiModel(text: String) = MailLabelUiModel.Custom(
        id = MailLabelId.Custom.Label(LabelId(text)),
        key = text,
        text = TextUiModel.Text(text),
        icon = R.drawable.ic_proton_circle_filled,
        iconTint = Color(0),
        isSelected = false,
        count = 0,
        isVisible = true,
        isExpanded = false,
        iconPaddingStart = 0.dp
    )

    private fun buildSidebarState(
        isSubscriptionVisible: Boolean = true,
        mailLabels: MailLabelsUiModel = MailLabelsUiModel.Loading
    ) = SidebarState(
        isSubscriptionVisible = isSubscriptionVisible,
        hasPrimaryAccount = false,
        appInformation = AppInformation(
            appName = "Proton Mail",
            appVersionName = "6.0.0-alpha+test"
        ),
        mailLabels = mailLabels
    )

    private fun setupScreenWithState(state: SidebarState) {
        composeTestRule.setContent {
            ProtonTheme {
                Sidebar(viewState = state, actions = Sidebar.Actions.Empty)
            }
        }
    }
}
