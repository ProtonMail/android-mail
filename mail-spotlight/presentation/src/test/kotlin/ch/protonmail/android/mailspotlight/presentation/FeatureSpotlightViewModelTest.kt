/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.presentation

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveIsBusinessUser
import ch.protonmail.android.mailspotlight.domain.usecase.UpdateCategoryView
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import ch.protonmail.android.mailspotlight.presentation.viewmodel.FeatureSpotlightViewModel
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeatureSpotlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appInformation = AppInformation(appVersionName = "7.7.0")
    private val markFeatureSpotlightSeen = mockk<MarkFeatureSpotlightSeen>()
    private val observeIsBusinessUser = mockk<ObserveIsBusinessUser>()
    private val updateCategoryView = mockk<UpdateCategoryView>()
    private lateinit var viewModel: FeatureSpotlightViewModel

    @BeforeTest
    fun setup() {
        every { observeIsBusinessUser() } returns flowOf(false.right())
        coEvery { updateCategoryView(any()) } returns Unit.right()
        viewModel = FeatureSpotlightViewModel(
            appInformation = appInformation,
            observeIsBusinessUser = observeIsBusinessUser,
            updateCategoryView = updateCategoryView,
            markFeatureSpotlightSeen = markFeatureSpotlightSeen
        )
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `appVersion contains correct text resource with version name`() {
        val appVersion = viewModel.appVersion
        val textModel = appVersion.text as TextUiModel.TextResWithArgs
        assertEquals(R.string.spotlight_screen_version_text, textModel.value)
        assertEquals(listOf(appInformation.appVersionName), textModel.formatArgs)
    }

    @Test
    fun `overviewFeatures list contains exactly three items`() {
        assertEquals(3, viewModel.overviewFeatures.size)
    }

    @Test
    fun `overviewFeatures list contains categories as first item`() {
        val firstFeature = viewModel.overviewFeatures[0]
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_title),
            firstFeature.title
        )
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_subtitle),
            firstFeature.description
        )
    }

    @Test
    fun `overviewFeatures list contains unread filter as second item`() {
        val secondFeature = viewModel.overviewFeatures[1]
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_title),
            secondFeature.title
        )
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_subtitle),
            secondFeature.description
        )
    }

    @Test
    fun `overviewFeatures list contains UI enhancements as third item`() {
        val thirdFeature = viewModel.overviewFeatures[2]
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_title),
            thirdFeature.title
        )
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_subtitle),
            thirdFeature.description
        )
    }

    @Test
    fun `userType is B2B when the user is a business account`() = runTest {
        // Given
        every { observeIsBusinessUser() } returns flowOf(true.right())
        val viewModel = FeatureSpotlightViewModel(
            appInformation,
            observeIsBusinessUser,
            updateCategoryView,
            markFeatureSpotlightSeen
        )

        // When / Then
        viewModel.userType.test {
            assertEquals(SpotlightUserType.B2B, awaitItem())
        }
    }

    @Test
    fun `userType is B2C when the user is not a business account`() = runTest {
        // Given
        every { observeIsBusinessUser() } returns flowOf(false.right())
        val viewModel = FeatureSpotlightViewModel(
            appInformation,
            observeIsBusinessUser,
            updateCategoryView,
            markFeatureSpotlightSeen
        )

        // When / Then
        viewModel.userType.test {
            assertEquals(SpotlightUserType.B2C, awaitItem())
        }
    }

    @Test
    fun `userType falls back to B2C when the business check fails`() = runTest {
        // Given
        every { observeIsBusinessUser() } returns flowOf(DataError.Local.NoUserSession.left())
        val viewModel = FeatureSpotlightViewModel(
            appInformation,
            observeIsBusinessUser,
            updateCategoryView,
            markFeatureSpotlightSeen
        )

        // When / Then
        viewModel.userType.test {
            assertEquals(SpotlightUserType.B2C, awaitItem())
        }
    }

    @Test
    fun `onTryCategories enables category view, marks seen and emits close`() = runTest {
        coEvery { markFeatureSpotlightSeen() } returns Unit.right()

        viewModel.closeScreenEvent.test {
            viewModel.onTryCategories()
            assertEquals(Unit, awaitItem())
        }

        coVerify(exactly = 1) { updateCategoryView(enabled = true) }
        coVerify(exactly = 1) { markFeatureSpotlightSeen() }
    }

    @Test
    fun `onDismissWithoutCategories disables category view, marks seen and emits close`() = runTest {
        coEvery { markFeatureSpotlightSeen() } returns Unit.right()

        viewModel.closeScreenEvent.test {
            viewModel.onDismissWithoutCategories()
            assertEquals(Unit, awaitItem())
        }

        coVerify(exactly = 1) { updateCategoryView(enabled = false) }
        coVerify(exactly = 1) { markFeatureSpotlightSeen() }
    }

}
