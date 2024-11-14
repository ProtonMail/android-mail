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

package ch.protonmail.android.mailupselling.presentation.ui.postsubscription

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.BackgroundGradientColorStops
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.BottomSectionBackgroundColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.CloseButtonBackground
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.HorizontalDividerColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.OtherPageIndicatorColor
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongUnspecified

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostSubscriptionScreen(onClose: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = BackgroundGradientColorStops
                )
            )
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.TopEnd
        ) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState
            ) { page ->
                when (page) {
                    FIRST_PAGE -> PostSubscriptionWelcomePage()
                }
            }

            CloseButton(onClick = onClose)
        }

        HorizontalDivider(color = HorizontalDividerColor)

        BottomSection(pagerState = pagerState)
    }
}

@Composable
private fun CloseButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(ProtonDimens.SmallSpacing)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    radius = MailDimens.PostSubscriptionCloseButtonRippleRadius,
                    color = Color.White
                ),
                role = Role.Button,
                onClick = onClick
            )
            .padding(ProtonDimens.SmallSpacing)
            .background(color = CloseButtonBackground, shape = CircleShape)
            .padding(ProtonDimens.SmallSpacing)
    ) {
        Icon(
            modifier = Modifier.size(ProtonDimens.SmallIconSize),
            painter = painterResource(id = R.drawable.ic_proton_close),
            contentDescription = stringResource(id = R.string.post_subscription_close_button_content_description),
            tint = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomSection(pagerState: PagerState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BottomSectionBackgroundColor)
            .padding(
                horizontal = ProtonDimens.MediumSpacing,
                vertical = ProtonDimens.DefaultSpacing
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProtonSolidButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(MailDimens.ExtraLargeSpacing),
            onClick = {},
            colors = ButtonDefaults.protonButtonColors(
                backgroundColor = Color.White,
                contentColor = ProtonTheme.colors.shade100
            )
        ) {
            Text(
                text = stringResource(id = R.string.post_subscription_continue_button),
                style = ProtonTheme.typography.defaultStrongUnspecified.copy(color = ProtonTheme.colors.shade100)
            )
        }
        Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
        PageIndicator(pagerState = pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicator(pagerState: PagerState) {
    Row {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.SmallSpacing)
                .size(MailDimens.pagerDotsCircleSize),
            onDraw = {
                var centerOffset = Offset(
                    size.width
                        .div(2)
                        .minus(MailDimens.pagerDotsCircleSize.toPx().times(pagerState.pageCount.minus(1))),
                    this.center.y
                )
                for (i in 0 until pagerState.pageCount) {
                    drawCircle(
                        color = if (i == pagerState.currentPage) Color.White else OtherPageIndicatorColor,
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

private const val PAGE_COUNT = 1
private const val FIRST_PAGE = 0

@Preview(showBackground = true)
@Composable
fun PostSubscriptionContentPreview() {
    PostSubscriptionScreen(
        onClose = {}
    )
}
