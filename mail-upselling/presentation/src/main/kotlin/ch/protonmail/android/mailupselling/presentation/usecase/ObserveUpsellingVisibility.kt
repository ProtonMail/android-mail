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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailfeatureflags.domain.annotation.IsUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.model.hasSubscription
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsession.domain.usecase.ObserveUser
import ch.protonmail.android.mailupselling.domain.annotation.PlayServicesAvailableValue
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlanUpgrades
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class ObserveUpsellingVisibility @Inject constructor(
    private val resolveUpsellingVisibilityForPlans: ResolveUpsellingVisibilityForPlans,
    private val observePlanUpgrades: ObservePlanUpgrades,
    private val observeUser: ObserveUser,
    private val observePrimaryUserId: ObservePrimaryUserId,
    @PlayServicesAvailableValue private val playServicesAvailable: Provider<Boolean>,
    @IsUpsellEnabled private val isUpsellEnabled: FeatureFlag<Boolean>
) {

    operator fun invoke(entryPoint: UpsellingEntryPoint.Feature): Flow<UpsellingVisibility> =
        observePrimaryUserId().flatMapLatest { userId ->
            userId ?: return@flatMapLatest flowOf(UpsellingVisibility.Hidden)

            if (!playServicesAvailable.get()) {
                return@flatMapLatest flowOf(UpsellingVisibility.Hidden)
            }

            if (!isUpsellEnabled.get()) {
                Timber.d("upsell: FF disabled - hiding entry point")
                return@flatMapLatest flowOf(UpsellingVisibility.Hidden)
            }

            combine(
                observeUser(userId).filterNotNull(),
                observePlanUpgrades(entryPoint)
            ) { userEither, plusPlans ->
                val user = userEither.getOrNull() ?: return@combine UpsellingVisibility.Hidden

                if (user.hasSubscription()) { // Need Monthly + Yearly for Upselling flows
                    UpsellingVisibility.Hidden
                } else {
                    resolveUpsellingVisibilityForPlans(plusPlans).also {
                        Timber.d("upsell: resolving entry point to $it")
                    }
                }
            }
        }
}
