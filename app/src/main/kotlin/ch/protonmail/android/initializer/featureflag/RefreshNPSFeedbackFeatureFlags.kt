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

package ch.protonmail.android.initializer.featureflag

import ch.protonmail.android.mailcommon.domain.MailFeatureId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshNPSFeedbackFeatureFlags @Inject constructor(
    private val accountManager: AccountManager,
    private val scopeProvider: CoroutineScopeProvider,
    private val featureFlagManager: FeatureFlagManager
) {

    operator fun invoke() {
        scopeProvider.GlobalIOSupervisedScope.launch {
            accountManager.getAccounts().first().forEach { account ->
                refreshNPSFeedbackFeatureFlag(account.userId)
            }
        }
    }

    private suspend fun refreshNPSFeedbackFeatureFlag(userId: UserId) {
        featureFlagManager.getOrDefault(
            userId = userId,
            featureId = MailFeatureId.NPSFeedback.id,
            default = FeatureFlag.default(MailFeatureId.NPSFeedback.id.id, false),
            refresh = true
        )
    }
}
