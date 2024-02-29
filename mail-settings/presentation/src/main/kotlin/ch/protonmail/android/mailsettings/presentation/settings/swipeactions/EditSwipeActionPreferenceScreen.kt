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

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceViewModel.Action
import ch.protonmail.android.mailsettings.presentation.testdata.SwipeActionsTestData
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.util.kotlin.exhaustive
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@Composable
fun EditSwipeActionPreferenceScreen(
    direction: SwipeActionDirection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditSwipeActionPreferenceViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = viewModel.initial)

    EditSwipeActionPreferenceScreen(
        state = state,
        direction = direction,
        onBack = onBack,
        onSwipeActionSelect = { viewModel.submit(Action.UpdateSwipeAction(direction, it)) },
        modifier = modifier
    )
}

@Composable
fun EditSwipeActionPreferenceScreen(
    state: EditSwipeActionPreferenceState,
    direction: SwipeActionDirection,
    onBack: () -> Unit,
    onSwipeActionSelect: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (direction) {
        SwipeActionDirection.RIGHT -> string.mail_settings_swipe_right_name
        SwipeActionDirection.LEFT -> string.mail_settings_swipe_left_name
    }
    Scaffold(topBar = { ProtonSettingsTopBar(title = stringResource(id = title), onBackClick = onBack) }) {
        when (state) {
            is EditSwipeActionPreferenceState.Data -> EditSwipeActionPreferenceContent(
                modifier = modifier.padding(it),
                items = state.items,
                onSwipeActionSelect = onSwipeActionSelect
            )
            EditSwipeActionPreferenceState.Loading -> ProtonCenteredProgress()
            EditSwipeActionPreferenceState.NotLoggedIn ->
                ProtonErrorMessage(errorMessage = stringResource(id = commonString.x_error_not_logged_in))
        }.exhaustive
    }
}

@Composable
fun EditSwipeActionPreferenceContent(
    items: List<EditSwipeActionPreferenceItemUiModel>,
    onSwipeActionSelect: (SwipeAction) -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(top = ProtonDimens.SmallSpacing)) {
        items(items) {
            ListItem(item = it, onSwipeActionSelect = onSwipeActionSelect)
        }
    }
}

@Composable
private fun ListItem(item: EditSwipeActionPreferenceItemUiModel, onSwipeActionSelect: (SwipeAction) -> Unit) {

    Row(
        modifier = Modifier
            .selectable(selected = item.isSelected, role = Role.RadioButton) { onSwipeActionSelect(item.swipeAction) }
            .padding(ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(ProtonDimens.DefaultIconSize),
            painter = painterResource(id = item.imageRes),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
        Text(
            modifier = Modifier.weight(1f, fill = true),
            text = stringResource(id = item.descriptionRes),
            style = ProtonTheme.typography.default
        )
        RadioButton(selected = item.isSelected, onClick = null)
    }
}

object EditSwipeActionPreferenceScreen {

    const val SWIPE_DIRECTION_KEY = "swipe action direction"
}

@Composable
@Preview(showBackground = true)
private fun ListItemPreview() {
    val item = SwipeActionsTestData.Edit.buildItem(SwipeAction.MarkRead, isSelected = true)
    ProtonTheme {
        ListItem(item = item, onSwipeActionSelect = {})
    }
}

@Composable
@Preview(showBackground = true)
private fun EditSwipeActionPreferenceContentPreview() {
    val items = SwipeActionsTestData.Edit.buildAllItems(selected = SwipeAction.Spam)
    ProtonTheme {
        EditSwipeActionPreferenceContent(modifier = Modifier, items = items, onSwipeActionSelect = {})
    }
}

@Composable
@Preview(showBackground = true)
private fun EditSwipeActionPreferenceScreenPreview() {
    val items = SwipeActionsTestData.Edit.buildAllItems(selected = SwipeAction.Trash)
    ProtonTheme {
        EditSwipeActionPreferenceScreen(
            state = EditSwipeActionPreferenceState.Data(items),
            direction = SwipeActionDirection.RIGHT,
            onBack = {},
            onSwipeActionSelect = {}
        )
    }
}
