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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.UpdateCategoryView
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import ch.protonmail.android.mailspotlight.presentation.ui.SPOTLIGHT_USER_TYPE_KEY
import ch.protonmail.android.mailspotlight.presentation.viewmodel.FeatureSpotlightViewModel
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeatureSpotlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appInformation = AppInformation(appVersionName = "7.7.0")
    private val markFeatureSpotlightSeen = mockk<MarkFeatureSpotlightSeen>()
    private val updateCategoryView = mockk<UpdateCategoryView>()

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    private fun buildViewModel(userTypeArg: String?): FeatureSpotlightViewModel {
        coEvery { updateCategoryView(any()) } returns Unit.right()
        return FeatureSpotlightViewModel(
            savedStateHandle = SavedStateHandle(
                if (userTypeArg == null) emptyMap() else mapOf(SPOTLIGHT_USER_TYPE_KEY to userTypeArg)
            ),
            appInformation = appInformation,
            updateCategoryView = updateCategoryView,
            markFeatureSpotlightSeen = markFeatureSpotlightSeen
        )
    }

    @Test
    fun `appVersion contains correct text resource with version name`() {
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)
        val appVersion = viewModel.appVersion
        val textModel = appVersion.text as TextUiModel.TextResWithArgs
        assertEquals(R.string.spotlight_screen_version_text, textModel.value)
        assertEquals(listOf(appInformation.appVersionName), textModel.formatArgs)
    }

    @Test
    fun `userType reflects the B2B navigation argument`() {
        val viewModel = buildViewModel(SpotlightUserType.B2B.name)
        assertEquals(SpotlightUserType.B2B, viewModel.userType)
    }

    @Test
    fun `userType reflects the B2C navigation argument`() {
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)
        assertEquals(SpotlightUserType.B2C, viewModel.userType)
    }

    @Test
    fun `userType falls back to B2C when the navigation argument is missing`() {
        val viewModel = buildViewModel(userTypeArg = null)
        assertEquals(SpotlightUserType.B2C, viewModel.userType)
    }

    @Test
    fun `userType falls back to B2C when the navigation argument is invalid`() {
        val viewModel = buildViewModel(userTypeArg = "not-a-user-type")
        assertEquals(SpotlightUserType.B2C, viewModel.userType)
    }

    @Test
    fun `overviewFeatures list contains exactly three items`() {
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)
        assertEquals(3, viewModel.overviewFeatures.size)
    }

    @Test
    fun `overviewFeatures list contains categories as first item`() {
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)
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
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)
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
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)
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
    fun `overviewFeatures uses B2B subtitles for a business user`() {
        val viewModel = buildViewModel(SpotlightUserType.B2B.name)
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_subtitle_b2b),
            viewModel.overviewFeatures[0].description
        )
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_subtitle_b2b),
            viewModel.overviewFeatures[1].description
        )
        assertEquals(
            TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_subtitle),
            viewModel.overviewFeatures[2].description
        )
    }

    @Test
    fun `onTryCategories enables category view, marks seen and emits close`() = runTest {
        coEvery { markFeatureSpotlightSeen() } returns Unit.right()
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)

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
        val viewModel = buildViewModel(SpotlightUserType.B2C.name)

        viewModel.closeScreenEvent.test {
            viewModel.onDismissWithoutCategories()
            assertEquals(Unit, awaitItem())
        }

        coVerify(exactly = 1) { updateCategoryView(enabled = false) }
        coVerify(exactly = 1) { markFeatureSpotlightSeen() }
    }
}
