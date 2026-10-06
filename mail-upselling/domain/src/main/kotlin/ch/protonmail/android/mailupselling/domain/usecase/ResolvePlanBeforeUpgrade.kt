/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.domain.usecase

import kotlinx.coroutines.CancellationException
import me.proton.android.core.payment.domain.usecase.GetCurrentSubscriptions
import timber.log.Timber
import javax.inject.Inject

class ResolvePlanBeforeUpgrade @Inject constructor(
    private val getCurrentSubscriptions: GetCurrentSubscriptions
) {

    suspend operator fun invoke(): String = runCatching {
        getCurrentSubscriptions().firstOrNull()?.name ?: FreePlan
    }.getOrElse {
        if (it is CancellationException) throw it
        Timber.d(it, "upsell: could not read the current subscriptions")
        FreePlan
    }

    private companion object {

        const val FreePlan = "Free plan"
    }
}
