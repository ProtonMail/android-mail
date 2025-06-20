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

package ch.protonmail.android.mailupselling.presentation.model.dynamicplans

import androidx.annotation.DrawableRes
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel

sealed interface PlanEntitlementsUiModel {
    @JvmInline
    value class SimpleList(val items: List<PlanEntitlementListUiModel>) : PlanEntitlementsUiModel

    @JvmInline
    value class CheckedSimpleList(val items: List<TextUiModel>) : PlanEntitlementsUiModel

    @JvmInline
    value class ComparisonTableList(val items: List<ComparisonTableEntitlementItemUiModel>) : PlanEntitlementsUiModel
}

sealed class PlanEntitlementListUiModel(open val text: TextUiModel) {

    data class Default(override val text: TextUiModel, val remoteResource: String) : PlanEntitlementListUiModel(text)

    data class Overridden(override val text: TextUiModel, @DrawableRes val localResource: Int) :
        PlanEntitlementListUiModel(text)
}
