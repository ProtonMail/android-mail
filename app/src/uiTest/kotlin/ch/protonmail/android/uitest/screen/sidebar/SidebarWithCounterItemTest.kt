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

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarItemWithCounterTestTags
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarLabelItems
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarSystemLabelItems
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Test

@SmokeTest
@HiltAndroidTest
internal class SidebarWithCounterItemTest : HiltInstrumentedTest() {

    private val countersNode by lazy {
        composeTestRule.onNodeWithTag(SidebarItemWithCounterTestTags.Counter, useUnmergedTree = true)
    }

    @Test
    fun sidebarSystemLabelCounterDisplaysValueWhenAvailable() {
        // Given
        val systemFolder = MailLabelUiModelTestData.spamFolder.copy(count = CounterValue)

        // When
        setupLabelItem(systemFolder)

        // Then
        countersNode.assertTextEquals(CounterValueText)
    }

    @Test
    fun sidebarSystemLabelItemDisplaysNoCounterValueWhenNull() {
        // Given
        val systemFolder = MailLabelUiModelTestData.spamFolder.copy(count = null)

        // When
        setupLabelItem(systemFolder)

        // Then
        countersNode.assertDoesNotExist()
    }

    @Test
    fun sidebarSystemLabelItemDisplaysCappedCounterValueWhenAboveThreshold() {
        // Given
        val systemFolder = MailLabelUiModelTestData.spamFolder.copy(count = CounterValueAboveThreshold)

        // When
        setupLabelItem(systemFolder)

        // Then
        countersNode.assertTextEquals(CounterValueTextCapped)
    }

    @Test
    fun sidebarCustomLabelCounterDisplaysValueWhenAvailable() {
        // Given
        val customLabel = MailLabelUiModelTestData.customLabelList.first().copy(count = CounterValue)

        // When
        setupLabelItem(customLabel)

        // Then
        countersNode.assertTextEquals(CounterValueText)
    }

    @Test
    fun sidebarCustomLabelItemDisplaysNoCounterValueWhenNull() {
        // Given
        val customLabel = MailLabelUiModelTestData.customLabelList.first().copy(count = null)

        // When
        setupLabelItem(customLabel)

        // Then
        countersNode.assertDoesNotExist()
    }

    @Test
    fun sidebarCustomLabelItemDisplaysCappedCounterValueWhenAboveThreshold() {
        // Given
        val customLabel = MailLabelUiModelTestData.customLabelList.first().copy(count = CounterValueAboveThreshold)

        // When
        setupLabelItem(customLabel)

        // Then
        countersNode.assertTextEquals(CounterValueTextCapped)
    }

    private fun setupLabelItem(item: MailLabelUiModel) {
        composeTestRule.setContent {
            ProtonTheme {
                ProtonSidebarLazy {
                    when (item) {
                        is MailLabelUiModel.Custom -> sidebarLabelItems(listOf(item)) {}
                        is MailLabelUiModel.System -> sidebarSystemLabelItems(listOf(item)) {}
                    }
                }
            }
        }
    }

    private companion object {

        const val CounterValue = 10
        const val CounterValueText = CounterValue.toString()
        const val CounterValueAboveThreshold = 10_000
        const val CounterValueTextCapped = "9999+"
    }
}
