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

package ch.protonmail.android.mailfeatureflags.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonOutlinedTextField
import ch.protonmail.android.design.compose.component.ProtonSettingsTopBar
import ch.protonmail.android.design.compose.component.ProtonSwitch
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailfeatureflags.presentation.R
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagListItem
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagOverridesState
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagUiModel
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeatureFlagOverridesScreenContent(
    state: FeatureFlagOverridesState.Loaded,
    actions: FeatureFlagOverridesScreen.Actions
) {
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val visibleItems = remember(state.featureFlagListItems, query.text) {
        state.featureFlagListItems.filterByQuery(query.text)
    }
    val hasOverrides = remember(state.featureFlagListItems) {
        state.featureFlagListItems.any { it is FeatureFlagListItem.FeatureFlag && it.model.overridden }
    }

    Scaffold(
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(R.string.feature_flag_overrides_title),
                onBackClick = actions.onBack,
                actions = {
                    if (hasOverrides) {
                        TextButton(onClick = actions.onResetAll) {
                            Text(text = stringResource(R.string.feature_flag_overrides_reset_action))
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .imePadding()
        ) {
            SearchField(value = query, onValueChange = { query = it })

            if (visibleItems.isEmpty()) {
                NoResults()
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(
                        items = visibleItems,
                        key = { item ->
                            when (item) {
                                is FeatureFlagListItem.Header -> "header-${item.categoryName}"
                                is FeatureFlagListItem.FeatureFlag -> "flag-${item.model.key}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is FeatureFlagListItem.Header -> SectionLabel(item.categoryName)
                            is FeatureFlagListItem.FeatureFlag -> FeatureFlagRow(
                                model = item.model,
                                onToggle = { actions.onToggled(item.model.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Keeps the flags whose name, description or key contains [query], plus the headers that still have
 * a flag under them. A blank query returns the list untouched.
 */
private fun List<FeatureFlagListItem>.filterByQuery(query: String): List<FeatureFlagListItem> {
    if (query.isBlank()) return this

    return buildList {
        var lastHeader: FeatureFlagListItem.Header? = null

        this@filterByQuery.forEach { item ->
            when (item) {
                is FeatureFlagListItem.Header -> lastHeader = item
                is FeatureFlagListItem.FeatureFlag -> if (item.model.matches(query)) {
                    lastHeader?.let { add(it) }
                    lastHeader = null
                    add(item)
                }
            }
        }
    }
}

private fun FeatureFlagUiModel.matches(query: String) = name.contains(query, ignoreCase = true) ||
    description.contains(query, ignoreCase = true) ||
    key.contains(query, ignoreCase = true)

@Composable
private fun SearchField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    ProtonOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ProtonDimens.Spacing.Large,
                vertical = ProtonDimens.Spacing.Standard
            ),
        placeholder = {
            Text(
                text = stringResource(R.string.feature_flag_overrides_search_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.Medium),
                painter = painterResource(R.drawable.ic_proton_magnifier),
                contentDescription = null,
                tint = ProtonTheme.colors.iconHint
            )
        },
        trailingIcon = {
            if (value.text.isNotEmpty()) {
                IconButton(onClick = { onValueChange(TextFieldValue()) }) {
                    Icon(
                        modifier = Modifier.size(ProtonDimens.IconSize.Medium),
                        painter = painterResource(R.drawable.ic_proton_cross_small),
                        contentDescription = null,
                        tint = ProtonTheme.colors.iconHint
                    )
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        modifier = Modifier.padding(
            start = ProtonDimens.Spacing.Large,
            end = ProtonDimens.Spacing.Large,
            top = ProtonDimens.Spacing.Large,
            bottom = ProtonDimens.Spacing.Standard
        ),
        text = title,
        color = ProtonTheme.colors.textHint,
        style = ProtonTheme.typography.bodyMedium
    )
}

@Composable
private fun FeatureFlagRow(
    model: FeatureFlagUiModel,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = model.enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() }
                )
                .padding(
                    horizontal = ProtonDimens.Spacing.Large,
                    vertical = ProtonDimens.Spacing.Medium
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = ProtonDimens.Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
            ) {
                Text(
                    text = model.name,
                    color = ProtonTheme.colors.textNorm,
                    style = ProtonTheme.typography.titleMedium
                )
                Text(
                    text = model.description,
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.bodyMedium
                )
                // Kept on its own line so a long name never has to share the row with it.
                if (model.overridden) {
                    Text(
                        text = stringResource(R.string.feature_flag_overridden_badge),
                        color = ProtonTheme.colors.notificationWarning,
                        style = ProtonTheme.typography.bodySmall
                    )
                }
            }

            ProtonSwitch(checked = model.enabled, onCheckedChange = { onToggle() })
        }
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
    }
}

@Composable
private fun NoResults() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ProtonDimens.Spacing.ExtraLarge),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.feature_flag_overrides_no_results),
            color = ProtonTheme.colors.textHint,
            style = ProtonTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
private fun FeatureFlagScreenPreview() {
    ProtonTheme {
        FeatureFlagOverridesScreenContent(
            state = sampleData,
            actions = FeatureFlagOverridesScreen.Actions.Empty
        )
    }
}

val sampleData = FeatureFlagOverridesState.Loaded(
    featureFlagListItems = listOf(
        FeatureFlagListItem.Header("Section 1"),
        FeatureFlagListItem.FeatureFlag(
            FeatureFlagUiModel(
                key = "key-1",
                name = "Feature 1",
                description = "Some description.",
                enabled = false,
                overridden = false
            )
        ),
        FeatureFlagListItem.FeatureFlag(
            FeatureFlagUiModel(
                key = "key-2",
                name = "Feature 2",
                description = "Some other description.",
                enabled = true,
                overridden = false
            )
        ),
        FeatureFlagListItem.Header("Section 2"),
        FeatureFlagListItem.FeatureFlag(
            FeatureFlagUiModel(
                key = "key-3",
                name = "Overridden",
                description = "An overridden FF.",
                enabled = false,
                overridden = true
            )
        )
    ).toImmutableList()
)
