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

package ch.protonmail.android.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugError
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugEvent
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugRepository
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugSwitches
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class TelemetryDebugViewModel @Inject constructor(
    private val repository: TelemetryDebugRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val searchQuery = MutableStateFlow("")
    private val actionError = MutableStateFlow<String?>(null)

    /**
     * Emits immediately, then every [AutoRefreshIntervalMillis]. Being cold, it restarts (and so
     * refreshes) whenever the screen re-subscribes.
     */
    private val autoRefreshTicks = flow {
        while (true) {
            emit(Unit)
            delay(AutoRefreshIntervalMillis.milliseconds)
        }
    }

    private val snapshots = combine(autoRefreshTicks, refreshTrigger) { _, _ -> }
        .mapLatest { readSnapshot() }

    val state: StateFlow<TelemetryDebugState> = combine(
        snapshots,
        searchQuery,
        actionError
    ) { snapshot, query, error ->
        snapshot.fold(
            ifLeft = { TelemetryDebugState.Failed(it.message) },
            ifRight = { (switches, events) ->
                TelemetryDebugState.Data(
                    capturing = switches.capturing,
                    sendingHalted = switches.sendingHalted,
                    searchQuery = query,
                    events = events.filter { it.matches(query) }.map(::toUiModel),
                    capturedCount = events.size,
                    actionError = error
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = TelemetryDebugState.Loading
    )

    fun onSearchQueryChanged(query: String) = searchQuery.update { query }

    fun onRefresh() = refresh()

    fun onCaptureToggled(enabled: Boolean) = runAction { repository.setCapturing(enabled) }

    fun onHaltSendingToggled(halted: Boolean) = runAction { repository.setSendingHalted(halted) }

    fun onClearCapturedEvents() = runAction { repository.clearCapturedEvents() }

    fun onDiscardPendingQueue() = runAction { repository.discardPendingQueue() }

    private fun runAction(action: suspend () -> Either<TelemetryDebugError, Unit>) {
        viewModelScope.launch {
            actionError.update { action().leftOrNull()?.message }
            refresh()
        }
    }

    private suspend fun readSnapshot(): Either<TelemetryDebugError, Snapshot> = either {
        Snapshot(repository.getSwitches().bind(), repository.getEvents().bind())
    }

    private fun refresh() = refreshTrigger.update { it + 1 }

    private fun TelemetryDebugEvent.matches(query: String): Boolean {
        if (query.isBlank()) return true
        val haystack = sequenceOf(name, measurementGroup, id) +
            dimensions.asSequence().flatMap { sequenceOf(it.key, it.value) } +
            values.keys.asSequence()
        return haystack.any { it.contains(query, ignoreCase = true) }
    }

    private fun toUiModel(event: TelemetryDebugEvent) = TelemetryDebugEventUiModel(
        id = event.id,
        name = event.name,
        measurementGroup = event.measurementGroup,
        values = event.values.toEntries { it.toString() },
        dimensions = event.dimensions.toEntries { it },
        payload = toPayload(event)
    )

    private fun <T> Map<String, T>.toEntries(formatValue: (T) -> String) = entries
        .sortedBy { it.key }
        .map { TelemetryDebugEntryUiModel(it.key, formatValue(it.value)) }

    private fun toPayload(event: TelemetryDebugEvent): String {
        val json = JsonObject(
            mapOf(
                "measurementGroup" to JsonPrimitive(event.measurementGroup),
                "event" to JsonPrimitive(event.name),
                "values" to JsonObject(event.values.toSortedMap().mapValues { JsonPrimitive(it.value) }),
                "dimensions" to JsonObject(event.dimensions.toSortedMap().mapValues { JsonPrimitive(it.value) })
            )
        )
        return payloadJson.encodeToString(JsonObject.serializer(), json)
    }

    private data class Snapshot(val switches: TelemetryDebugSwitches, val events: List<TelemetryDebugEvent>)

    private companion object {

        const val AutoRefreshIntervalMillis = 30_000L
        const val StopTimeoutMillis = 5_000L

        val payloadJson = Json { prettyPrint = true }
    }
}
