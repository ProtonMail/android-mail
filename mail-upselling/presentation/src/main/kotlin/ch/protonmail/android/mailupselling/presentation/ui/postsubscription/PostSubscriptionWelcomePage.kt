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

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.ContentTextColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.EntitlementTextColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.FadedContentColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.HorizontalDividerColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionDimens.WelcomePageIllustrationBigHeight
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionDimens.WelcomePageIllustrationBigWidth
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionDimens.WelcomePageIllustrationSmallHeight
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionDimens.WelcomePageIllustrationSmallWidth
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionDimens.WelcomePageVerticalSpacing
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallUnspecified
import me.proton.core.compose.theme.defaultUnspecified
import me.proton.core.compose.theme.headlineUnspecified

@Composable
fun PostSubscriptionWelcomePage(modifier: Modifier = Modifier, onClose: () -> Unit) {
    val listState = rememberLazyListState()
    val isScrolled = remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
            .collect { isScrolled.value = it }
    }

    val imageWidth by animateDpAsState(
        targetValue = if (isScrolled.value) WelcomePageIllustrationSmallWidth else WelcomePageIllustrationBigWidth,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing), label = ""
    )
    val imageHeight by animateDpAsState(
        targetValue = if (isScrolled.value) WelcomePageIllustrationSmallHeight else WelcomePageIllustrationBigHeight,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing), label = ""
    )
    val entitlementTextColor by animateColorAsState(
        targetValue = if (isScrolled.value) EntitlementTextColor else FadedContentColor,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing), label = ""
    )
    val entitlementDividerColor by animateColorAsState(
        targetValue = if (isScrolled.value) HorizontalDividerColor else FadedContentColor,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing), label = ""
    )

    Box {
        if (!isScrolled.value) {
            PostSubscriptionCloseButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(ProtonDimens.ExtraSmallSpacing)
                    .zIndex(1f),
                onClick = onClose
            )
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = ProtonDimens.MediumSpacing),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.size(WelcomePageVerticalSpacing)) }

            item {
                Image(
                    modifier = Modifier
                        .size(width = imageWidth, height = imageHeight)
                        .graphicsLayer { translationY = if (isScrolled.value) 0f else 0f },
                    painter = painterResource(id = R.drawable.illustration_upselling_mailbox),
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
            }

            item {
                Spacer(modifier = Modifier.size(ProtonDimens.MediumSpacing))
            }

            item {
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.post_subscription_welcome_page_title),
                    style = ProtonTheme.typography.headlineUnspecified.copy(color = Color.White)
                )
            }

            item {
                Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
            }

            item {
                Text(
                    text = stringResource(id = R.string.post_subscription_welcome_page_message),
                    style = ProtonTheme.typography.defaultUnspecified.copy(color = ContentTextColor),
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.size(MailDimens.ExtraLargeSpacing))
            }

            item {
                Text(
                    text = stringResource(id = R.string.post_subscription_welcome_page_unlocked),
                    style = ProtonTheme.typography.defaultSmallUnspecified.copy(color = ContentTextColor)
                )
            }

            item {
                Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
            }

            item {
                Icon(
                    modifier = Modifier.size(ProtonDimens.SmallIconSize),
                    painter = painterResource(id = R.drawable.ic_proton_arrow_down),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = ContentTextColor
                )
            }

            item {
                Entitlements(textColor = entitlementTextColor, dividerColor = entitlementDividerColor)
            }

            item {
                Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
            }
        }
    }
}

@Composable
private fun Entitlements(textColor: Color, dividerColor: Color) {
    Column {
        Entitlement(
            textId = R.string.post_subscription_plus_feature_storage,
            textColor = textColor,
            dividerColor = dividerColor
        )
        Entitlement(
            textId = R.string.post_subscription_plus_feature_email_addresses,
            textColor = textColor,
            dividerColor = dividerColor
        )
        Entitlement(
            textId = R.string.post_subscription_plus_feature_email_domains,
            textColor = textColor,
            dividerColor = dividerColor
        )
        Entitlement(
            textId = R.string.post_subscription_plus_feature_folders_labels,
            textColor = textColor,
            dividerColor = dividerColor
        )
        Entitlement(
            textId = R.string.post_subscription_plus_feature_aliases,
            textColor = textColor,
            dividerColor = dividerColor
        )
        Entitlement(
            textId = R.string.post_subscription_plus_feature_desktop_app,
            textColor = textColor,
            dividerColor = dividerColor
        )
        Entitlement(
            textId = R.string.post_subscription_plus_feature_customer_support,
            textColor = textColor,
            dividerColor = dividerColor
        )
    }
}

@Composable
private fun Entitlement(
    @StringRes textId: Int,
    textColor: Color,
    dividerColor: Color
) {
    Column {
        Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
        HorizontalDivider(color = dividerColor)
        Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = textId),
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.defaultUnspecified.copy(
                color = textColor
            )
        )
    }
}

private const val ANIMATION_DURATION = 250
