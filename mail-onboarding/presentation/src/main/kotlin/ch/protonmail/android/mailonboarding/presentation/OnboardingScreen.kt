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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingUiModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(shouldShowUpselling: Boolean, onCloseOnboarding: () -> Unit) {
    val contentMap = listOfNotNull(
        OnboardingUiModel(
            illustrationId = R.drawable.illustration_onboarding_ga,
            headlineId = R.string.onboarding_headline_ga,
            descriptionId = R.string.onboarding_description_ga
        ),
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
        if (shouldShowUpselling) {
            OnboardingUiModel(
                illustrationId = R.drawable.empty,
                headlineId = R.string.empty,
                descriptionId = R.string.empty
            )
        } else null
    )
    val viewCount = contentMap.size
    val pagerState = rememberPagerState(pageCount = { viewCount })

    var isSwipingToUpsellingPage by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { Pair(pagerState.currentPage, pagerState.targetPage) }
            .distinctUntilChanged()
            .map { (currentPage, targetPage) ->

                val fromPage = currentPage + 1
                val toPage = targetPage + 1

                // return true if we're showing upselling and are about to swipe to last page
                shouldShowUpselling && fromPage == viewCount - 1 && toPage == viewCount
            }
            .collect { isSwipingToUpsellingPage = it }
    }

    LaunchedEffect(isSwipingToUpsellingPage) {
        if (isSwipingToUpsellingPage) {
            onCloseOnboarding()
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
                    onClick = {
                        onCloseOnboarding()
                    }
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

        OnboardingButton(onCloseOnboarding, pagerState, viewCount)
        OnboardingIndexDots(pagerState, viewCount)
    }
}

@Composable
fun OnboardingContent(content: OnboardingUiModel) {
    Column(Modifier.fillMaxHeight()) {
        Image(
            modifier = Modifier
                .testTag(OnboardingScreenTestTags.OnboardingImage)
                .fillMaxHeight(MailDimens.OnboardingIllustrationWeight)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit,
            painter = painterResource(id = content.illustrationId),
            contentDescription = stringResource(id = R.string.onboarding_illustration_content_description)
        )

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ProtonDimens.DefaultSpacing)
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            text = stringResource(id = content.headlineId),
            style = ProtonTheme.typography.headlineNorm.copy(textAlign = TextAlign.Center)
        )

        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .padding(ProtonDimens.DefaultSpacing),
                text = stringResource(id = content.descriptionId),
                style = ProtonTheme.typography.defaultWeak.copy(textAlign = TextAlign.Center)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingButton(
    onCloseOnboarding: () -> Unit,
    pagerState: PagerState,
    viewCount: Int
) {
    val scope = rememberCoroutineScope()

    ProtonSolidButton(
        modifier = Modifier
            .testTag(OnboardingScreenTestTags.BottomButton)
            .padding(ProtonDimens.DefaultSpacing)
            .height(MailDimens.onboardingBottomButtonHeight)
            .fillMaxWidth()
            .horizontalScroll(state = ScrollState(0), enabled = true),
        onClick = {
            val nextPageIndex = pagerState.currentPage.plus(1)
            if (nextPageIndex == viewCount) {
                onCloseOnboarding()
            } else {
                scope.launch {
                    pagerState.animateScrollToPage(nextPageIndex)
                }
            }
        }
    ) {
        val positiveButtonTextId =
            if (pagerState.currentPage == viewCount.minus(1)) R.string.onboarding_get_started
            else R.string.onboarding_next
        Text(text = stringResource(id = positiveButtonTextId))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingIndexDots(pagerState: PagerState, viewCount: Int) {
    val highlightedDotColor = ProtonTheme.colors.brandNorm
    val defaultDotColor = ProtonTheme.colors.shade20

    Row {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.SmallSpacing)
                .size(MailDimens.pagerDotsCircleSize),
            onDraw = {
                var centerOffset = Offset(
                    size.width.div(2).minus(MailDimens.pagerDotsCircleSize.toPx().times(viewCount.minus(1))),
                    this.center.y
                )
                for (i in 0 until viewCount) {
                    drawCircle(
                        color = if (i == pagerState.currentPage) highlightedDotColor else defaultDotColor,
                        center = centerOffset
                    )
                    centerOffset = Offset(
                        centerOffset.x.plus(MailDimens.pagerDotsCircleSize.toPx().times(2)),
                        centerOffset.y
                    )
                }
            }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    ProtonTheme {
        OnboardingScreen(
            shouldShowUpselling = true,
            onCloseOnboarding = {}
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
