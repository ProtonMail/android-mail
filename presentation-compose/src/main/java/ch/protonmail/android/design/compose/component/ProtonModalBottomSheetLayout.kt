/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.design.compose.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import ch.protonmail.android.design.compose.theme.ProtonTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtonModalBottomSheetLayout(
    showBottomSheet: Boolean,
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: SheetState,
    onDismissed: () -> Unit,
    dismissOnBack: Boolean,
    contentWindowInsets: @Composable () -> WindowInsets = {
        WindowInsets.systemBars.only(WindowInsetsSides.Top)
    },
    shape: Shape = ProtonTheme.shapes.bottomSheet,
    containerColor: Color = ProtonTheme.colors.backgroundInvertedNorm,
    // Null when the sheet content draws its own handle, e.g. to show it on a custom background.
    dragHandle: (@Composable () -> Unit)? = ProtonModalBottomSheetDefaults.DragHandle,
    content: @Composable () -> Unit
) {
    content()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissed,
            sheetState = sheetState,
            shape = shape,
            containerColor = containerColor,
            contentColor = ProtonTheme.colors.textNorm,
            dragHandle = dragHandle,
            content = sheetContent,
            contentWindowInsets = contentWindowInsets,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissOnBack)
        )
    }
}

object ProtonModalBottomSheetDefaults {

    @OptIn(ExperimentalMaterial3Api::class)
    val DragHandle: @Composable () -> Unit = {
        BottomSheetDefaults.DragHandle(
            modifier = Modifier.semantics {
                this.role = Role.Companion.Image
                this.contentDescription = ""
            },
            color = ProtonTheme.colors.iconHint
        )
    }
}
