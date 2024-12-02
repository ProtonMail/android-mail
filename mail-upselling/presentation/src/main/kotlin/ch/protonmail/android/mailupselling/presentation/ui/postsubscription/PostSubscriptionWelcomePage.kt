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
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
fun PostSubscriptionWelcomePage(modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val transition = updateTransition(
        targetState = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }.value,
        label = ""
    )
    val imageWidth = transition.animateDp(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing) },
        label = ""
    ) { scrollOffset ->
        if (scrollOffset < SCROLL_OFFSET) WelcomePageIllustrationBigWidth else WelcomePageIllustrationSmallWidth
    }
    val imageHeight = transition.animateDp(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing) },
        label = ""
    ) { scrollOffset ->
        if (scrollOffset < SCROLL_OFFSET) WelcomePageIllustrationBigHeight else WelcomePageIllustrationSmallHeight
    }
    val entitlementTextColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing) },
        label = ""
    ) { scrollOffset ->
        if (scrollOffset < SCROLL_OFFSET) FadedContentColor else EntitlementTextColor
    }
    val entitlementDividerColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION, easing = LinearEasing) },
        label = ""
    ) { scrollOffset ->
        if (scrollOffset < SCROLL_OFFSET) FadedContentColor else HorizontalDividerColor
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ProtonDimens.MediumSpacing),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.size(WelcomePageVerticalSpacing))
            Image(
                modifier = Modifier.size(width = imageWidth.value, height = imageHeight.value),
                painter = painterResource(id = R.drawable.illustration_upselling_mailbox),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
            Spacer(modifier = Modifier.size(ProtonDimens.MediumSpacing))
            Text(
                text = stringResource(id = R.string.post_subscription_welcome_page_title),
                style = ProtonTheme.typography.headlineUnspecified.copy(color = Color.White)
            )
            Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
            Text(
                text = stringResource(id = R.string.post_subscription_welcome_page_message),
                style = ProtonTheme.typography.defaultUnspecified.copy(color = ContentTextColor),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(MailDimens.ExtraLargeSpacing))
            Text(
                text = stringResource(id = R.string.post_subscription_welcome_page_unlocked),
                style = ProtonTheme.typography.defaultSmallUnspecified.copy(color = ContentTextColor)
            )
            Icon(
                modifier = Modifier.size(ProtonDimens.SmallIconSize),
                painter = painterResource(id = R.drawable.ic_proton_arrow_down),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ContentTextColor
            )
            Entitlements(textColor = entitlementTextColor.value, dividerColor = entitlementDividerColor.value)
            Spacer(modifier = Modifier.size(WelcomePageVerticalSpacing))
        }
    }
}

@Composable
private fun Entitlements(textColor: Color, dividerColor: Color) {
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

@Composable
private fun Entitlement(
    @StringRes textId: Int,
    textColor: Color,
    dividerColor: Color
) {
    Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
    HorizontalDivider(color = dividerColor)
    Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
    Text(
        text = stringResource(id = textId),
        textAlign = TextAlign.Center,
        style = ProtonTheme.typography.defaultUnspecified.copy(
            color = textColor
        )
    )
}

private const val ANIMATION_DURATION = 1000
private const val SCROLL_OFFSET = 100
