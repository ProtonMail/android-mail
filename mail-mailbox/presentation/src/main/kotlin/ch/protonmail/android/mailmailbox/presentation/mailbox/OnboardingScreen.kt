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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import ch.protonmail.android.mailcommon.presentation.compose.HyperlinkText
import ch.protonmail.android.mailcommon.presentation.compose.LockScreenOrientation
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.OnboardingUiModel
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(actions: MailboxScreen.Actions) {
    val pagerState = rememberPagerState()

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val contentMap = listOf(
        OnboardingUiModel(
            illustrationId = R.drawable.illustration_onboarding_beta,
            headlineId = R.string.onboarding_headline_beta,
            descriptionId = R.string.onboarding_description_beta,
            hyperLinks = listOf(
                HyperlinkText(
                    text = stringResource(R.string.onboarding_description_beta_learn_more),
                    url = stringResource(R.string.onboarding_description_beta_link)
                )
            )
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
        )
    )
    val viewCount = contentMap.size

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        val (positiveButton, dismissButton, dots) = createRefs()

        HorizontalPager(
            state = pagerState,
            pageCount = viewCount,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingContent(content = contentMap[pageIndex])
        }

        OnboardingButton(actions, pagerState, viewCount, positiveButton, dismissButton, dots)

        OnboardingIndexDots(pagerState, viewCount, dots)
    }
}

@Composable
fun OnboardingContent(content: OnboardingUiModel) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val (illustration, headline, description) = createRefs()

        Image(
            modifier = Modifier
                .fillMaxHeight(MailDimens.OnboardingIllustrationWeight)
                .fillMaxWidth()
                .constrainAs(illustration) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            contentScale = ContentScale.Crop,
            painter = painterResource(id = content.illustrationId),
            contentDescription = stringResource(id = R.string.onboarding_illustration_content_description)
        )
        Text(
            modifier = Modifier
                .constrainAs(headline) {
                    width = Dimension.fillToConstraints
                    top.linkTo(illustration.bottom, margin = ProtonDimens.DefaultSpacing)
                    start.linkTo(parent.start, margin = ProtonDimens.LargeSpacing)
                    end.linkTo(parent.end, margin = ProtonDimens.LargeSpacing)
                },
            textAlign = TextAlign.Center,
            text = stringResource(id = content.headlineId),
            style = ProtonTheme.typography.headlineNorm
        )
        HyperlinkText(
            modifier = Modifier
                .constrainAs(description) {
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                    top.linkTo(headline.bottom, margin = ProtonDimens.DefaultSpacing)
                    start.linkTo(parent.start, margin = ProtonDimens.LargeSpacing)
                    end.linkTo(parent.end, margin = ProtonDimens.LargeSpacing)
                    bottom.linkTo(parent.bottom, margin = ProtonDimens.DefaultSpacing)
                },
            fullText = stringResource(id = content.descriptionId),
            hyperLinks = content.hyperLinks,
            textStyle = ProtonTheme.typography.defaultWeak.copy(
                textAlign = TextAlign.Center
            ),
            linkTextColor = ProtonTheme.colors.textAccent
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConstraintLayoutScope.OnboardingButton(
    actions: MailboxScreen.Actions,
    pagerState: PagerState,
    viewCount: Int,
    positiveButton: ConstrainedLayoutReference,
    dismissButton: ConstrainedLayoutReference,
    dots: ConstrainedLayoutReference
) {
    val scope = rememberCoroutineScope()

    ProtonSolidButton(
        modifier = Modifier
            .constrainAs(positiveButton) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start, margin = ProtonDimens.MediumSpacing)
                end.linkTo(parent.end, margin = ProtonDimens.MediumSpacing)
                bottom.linkTo(dots.top, margin = ProtonDimens.DefaultSpacing)
            }
            .horizontalScroll(state = ScrollState(0), enabled = true),
        onClick = {
            val nextPageIndex = pagerState.currentPage.plus(1)
            if (nextPageIndex == viewCount) {
                actions.closeOnboarding()
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

    if (pagerState.currentPage != viewCount.minus(1)) {
        IconButton(
            modifier = Modifier
                .constrainAs(dismissButton) {
                    width = Dimension.fillToConstraints
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
                .horizontalScroll(state = ScrollState(0), enabled = true),
            onClick = {
                actions.closeOnboarding()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConstraintLayoutScope.OnboardingIndexDots(
    pagerState: PagerState,
    viewCount: Int,
    dots: ConstrainedLayoutReference
) {
    val highlightedDotColor = ProtonTheme.colors.brandNorm
    val defaultDotColor = ProtonTheme.colors.shade20
    Canvas(
        modifier = Modifier
            .size(MailDimens.pagerDotsCircleSize)
            .constrainAs(dots) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom, margin = ProtonDimens.LargeSpacing)
            },
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    ProtonTheme {
        OnboardingScreen(
            actions = MailboxScreen.Actions.Empty
        )
    }
}
