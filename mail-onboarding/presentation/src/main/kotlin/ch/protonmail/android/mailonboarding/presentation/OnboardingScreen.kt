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

package ch.protonmail.android.mailonboarding.presentation

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingUiModel
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingButton
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingContent
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingIndexDots
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.compose.theme.ProtonTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    exitAction: () -> Unit,
    onUpsellingNavigation: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val isEligibleForUpselling = state is OnboardingState.ToUpsell
    val isUpsellingEligibilityPending = state is OnboardingState.Loading

    val onExitAction = if (isEligibleForUpselling || isUpsellingEligibilityPending) {
        { onUpsellingNavigation() }
    } else {
        exitAction
    }

    val contentMap = listOfNotNull(
        OnboardingUiModel(
            illustrationId = R.drawable.illustration_privacy_for_all,
            headlineId = R.string.onboarding_privacy_for_all_headline,
            descriptionId = R.string.onboarding_privacy_for_all_description
        ),
        OnboardingUiModel(
            illustrationId = R.drawable.illustration_easily_up_to_date,
            headlineId = R.string.onboarding_easily_up_to_date_headline,
            descriptionId = R.string.onboarding_easily_up_to_date_description
        ),
        OnboardingUiModel(
            illustrationId = R.drawable.illustration_neat_and_tidy,
            headlineId = R.string.onboarding_neat_and_tidy_headline,
            descriptionId = R.string.onboarding_neat_and_tidy_description
        ),
        if (isEligibleForUpselling) OnboardingUiModel.Empty else null
    )

    val viewCount = contentMap.size
    val pagerState = rememberPagerState(pageCount = { viewCount })

    var isSwipingToUpsellingPage by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState, viewCount) {
        snapshotFlow {
            Triple(pagerState.currentPage, pagerState.targetPage, viewCount)
        }
            .distinctUntilChanged()
            .map { (currentPage, targetPage) ->

                val fromPage = currentPage + 1
                val toPage = targetPage + 1

                // return true if we're showing upselling and are about to swipe to last page
                isEligibleForUpselling && fromPage == viewCount - 1 && toPage == viewCount
            }
            .collect { isSwipingToUpsellingPage = it }
    }

    LaunchedEffect(isSwipingToUpsellingPage) {
        if (isSwipingToUpsellingPage) {
            onUpsellingNavigation()
        }
    }

    Column(
        modifier = Modifier
            .testTag(OnboardingScreenTestTags.RootItem)
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {

        // Keep the spacing to avoid the jumping effect on transitioning to the last page.
        Row(
            modifier = Modifier
                .testTag(OnboardingScreenTestTags.TopBarRootItem)
                .fillMaxWidth()
                .heightIn(min = MailDimens.OnboardingCloseButtonToolbarHeight)
        ) {
            if (pagerState.currentPage != viewCount.minus(1)) {
                IconButton(
                    modifier = Modifier
                        .testTag(OnboardingScreenTestTags.CloseButton)
                        .horizontalScroll(state = ScrollState(0), enabled = true),
                    onClick = onExitAction
                ) {
                    Icon(
                        tint = ProtonTheme.colors.iconNorm,
                        imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.onboarding_close_content_description)
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingContent(content = contentMap[pageIndex])
        }

        if (!isEligibleForUpselling || pagerState.currentPage != viewCount.minus(1)) {
            OnboardingButton(onExitAction, pagerState, viewCount)
            OnboardingIndexDots(pagerState, viewCount)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    ProtonTheme {
        OnboardingScreen(onUpsellingNavigation = {}, exitAction = {})
    }
}

object OnboardingScreenTestTags {

    const val RootItem = "OnboardingScreenRootItem"
    const val TopBarRootItem = "OnboardingTopBarRootItem"
    const val CloseButton = "OnboardingScreenCloseButton"
    const val BottomButton = "OnboardingScreenBottomButton"
    const val OnboardingImage = "OnboardingScreenWelcomeImage"
}
