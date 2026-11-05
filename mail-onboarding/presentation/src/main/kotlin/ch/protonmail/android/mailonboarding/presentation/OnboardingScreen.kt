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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbyoe.domain.model.ByoeOnboardingStep
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingUiModel
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingButton
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingContent
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingGmailButtons
import ch.protonmail.android.mailonboarding.presentation.ui.OnboardingIndexDots
import ch.protonmail.android.mailonboarding.presentation.ui.moveToNextPage
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingAction
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingViewModel
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(actions: OnboardingScreen.Actions, viewModel: OnboardingViewModel = hiltViewModel()) {
    val exitAction = actions.exitAction
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hasCompletedOnboarding by remember { mutableStateOf(false) }

    when {
        hasCompletedOnboarding && (state is OnboardingState.ToUpsell || state is OnboardingState.Loading) -> {
            LaunchedEffect(state) {
                if (state is OnboardingState.ToUpsell) {
                    viewModel.submit(OnboardingAction.PageShown(ByoeOnboardingStep.Upsell))
                }
            }
            OnboardingUpsellScreen(exitAction, actions.onUpsellingError)
        }

        hasCompletedOnboarding && state is OnboardingState.NoUpsell -> exitAction()
        else -> OnboardingScreen(
            state = state,
            onUpsellingNavigation = { hasCompletedOnboarding = true },
            exitAction = exitAction,
            onPageShown = { viewModel.submit(OnboardingAction.PageShown(it)) },
            gmailActions = GmailPageActions(
                onSkipped = { viewModel.submit(OnboardingAction.GmailPageSkipped) },
                onExplore = {
                    viewModel.submit(OnboardingAction.GmailExploreClicked)
                    actions.onExploreGmail()
                }
            )
        )
    }
}

@Composable
private fun OnboardingScreen(
    state: OnboardingState,
    onUpsellingNavigation: () -> Unit,
    exitAction: () -> Unit,
    onPageShown: (ByoeOnboardingStep) -> Unit,
    gmailActions: GmailPageActions
) {
    val isEligibleForUpselling = state is OnboardingState.ToUpsell
    val isUpsellingEligibilityPending = state is OnboardingState.Loading
    val showGmailPage = when (state) {
        is OnboardingState.ToUpsell -> state.showGmailPage
        is OnboardingState.NoUpsell -> state.showGmailPage
        is OnboardingState.Loading -> false
    }

    // The Gmail page comes last, so leaving onboarding or moving on to the upsell without Explore skips it.
    val onLeaveOnboarding: (() -> Unit) -> () -> Unit = { action ->
        {
            if (showGmailPage) gmailActions.onSkipped()
            action()
        }
    }
    val onUpsellingNavigationAction = onLeaveOnboarding(onUpsellingNavigation)
    val onExitAction = if (isEligibleForUpselling || isUpsellingEligibilityPending) {
        onUpsellingNavigationAction
    } else {
        onLeaveOnboarding(exitAction)
    }

    val welcomePage = OnboardingUiModel(
        illustrationId = R.drawable.illustration_onboarding_welcome,
        title = R.string.onboarding_welcome_title,
        descriptionId = R.string.onboarding_welcome_description
    )
    val gmailPage = OnboardingUiModel(
        illustrationId = R.drawable.illustration_onboarding_gmail,
        title = R.string.onboarding_gmail_title,
        descriptionId = R.string.onboarding_gmail_description
    )
    val offlinePage = OnboardingUiModel(
        illustrationId = R.drawable.illustration_onboarding_offline,
        title = R.string.onboarding_feedback_title,
        descriptionId = R.string.onboarding_feedback_description
    )
    val featurePages = if (showGmailPage) {
        listOf(welcomePage, gmailPage)
    } else {
        listOf(
            welcomePage,
            OnboardingUiModel(
                illustrationId = R.drawable.illustration_onboarding_focus,
                title = R.string.onboarding_rebuilt_title,
                descriptionId = R.string.onboarding_rebuilt_description
            ),
            offlinePage
        )
    }
    val contentMap = featurePages + listOfNotNull(if (isEligibleForUpselling) OnboardingUiModel.Empty else null)
    val gmailPageIndex = contentMap.indexOf(gmailPage)

    val viewCount = contentMap.size
    val pagerState = rememberPagerState(pageCount = { viewCount })
    val scope = rememberCoroutineScope()

    var isSwipingToUpsellingPage by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState, viewCount) {
        snapshotFlow {
            Triple(pagerState.currentPage, pagerState.targetPage, viewCount)
        }
            .distinctUntilChanged()
            .map { (_, targetPage) ->
                val toPage = targetPage + 1

                // return true if we're showing upselling and are navigating to last page
                // (handles both swipe gestures and button taps with animations disabled)
                isEligibleForUpselling && toPage == viewCount
            }
            .collect { isSwipingToUpsellingPage = it }
    }

    LaunchedEffect(isSwipingToUpsellingPage) {
        if (isSwipingToUpsellingPage) {
            onUpsellingNavigationAction()
        }
    }

    // The SDK records each page as it appears; the "rebuilt" page has no step of its own.
    LaunchedEffect(pagerState.currentPage, contentMap) {
        val step = when (contentMap.getOrNull(pagerState.currentPage)) {
            welcomePage -> ByoeOnboardingStep.Welcome
            offlinePage -> ByoeOnboardingStep.Offline
            gmailPage -> ByoeOnboardingStep.Byoe
            else -> null
        }
        step?.let(onPageShown)
    }

    Column(
        modifier = Modifier
            .testTag(OnboardingScreenTestTags.RootItem)
            .background(ProtonTheme.colors.backgroundInvertedNorm)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(ProtonDimens.Spacing.Large)
            .verticalScroll(rememberScrollState())
    ) {

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = pagerState.pageCount // pre-render all three pages to ensure consistent sizing.
        ) { pageIndex ->
            OnboardingContent(content = contentMap[pageIndex])
        }

        if (!isEligibleForUpselling || pagerState.currentPage != viewCount.minus(1)) {
            OnboardingIndexDots(pagerState.currentPage, viewCount)
            if (pagerState.currentPage == gmailPageIndex) {
                OnboardingGmailButtons(
                    onExplore = gmailActions.onExplore,
                    onLater = { scope.launch { pagerState.moveToNextPage(viewCount, onExitAction) } }
                )
            } else {
                OnboardingButton(
                    onClick = { scope.launch { pagerState.moveToNextPage(viewCount, onExitAction) } },
                    pagerState = pagerState,
                    viewCount = viewCount
                )
            }
        }
    }
}

object OnboardingScreen {

    /**
     * [onExploreGmail] runs when a user in a BYOE treatment chooses to connect Gmail from the Gmail page.
     * Onboarding ends there, and an upsell it would have shown waits until the user is back in the mailbox.
     */
    data class Actions(
        val exitAction: () -> Unit,
        val onUpsellingError: (String) -> Unit,
        val onExploreGmail: () -> Unit
    )
}

private data class GmailPageActions(
    val onSkipped: () -> Unit,
    val onExplore: () -> Unit
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    ProtonTheme {
        OnboardingScreen(
            state = OnboardingState.NoUpsell(showGmailPage = false),
            onUpsellingNavigation = {},
            exitAction = {},
            onPageShown = {},
            gmailActions = GmailPageActions(onSkipped = {}, onExplore = {})
        )
    }
}

object OnboardingScreenTestTags {

    const val RootItem = "OnboardingScreenRootItem"
    const val TopBarRootItem = "OnboardingTopBarRootItem"
    const val CloseButton = "OnboardingScreenCloseButton"
    const val BottomButton = "OnboardingScreenBottomButton"
    const val OnboardingImage = "OnboardingScreenWelcomeImage"
}
