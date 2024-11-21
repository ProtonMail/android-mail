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

package ch.protonmail.android.mailupselling.presentation.ui.screen.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.UpsellingAutoRenewPolicyText
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UpsellingPlansList(
    modifier: Modifier = Modifier,
    dynamicPlansModel: DynamicPlanInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    Column {
        UpsellingAutoRenewPolicyText(modifier = Modifier.padding(ProtonDimens.DefaultSpacing))

        FlowRow(
            modifier = modifier,
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
        ) {
            UpsellingPlanItem(
                modifier = Modifier.weight(1f),
                planUiModel = dynamicPlansModel.shorterCycle,
                actions = actions
            )
            UpsellingPlanItem(
                modifier = Modifier.weight(1f),
                planUiModel = dynamicPlansModel.longerCycle,
                actions = actions
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingItem() {
    ProtonTheme {
        UpsellingPlansList(
            modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
            dynamicPlansModel = UpsellingContentPreviewData.Base.plans.list
                as DynamicPlanInstanceListUiModel.Data,
            actions = UpsellingScreen.Actions(
                onDisplayed = {},
                onDismiss = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onUpgradeAttempt = {},
                onError = {},
                onSuccess = {}
            )
        )
    }
}
