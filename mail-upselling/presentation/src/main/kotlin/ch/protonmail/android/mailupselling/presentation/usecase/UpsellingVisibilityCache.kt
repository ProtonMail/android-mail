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

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.proton.core.payment.domain.PurchaseManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpsellingVisibilityCache @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    purchaseManager: PurchaseManager,
    @AppScope appScope: CoroutineScope
) {

    private val cachedResponse: ConcurrentHashMap<UpsellingEntryPoint.Feature, CachedValue> = ConcurrentHashMap()

    init {
        appScope.launch {
            combine(
                observePrimaryUser().distinctUntilChanged(),
                purchaseManager.observePurchases()
            ) { _, _ ->
                cachedResponse.clear()
            }.collect {}
        }
    }

    fun retrieve(upsellingEntryPoint: UpsellingEntryPoint.Feature): Boolean? {
        val cached = cachedResponse[upsellingEntryPoint]
        return cached?.takeIf { !it.isExpired() }?.value
    }

    fun store(upsellingEntryPoint: UpsellingEntryPoint.Feature, value: Boolean) {
        cachedResponse[upsellingEntryPoint] = CachedValue(Instant.now().toEpochMilli() + EXPIRY_DURATION_MS, value)
    }
}

private data class CachedValue(
    val expiresAtMs: Long,
    val value: Boolean
) {
    fun isExpired() = Instant.now().toEpochMilli() > expiresAtMs
}

private val EXPIRY_DURATION_MS = Duration.ofMinutes(20).toMillis()
