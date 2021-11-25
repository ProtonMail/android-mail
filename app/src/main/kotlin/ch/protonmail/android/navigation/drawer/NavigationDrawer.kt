/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.navigation.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue.Open
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import ch.protonmail.android.R
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.presentation.view.AccountPrimaryView

@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    navigateToSigningOut: () -> Unit,
    onAccountViewAdded: (AccountPrimaryView) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier) {
            AndroidView(
                factory = { context ->
                    AccountPrimaryView(context)
                },
                modifier = Modifier.fillMaxWidth(),
                update = onAccountViewAdded
            )

            Button(
                onClick = navigateToSigningOut,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(id = R.string.logout)) }
        }
    }
}

@Preview
@Composable
fun previewNavigationDrawer() {
    NavigationDrawer(
        drawerState = DrawerState(Open),
        navigateToSigningOut = {},
        onAccountViewAdded = {}
    )
}
