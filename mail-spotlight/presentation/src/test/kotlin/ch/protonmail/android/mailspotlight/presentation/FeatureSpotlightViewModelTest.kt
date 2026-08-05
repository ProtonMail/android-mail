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
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
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

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    private fun buildViewModel() = FeatureSpotlightViewModel(
        appInformation = appInformation,
        markFeatureSpotlightSeen = markFeatureSpotlightSeen
    )

    @Test
    fun `appVersion contains correct text resource with version name`() {
        val viewModel = buildViewModel()
        val appVersion = viewModel.appVersion
        val textModel = appVersion.text as TextUiModel.TextResWithArgs
        assertEquals(R.string.spotlight_screen_version_text, textModel.value)
        assertEquals(listOf(appInformation.appVersionName), textModel.formatArgs)
    }

    @Test
    fun `overviewFeatures contains the three content search items in order`() {
        val viewModel = buildViewModel()
        assertEquals(
            listOf(
                FeatureItem(
                    icon = R.drawable.ic_arrow_down_to_line,
                    title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_message_content_title),
                    description = TextUiModel.TextRes(
                        R.string.spotlight_screen_content_search_message_content_subtitle
                    )
                ),
                FeatureItem(
                    icon = R.drawable.ic_magnifier,
                    title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_bottom_bar_title),
                    description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_bottom_bar_subtitle)
                ),
                FeatureItem(
                    icon = R.drawable.ic_envelope_lines,
                    title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_recent_searches_title),
                    description = TextUiModel.TextRes(
                        R.string.spotlight_screen_content_search_recent_searches_subtitle
                    )
                )
            ),
            viewModel.overviewFeatures
        )
    }

    @Test
    fun `onGotIt marks the spotlight as seen and emits close`() = runTest {
        coEvery { markFeatureSpotlightSeen() } returns Unit.right()
        val viewModel = buildViewModel()

        viewModel.closeScreenEvent.test {
            viewModel.onGotIt()
            assertEquals(Unit, awaitItem())
        }

        coVerify(exactly = 1) { markFeatureSpotlightSeen() }
    }
}
