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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingColors
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingDimens
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UpsellingPlansList(
    modifier: Modifier = Modifier,
    dynamicPlansModel: DynamicPlansUiModel,
    actions: UpsellingBottomSheet.Actions
) {
    FlowRow(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        if (dynamicPlansModel.plans.isEmpty()) {
            TextNoPlansAvailable()
        } else {
            dynamicPlansModel.plans.forEach { planUiModel ->
                UpsellingPlanItem(
                    modifier = Modifier.weight(UpsellingDimens.UpsellingPaymentItemWeight),
                    planUiModel = planUiModel,
                    actions = actions
                )
            }
        }
    }
}

@Composable
private fun TextNoPlansAvailable() {
    val colors = requireNotNull(UpsellingColors.BottomSheetContentColors)

    Text(
        modifier = Modifier
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(top = ProtonDimens.SmallSpacing),
        text = stringResource(id = R.string.upselling_unable_retrieve_options),
        style = ProtonTheme.typography.defaultWeak,
        color = colors.textWeak,
        textAlign = TextAlign.Center
    )
}

@AdaptivePreviews
@Composable
private fun UpsellingItem() {
    ProtonTheme {
        UpsellingPlansList(
            modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
            dynamicPlansModel = UpsellingBottomSheetContentPreviewData.Base.plans,
            actions = UpsellingBottomSheet.Actions(
                onDisplayed = {},
                onDismiss = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onPlanSelected = {},
                onError = {},
                onSuccess = {}
            )
        )
    }
}
