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

package ch.protonmail.android.mailupselling.presentation.ui.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.compose.theme.headlineSmallNorm

@Composable
internal fun UpsellingBottomSheetContent(
    modifier: Modifier = Modifier,
    state: UpsellingBottomSheetContentState.Data,
    actions: UpsellingBottomSheet.Actions
) {
    val contentColors = requireNotNull(UpsellingColors.BottomSheetContentColors)
    val backgroundColor = Color(LocalContext.current.getColor(UpsellingColors.BottomSheetBackgroundColor))
    val dynamicPlansModel = state.plans

    val isNarrowScreen = LocalConfiguration.current.screenWidthDp <= MailDimens.NarrowScreenWidth.value

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if (!isNarrowScreen) {
                Image(
                    modifier = Modifier.padding(
                        start = ProtonDimens.DefaultSpacing,
                        end = ProtonDimens.DefaultSpacing,
                        top = ProtonDimens.DefaultSpacing
                    ),
                    painter = painterResource(id = R.drawable.illustration_upselling),
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
            }
        }

        item {
            Text(
                modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
                text = "${stringResource(id = R.string.upselling_title)} ${dynamicPlansModel.title.text.string()}",
                style = if (isNarrowScreen) {
                    ProtonTheme.typography.headlineSmallNorm
                } else ProtonTheme.typography.headlineNorm,
                color = contentColors.textNorm
            )
        }

        item {
            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
        }

        item {
            Text(
                modifier = Modifier
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
                    .padding(top = ProtonDimens.SmallSpacing),
                text = dynamicPlansModel.description.text.string(),
                style = ProtonTheme.typography.defaultWeak,
                color = contentColors.textWeak,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
        }

        dynamicPlansModel.entitlements.forEachIndexed { index, model ->

            item { UpsellingEntitlementListItem(entitlementUiModel = model, color = contentColors.textWeak) }

            if (index != dynamicPlansModel.entitlements.lastIndex) {
                item {
                    Divider(
                        modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                        color = UpsellingColors.EntitlementsRowDivider
                    )
                }
            }
        }

        item { UpsellingPlansList(modifier = Modifier.padding(ProtonDimens.DefaultSpacing), state.plans, actions) }
    }

    LaunchedEffect(key1 = Unit) {
        actions.onDisplayed()
    }
}

@AdaptivePreviews
@Composable
private fun BottomSheetPreview() {
    ProtonTheme3 {
        UpsellingBottomSheetContent(
            state = UpsellingBottomSheetContentPreviewData.Base,
            actions = UpsellingBottomSheet.Actions(
                onDisplayed = {},
                onDismiss = {},
                onError = {},
                onPlanSelected = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onSuccess = {}
            )
        )
    }
}
