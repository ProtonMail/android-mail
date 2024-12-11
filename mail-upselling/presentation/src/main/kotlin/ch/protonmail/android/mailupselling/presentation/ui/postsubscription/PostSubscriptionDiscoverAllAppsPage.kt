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

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventType
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.PostSubscriptionState
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.AppItemBackground
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.AppMessageTextColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.CardBackground
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.CardDividerColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.InstallButtonBackgroundColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.InstallButtonContentColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.InstallButtonDisabledBackgroundColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.InstallButtonDisabledContentColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.InstallButtonDisabledTextColor
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallUnspecified
import me.proton.core.compose.theme.headlineUnspecified

@Composable
fun PostSubscriptionDiscoverAllAppsPage(
    state: PostSubscriptionState,
    trackTelemetryEvent: (PostSubscriptionTelemetryEventType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is PostSubscriptionState.Loading -> ProtonCenteredProgress()
        is PostSubscriptionState.Data -> PostSubscriptionDiscoverAllAppsPage(
            state = state,
            trackTelemetryEvent = trackTelemetryEvent,
            onClose = onClose,
            modifier = modifier
        )
    }
}

@Composable
private fun PostSubscriptionDiscoverAllAppsPage(
    state: PostSubscriptionState.Data,
    trackTelemetryEvent: (PostSubscriptionTelemetryEventType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentContext = LocalContext.current
    val listState = rememberLazyListState()
    val isScrolled = remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
            .collect { isScrolled.value = it }
    }

    LaunchedEffect(Unit) {
        trackTelemetryEvent(PostSubscriptionTelemetryEventType.LastStepDisplayed)
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState
        ) {
            item {
                Spacer(modifier = Modifier.size(ProtonDimens.LargeSpacing + ProtonDimens.LargeSpacing))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.post_subscription_discover_apps_page_title),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.headlineUnspecified.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.size(ProtonDimens.LargeSpacing))
            }
            item {
                Card(
                    shape = ProtonTheme.shapes.large,
                    colors = CardDefaults.cardColors().copy(
                        containerColor = CardBackground
                    )
                ) {
                    state.apps.forEachIndexed { index, app ->
                        AppItem(
                            logo = app.logo,
                            appName = app.name,
                            appMessage = app.message,
                            isInstalled = app.isInstalled,
                            onButtonClick = {
                                trackTelemetryEvent(PostSubscriptionTelemetryEventType.DownloadApp(app.packageName))

                                currentContext.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(PLAY_STORE_APP_DETAILS_BASE_URL + app.packageName)
                                    )
                                )
                            }
                        )
                        if (index != state.apps.size - 1) {
                            HorizontalDivider(color = CardDividerColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.size(ProtonDimens.MediumSpacing))
            }
        }
    }
}

@Composable
private fun AppItem(
    @DrawableRes logo: Int,
    @StringRes appName: Int,
    @StringRes appMessage: Int,
    isInstalled: Boolean,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = AppItemBackground)
            .padding(ProtonDimens.MediumSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                modifier = Modifier.size(MailDimens.ExtraLargeSpacing),
                painter = painterResource(id = logo),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
            Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = appName),
                    style = ProtonTheme.typography.defaultSmallStrongUnspecified.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
                Text(
                    text = stringResource(id = appMessage),
                    style = ProtonTheme.typography.defaultSmallUnspecified.copy(color = AppMessageTextColor)
                )
            }
        }
        Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onButtonClick,
            enabled = !isInstalled,
            shape = ProtonTheme.shapes.large,
            colors = ButtonDefaults.protonTextButtonColors(
                backgroundColor = InstallButtonBackgroundColor,
                contentColor = InstallButtonContentColor,
                disabledBackgroundColor = InstallButtonDisabledBackgroundColor,
                disabledContentColor = InstallButtonDisabledContentColor
            )
        ) {
            Text(
                text = stringResource(
                    id = if (isInstalled) {
                        R.string.post_subscription_button_installed
                    } else {
                        R.string.post_subscription_button_get_the_app
                    }
                ),
                style = ProtonTheme.typography.defaultSmallUnspecified.copy(
                    color = if (isInstalled) InstallButtonDisabledTextColor else Color.White
                )
            )
        }
    }
}

private const val PLAY_STORE_APP_DETAILS_BASE_URL = "https://play.google.com/store/apps/details?id="
