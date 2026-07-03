/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcategory.domain.model.CategoryViewStatus
import ch.protonmail.android.mailcategory.presentation.mapper.CategoryViewUiModelMapper
import ch.protonmail.android.mailcategory.presentation.mapper.toUiModel
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcategory.presentation.model.CategorySpotlightState
import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class MailboxCategoryViewReducerTest {

    private val categoryViewUiModelMapper = mockk<CategoryViewUiModelMapper>()

    private val reducer = MailboxCategoryViewReducer(
        categoryViewUiModelMapper = categoryViewUiModelMapper
    )

    @Test
    fun `should map category view status changed event to category view state`() {
        // Given
        val categoryViewStatus = mockk<CategoryViewStatus>()
        val currentState: CategoryViewState = CategoryViewState.NotAvailable
        val expectedState: CategoryViewState = CategoryViewState.NotAvailable

        every {
            categoryViewUiModelMapper.toUiModel(categoryViewStatus)
        } returns expectedState

        val operation = MailboxEvent.CategoryViewStatusChanged(
            categoryViewStatus = categoryViewStatus
        )

        // When
        val actual = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(expectedState, actual)
    }

    @Test
    fun `should emit reset scroll effect when primary account changes and category view is available data`() {
        // Given
        val currentState = CategoryViewState.Available.Data(
            categories = CategoryItemUiModelSample.all,
            resetScrollEffect = Effect.empty()
        )

        // When
        val actual = reducer.newStateFrom(currentState, MailboxEvent.PrimaryAccountChanged)

        // Then
        assertEquals(CategoryItemUiModelSample.all, (actual as CategoryViewState.Available.Data).categories)
        assertEquals(Unit, actual.resetScrollEffect.consume())
    }

    @Test
    fun `should reset scroll position when categories become available after being unavailable`() {
        // Given
        val currentState: CategoryViewState = CategoryViewState.NotAvailable
        val categoryViewStatus = mockk<CategoryViewStatus>()
        every {
            categoryViewUiModelMapper.toUiModel(categoryViewStatus)
        } returns CategoryViewState.Available.Data(categories = CategoryItemUiModelSample.all)

        // When
        val actual = reducer.newStateFrom(
            currentState,
            MailboxEvent.CategoryViewStatusChanged(categoryViewStatus)
        )

        // Then
        assertEquals(Unit, (actual as CategoryViewState.Available.Data).resetScrollEffect.consume())
    }

    @Test
    fun `should reset scroll position when the view mode changes`() {
        // Given
        val currentState = CategoryViewState.Available.Data(
            categories = CategoryItemUiModelSample.all,
            resetScrollEffect = Effect.empty()
        )

        // When
        val actual = reducer.newStateFrom(currentState, MailboxEvent.ViewModeChanged)

        // Then
        assertEquals(Unit, (actual as CategoryViewState.Available.Data).resetScrollEffect.consume())
    }

    @Test
    fun `should keep a pending scroll reset when the category status changes`() {
        // Given
        val currentState = CategoryViewState.Available.Data(
            categories = CategoryItemUiModelSample.all,
            resetScrollEffect = Effect.of(Unit)
        )
        val categoryViewStatus = mockk<CategoryViewStatus>()
        every {
            categoryViewUiModelMapper.toUiModel(categoryViewStatus)
        } returns CategoryViewState.Available.Data(categories = CategoryItemUiModelSample.all)

        // When
        val actual = reducer.newStateFrom(
            currentState,
            MailboxEvent.CategoryViewStatusChanged(categoryViewStatus)
        )

        // Then
        assertEquals(Unit, (actual as CategoryViewState.Available.Data).resetScrollEffect.consume())
    }

    @Test
    fun `should preserve current spotlight when category view status changes`() {
        // Given
        val spotlight = CategorySpotlightState.Shown(CategoryItemUiModelSample.social)
        val currentState = CategoryViewState.Available.Data(
            categories = CategoryItemUiModelSample.all,
            spotlightState = spotlight
        )
        val categoryViewStatus = mockk<CategoryViewStatus>()
        every {
            categoryViewUiModelMapper.toUiModel(categoryViewStatus)
        } returns CategoryViewState.Available.Data(categories = CategoryItemUiModelSample.all)

        // When
        val actual = reducer.newStateFrom(
            currentState,
            MailboxEvent.CategoryViewStatusChanged(categoryViewStatus)
        )

        // Then
        assertEquals(spotlight, (actual as CategoryViewState.Available.Data).spotlightState)
    }

    @Test
    fun `should apply spotlight state changed event`() {
        // Given
        val currentState = CategoryViewState.Available.Data(categories = CategoryItemUiModelSample.all)
        val spotlight = CategorySpotlightState.Shown(CategoryItemUiModelSample.social)

        // When
        val actual = reducer.newStateFrom(
            currentState,
            MailboxEvent.CategorySpotlightStateChanged(spotlight)
        )

        // Then
        assertEquals(spotlight, (actual as CategoryViewState.Available.Data).spotlightState)
    }

    @Test
    fun `should hide spotlight when dismissed`() {
        // Given
        val currentState = CategoryViewState.Available.Data(
            categories = CategoryItemUiModelSample.all,
            spotlightState = CategorySpotlightState.Shown(CategoryItemUiModelSample.social)
        )

        // When
        val actual = reducer.newStateFrom(currentState, MailboxViewAction.DismissCategorySpotlight)

        // Then
        assertEquals(
            CategorySpotlightState.Hidden,
            (actual as CategoryViewState.Available.Data).spotlightState
        )
    }
}
