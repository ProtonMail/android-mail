/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.domain.cache

import java.util.concurrent.ConcurrentHashMap
import ch.protonmail.android.mailupselling.domain.annotation.UpsellingCacheScope
import ch.protonmail.android.mailupselling.domain.sdk.SdkBackedGetAvailableUpgrades
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class AvailableUpgradesCache @Inject constructor(
    private val getAvailableUpgrades: SdkBackedGetAvailableUpgrades,
    @UpsellingCacheScope private val scope: CoroutineScope
) {

    private val cache = ConcurrentHashMap<UserId, MutableStateFlow<CacheState>>()

    fun observe(userId: UserId): Flow<List<ProductOfferList>> {
        return flow {
            val mutableStateFlow = getOrCreateMutableCache(userId)

            val initialState = mutableStateFlow.value
            if (initialState is CacheState.Success && initialState.isExpired()) {
                scope.launch { loadUpgrades(mutableStateFlow) }
            }

            emitAll(
                mutableStateFlow.map { state ->
                    when (state) {
                        is CacheState.Loading -> emptyList()
                        is CacheState.Success -> state.upgrades
                        is CacheState.Error -> emptyList()
                    }
                }
            )
        }
    }

    suspend fun get(userId: UserId): List<ProductOfferList> {
        val mutableStateFlow = getOrCreateMutableCache(userId)

        val currentState = mutableStateFlow.value
        if (currentState is CacheState.Success) {
            if (!currentState.isExpired()) {
                return currentState.upgrades
            } else {
                // Trigger reload for expired cache
                scope.launch { loadUpgrades(mutableStateFlow) }
            }
        }

        return withTimeoutOrNull(COLLECTION_TIMEOUT_MS) {
            mutableStateFlow.first { it !is CacheState.Loading }
        }?.let { state ->
            when (state) {
                is CacheState.Success -> state.upgrades
                else -> emptyList()
            }
        } ?: emptyList()
    }

    fun invalidateAll() {
        cache.values.forEach { mutableStateFlow ->
            scope.launch {
                loadUpgrades(mutableStateFlow)
            }
        }
    }

    private fun getOrCreateMutableCache(userId: UserId): MutableStateFlow<CacheState> {
        return cache.getOrPut(userId) {
            MutableStateFlow<CacheState>(CacheState.Loading).also { stateFlow ->
                scope.launch {
                    loadUpgrades(stateFlow)
                }
            }
        }
    }

    private suspend fun loadUpgrades(stateFlow: MutableStateFlow<CacheState>) {
        stateFlow.value = CacheState.Loading

        @Suppress("TooGenericExceptionCaught")
        try {
            val upgrades = getAvailableUpgrades()
            stateFlow.value = CacheState.Success(upgrades)
        } catch (e: Exception) {
            Timber.d(e, "Failed to load available upgrades")
            stateFlow.value = CacheState.Error(e)
        }
    }

    private companion object {

        const val COLLECTION_TIMEOUT_MS = 15_000L
    }
}

private sealed class CacheState {
    object Loading : CacheState()

    data class Success(
        val upgrades: List<ProductOfferList>,
        val timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : CacheState() {

        fun isExpired(): Boolean = Clock.System.now().toEpochMilliseconds() - timestamp > CACHE_EXPIRATION_MS
    }

    data class Error(val exception: Throwable) : CacheState()

    companion object {

        const val CACHE_EXPIRATION_MS = 30 * 60 * 1000L // 30 minutes
    }
}
