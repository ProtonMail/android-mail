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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.appbar.ProtonMediumTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TelemetryDebugScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: TelemetryDebugViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var overflowExpanded by remember { mutableStateOf(false) }
    var legendVisible by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val actions = remember(viewModel) {
        TelemetryDebugScreen.Actions(
            onCaptureToggled = viewModel::onCaptureToggled,
            onHaltSendingToggled = viewModel::onHaltSendingToggled,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onClearCapturedEvents = viewModel::onClearCapturedEvents,
            onDiscardPendingQueue = viewModel::onDiscardPendingQueue,
            onRefresh = viewModel::onRefresh
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ProtonMediumTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.app_debug_screen_telemetry_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.presentation_back)
                        )
                    }
                },
                actions = {
                    (state as? TelemetryDebugState.Data)?.let { data ->
                        CaptureAction(capturing = data.capturing, onToggled = actions.onCaptureToggled)
                        HaltSendingAction(halted = data.sendingHalted, onToggled = actions.onHaltSendingToggled)
                    }
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            legendVisible = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_proton_question_circle),
                            contentDescription = stringResource(R.string.app_debug_screen_telemetry_legend_open)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                overflowExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(
                                    R.string.app_debug_screen_telemetry_more_actions
                                )
                            )
                        }
                        TelemetryDebugOverflowMenu(
                            expanded = overflowExpanded,
                            onDismiss = { overflowExpanded = false },
                            onClearCapturedEvents = actions.onClearCapturedEvents,
                            onDiscardPendingQueue = actions.onDiscardPendingQueue
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is TelemetryDebugState.Loading -> ProtonCenteredProgress()
            is TelemetryDebugState.Failed -> Text(
                text = currentState.message,
                color = ProtonTheme.colors.notificationError,
                style = ProtonTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(ProtonDimens.Spacing.Large)
            )

            is TelemetryDebugState.Data -> TelemetryDebugContent(
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
                bottomContentPadding = paddingValues.calculateBottomPadding(),
                state = currentState,
                actions = actions
            )
        }
    }

    if (legendVisible) {
        TelemetryDebugLegendDialog(onDismiss = { legendVisible = false })
    }
}

/** A record dot, red while events are being copied to the local log. */
@Composable
private fun CaptureAction(capturing: Boolean, onToggled: (Boolean) -> Unit) {
    IconButton(onClick = { onToggled(!capturing) }) {
        Icon(
            painter = painterResource(R.drawable.ic_telemetry_capture),
            tint = if (capturing) ProtonTheme.colors.notificationError else ProtonTheme.colors.textHint,
            contentDescription = stringResource(
                if (capturing) {
                    R.string.app_debug_screen_telemetry_capture_on
                } else {
                    R.string.app_debug_screen_telemetry_capture_off
                }
            )
        )
    }
}

/** Cloud sync while events are on their way out, sync-disabled and red while delivery is halted. */
@Composable
private fun HaltSendingAction(halted: Boolean, onToggled: (Boolean) -> Unit) {
    IconButton(onClick = { onToggled(!halted) }) {
        Icon(
            painter = painterResource(
                if (halted) R.drawable.ic_telemetry_sending_off else R.drawable.ic_telemetry_sending_on
            ),
            tint = if (halted) ProtonTheme.colors.notificationError else ProtonTheme.colors.textNorm,
            contentDescription = stringResource(
                if (halted) {
                    R.string.app_debug_screen_telemetry_sending_halted
                } else {
                    R.string.app_debug_screen_telemetry_sending_on
                }
            )
        )
    }
}

@Composable
private fun TelemetryDebugOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onClearCapturedEvents: () -> Unit,
    onDiscardPendingQueue: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(ProtonTheme.colors.backgroundSecondary)
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.app_debug_screen_telemetry_clear_log)) },
            onClick = {
                onDismiss()
                onClearCapturedEvents()
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.app_debug_screen_telemetry_discard_queue)) },
            onClick = {
                onDismiss()
                onDiscardPendingQueue()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TelemetryDebugContent(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp,
    state: TelemetryDebugState.Data,
    actions: TelemetryDebugScreen.Actions
) {
    var expandedIds by remember { mutableStateOf(emptySet<String>()) }
    var eventInFocus by remember { mutableStateOf<TelemetryDebugEventUiModel?>(null) }
    val allExpanded = state.events.isNotEmpty() && expandedIds.containsAll(state.events.map { it.id })
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Scrolling means the tester is done typing: drop focus so the keyboard stops hiding the log.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) focusManager.clearFocus()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        modifier = modifier.fillMaxSize()
    ) {
        state.actionError?.let { error ->
            item {
                Text(
                    text = error,
                    color = ProtonTheme.colors.notificationError,
                    style = ProtonTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large)
                )
            }
        }

        stickyHeader {
            TelemetryDebugListHeader(
                state = state,
                allExpanded = allExpanded,
                onSearchQueryChanged = actions.onSearchQueryChanged,
                onToggleAll = {
                    expandedIds = if (allExpanded) emptySet() else state.events.map { it.id }.toSet()
                },
                onRefresh = actions.onRefresh
            )
        }

        if (state.events.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        if (state.capturedCount == 0) {
                            R.string.app_debug_screen_telemetry_events_empty
                        } else {
                            R.string.app_debug_screen_telemetry_events_no_match
                        }
                    ),
                    style = ProtonTheme.typography.bodyMedium,
                    color = ProtonTheme.colors.textWeak,
                    modifier = Modifier.padding(ProtonDimens.Spacing.Large)
                )
            }
        }

        itemsIndexed(items = state.events, key = { _, event -> event.id }) { index, event ->
            TelemetryDebugEventItem(
                event = event,
                position = index + 1,
                expanded = event.id in expandedIds,
                onClick = {
                    focusManager.clearFocus()
                    expandedIds = if (event.id in expandedIds) expandedIds - event.id else expandedIds + event.id
                },
                onLongClick = {
                    focusManager.clearFocus()
                    eventInFocus = event
                }
            )
            HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
        }
    }

    eventInFocus?.let { event ->
        TelemetryDebugEventDialog(event = event, onDismiss = { eventInFocus = null })
    }
}

@Composable
private fun TelemetryDebugListHeader(
    state: TelemetryDebugState.Data,
    allExpanded: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onToggleAll: () -> Unit,
    onRefresh: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundNorm)
            .padding(horizontal = ProtonDimens.Spacing.Large)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                textStyle = ProtonTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = stringResource(R.string.app_debug_screen_telemetry_search_hint),
                        style = ProtonTheme.typography.bodyMedium,
                        color = ProtonTheme.colors.textHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        tint = ProtonTheme.colors.textHint,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(
                                    R.string.app_debug_screen_telemetry_search_clear
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    onRefresh()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    tint = ProtonTheme.colors.textWeak,
                    contentDescription = stringResource(R.string.app_debug_screen_telemetry_refresh)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ProtonDimens.Spacing.Standard, bottom = ProtonDimens.Spacing.Standard),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.events.size == state.capturedCount) {
                    pluralStringResource(
                        R.plurals.app_debug_screen_telemetry_events_count,
                        state.capturedCount,
                        state.capturedCount
                    )
                } else {
                    pluralStringResource(
                        R.plurals.app_debug_screen_telemetry_events_count_filtered,
                        state.capturedCount,
                        state.events.size,
                        state.capturedCount
                    )
                },
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textWeak
            )
            if (state.events.isNotEmpty()) {
                Text(
                    text = stringResource(
                        if (allExpanded) {
                            R.string.app_debug_screen_telemetry_collapse_all
                        } else {
                            R.string.app_debug_screen_telemetry_expand_all
                        }
                    ),
                    style = ProtonTheme.typography.bodySmall,
                    color = ProtonTheme.colors.interactionBrandDefaultNorm,
                    modifier = Modifier.clickable {
                        focusManager.clearFocus()
                        onToggleAll()
                    }
                )
            }
        }
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TelemetryDebugEventItem(
    event: TelemetryDebugEventUiModel,
    position: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = ProtonDimens.Spacing.Large,
                vertical = ProtonDimens.Spacing.Medium
            )
    ) {
        Text(
            text = "$position",
            style = ProtonTheme.typography.bodySmall,
            color = ProtonTheme.colors.textHint,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(PositionColumnWidth)
        )
        Column {
            Text(
                text = event.name,
                style = ProtonTheme.typography.titleSmall,
                color = ProtonTheme.colors.textNorm
            )
            Text(
                text = event.measurementGroup,
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textWeak,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (expanded) {
                TelemetryDebugEntries(
                    label = stringResource(R.string.app_debug_screen_telemetry_event_dimensions),
                    entries = event.dimensions
                )
                TelemetryDebugEntries(
                    label = stringResource(R.string.app_debug_screen_telemetry_event_values),
                    entries = event.values
                )
            } else {
                Text(
                    // The copy hint rides on the first row only: repeated on every row it is noise.
                    text = stringResource(
                        if (position == 1) {
                            R.string.app_debug_screen_telemetry_event_summary_hint
                        } else {
                            R.string.app_debug_screen_telemetry_event_summary
                        },
                        event.dimensions.size,
                        event.values.size
                    ),
                    style = ProtonTheme.typography.bodySmall,
                    color = ProtonTheme.colors.textHint
                )
            }
        }
    }
}

@Composable
private fun TelemetryDebugEntries(label: String, entries: List<TelemetryDebugEntryUiModel>) {
    if (entries.isEmpty()) return

    Text(
        text = label,
        style = ProtonTheme.typography.bodySmall,
        color = ProtonTheme.colors.textHint,
        modifier = Modifier.padding(top = ProtonDimens.Spacing.Standard)
    )
    entries.forEach { entry ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.key,
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textWeak,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(KeyColumnWeight)
            )
            Text(
                text = entry.value,
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textNorm,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(ValueColumnWeight)
            )
        }
    }
}

private const val KeyColumnWeight = 0.45f
private const val ValueColumnWeight = 0.55f
private val PositionColumnWidth = 28.dp

internal object TelemetryDebugScreen {

    data class Actions(
        val onCaptureToggled: (Boolean) -> Unit,
        val onHaltSendingToggled: (Boolean) -> Unit,
        val onSearchQueryChanged: (String) -> Unit,
        val onClearCapturedEvents: () -> Unit,
        val onDiscardPendingQueue: () -> Unit,
        val onRefresh: () -> Unit
    )
}
