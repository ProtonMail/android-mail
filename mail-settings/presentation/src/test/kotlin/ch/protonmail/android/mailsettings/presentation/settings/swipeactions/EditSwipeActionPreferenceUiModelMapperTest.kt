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

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.mailsettings.presentation.testdata.SwipeActionsTestData.Edit.buildAllItems
import me.proton.core.mailsettings.domain.entity.SwipeAction
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EditSwipeActionPreferenceUiModelMapperTest {

    private val mapper = EditSwipeActionPreferenceUiModelMapper()

    @Test
    fun `all items are unselected without swipe preferences`() {
        // given
        val preferences: SwipeActionsPreference? = null
        val direction = SwipeActionDirection.RIGHT

        // when
        val result = mapper.toUiModels(preferences, direction)

        // then
        val expected = buildAllItems(selected = null)
        assertEquals(expected, result)
    }

    @Test
    fun `correct item is selected for swipe right preferences`() {
        // given
        val preferences = SwipeActionsPreference(
            swipeLeft = SwipeAction.Trash,
            swipeRight = SwipeAction.Spam
        )
        val direction = SwipeActionDirection.RIGHT

        // when
        val result = mapper.toUiModels(preferences, direction)

        // then
        val expected = buildAllItems(selected = SwipeAction.Spam)
        assertEquals(expected, result)
    }

    @Test
    fun `correct item is selected for swipe left preferences`() {
        // given
        val preferences = SwipeActionsPreference(
            swipeLeft = SwipeAction.Trash,
            swipeRight = SwipeAction.Spam
        )
        val direction = SwipeActionDirection.LEFT

        // when
        val result = mapper.toUiModels(preferences, direction)

        // then
        val expected = buildAllItems(selected = SwipeAction.Trash)
        assertEquals(expected, result)
    }
}
