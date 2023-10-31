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

package ch.protonmail.android.maillabel.presentation.labellist

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.R
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.label.domain.entity.LabelId

@Composable
fun LabelListScreen(actions: LabelListScreen.Actions, viewModel: LabelListViewModel = hiltViewModel()) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = viewModel.initialState
        ).value
    ) {
        is LabelListState.Data -> {
            LabelListScreen(
                state = state,
                actions = actions
            )
        }
        is LabelListState.EmptyLabelList -> {
            EmptyLabelListScreen(
                actions = actions
            )
        }
        is LabelListState.Loading -> { }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LabelListScreen(state: LabelListState.Data, actions: LabelListScreen.Actions) {
    Scaffold(
        topBar = {
            LabelListTopBar(
                isAddLabelButtonVisible = true,
                actions
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .selectableGroup()
                    .padding(
                        PaddingValues(
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            top = paddingValues.calculateTopPadding() + ProtonDimens.SmallSpacing,
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = paddingValues.calculateBottomPadding()
                        )
                    )
            ) {
                items(state.mailLabels) { mailLabel ->
                    Column(
                        modifier = Modifier
                            .animateItemPlacement()
                            .selectable(selected = false) {
                                actions.onLabelSelected(mailLabel)
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
                                painter = painterResource(id = R.drawable.ic_proton_circle_filled),
                                tint = Color(mailLabel.color),
                                contentDescription = NO_CONTENT_DESCRIPTION
                            )
                            Text(
                                text = mailLabel.text,
                                modifier = Modifier.padding(
                                    start = ProtonDimens.ExtraSmallSpacing,
                                    end = ProtonDimens.DefaultSpacing
                                ),
                                style = ProtonTheme.typography.defaultNorm
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    )
}

@Composable
fun EmptyLabelListScreen(actions: LabelListScreen.Actions) {
    Scaffold(
        topBar = {
            LabelListTopBar(
                isAddLabelButtonVisible = false,
                actions
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier
                        .padding(start = ProtonDimens.ExtraSmallSpacing)
                        .background(
                            color = ProtonTheme.colors.backgroundSecondary,
                            shape = RoundedCornerShape(MailDimens.IconWeakRoundBackgroundRadius)
                        )
                        .padding(
                            horizontal = ProtonDimens.SmallSpacing,
                            vertical = ProtonDimens.SmallSpacing
                        ),
                    painter = painterResource(id = R.drawable.ic_proton_tag_plus),
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
                Text(
                    stringResource(R.string.label_list_no_labels_found),
                    Modifier.padding(
                        start = ProtonDimens.LargeSpacing,
                        top = ProtonDimens.MediumSpacing,
                        end = ProtonDimens.LargeSpacing
                    ),
                    style = ProtonTheme.typography.defaultStrongNorm
                )
                Text(
                    stringResource(R.string.label_list_create_label_placeholder_description),
                    Modifier.padding(
                        start = ProtonDimens.LargeSpacing,
                        top = MailDimens.TinySpacing,
                        end = ProtonDimens.LargeSpacing
                    ),
                    style = ProtonTheme.typography.defaultSmallWeak
                )
                ProtonSecondaryButton(
                    modifier = Modifier.padding(top = ProtonDimens.LargeSpacing),
                    onClick = actions.onAddLabelClick
                ) {
                    Text(
                        text = stringResource(R.string.label_title_add_label),
                        Modifier.padding(
                            start = ProtonDimens.SmallSpacing,
                            end = ProtonDimens.SmallSpacing
                        ),
                        style = ProtonTheme.typography.captionNorm
                    )
                }
            }
        }
    )
}

@Composable
fun LabelListTopBar(isAddLabelButtonVisible: Boolean, actions: LabelListScreen.Actions) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                stringResource(id = R.string.label_title_labels)
            )
        },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        },
        actions = {
            if (isAddLabelButtonVisible) {
                IconButton(onClick = actions.onAddLabelClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_plus),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.add_label_content_description)
                    )
                }
            }
        }
    )
}

object LabelListScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onLabelSelected: (MailLabel) -> Unit,
        val onAddLabelClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onLabelSelected = {},
                onAddLabelClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun LabelListScreenPreview() {
    LabelListScreen(
        state = LabelListState.Data(
            mailLabels = listOf(
                buildSampleMailLabelCustom(
                    name = "Label 1",
                    color = colorResource(id = R.color.chambray).toArgb()
                ),
                buildSampleMailLabelCustom(
                    name = "Label 2",
                    color = colorResource(id = R.color.goblin).toArgb()
                ),
                buildSampleMailLabelCustom(
                    name = "Label 3",
                    color = colorResource(id = R.color.copper_intense).toArgb()
                )
            )
        ),
        actions = LabelListScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyLabelListScreenPreview() {
    EmptyLabelListScreen(
        actions = LabelListScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun LabelListTopBarPreview() {
    LabelListTopBar(
        isAddLabelButtonVisible = true,
        actions = LabelListScreen.Actions(
            onBackClick = {},
            onLabelSelected = {},
            onAddLabelClick = {}
        )
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyLabelListTopBarPreview() {
    LabelListTopBar(
        isAddLabelButtonVisible = false,
        actions = LabelListScreen.Actions(
            onBackClick = {},
            onLabelSelected = {},
            onAddLabelClick = {}
        )
    )
}

private fun buildSampleMailLabelCustom(name: String, color: Int): MailLabel.Custom {
    return MailLabel.Custom(
        id = MailLabelId.Custom.Label(LabelId("id$name")),
        text = name,
        color = color,
        parent = null,
        isExpanded = false,
        level = 0,
        order = 0,
        children = listOf()
    )
}
