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

package ch.protonmail.android.mailsidebar.presentation.usecase

import ch.protonmail.android.mailupselling.domain.annotations.SidebarUpsellingEnabled
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveFreeUserClickUpsellingVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import javax.inject.Inject

class ObserveSidebarUpsellingVisibility @Inject constructor(
    private val shouldUpgradeStorage: ShouldUpgradeStorage,
    private val observeFreeUserUpsellingVisibility: ObserveFreeUserClickUpsellingVisibility,
    @SidebarUpsellingEnabled private val isSidebarUpsellingEnabled: Boolean
) {

    operator fun invoke(): Flow<Boolean> {
        if (!isSidebarUpsellingEnabled) return flowOf(false)
        return shouldUpgradeStorage()
            .filterIsInstance<ShouldUpgradeStorage.Result.NoUpgrade>()
            .flatMapLatest {
                observeFreeUserUpsellingVisibility()
            }
            .onStart { emit(false) }
            .distinctUntilChanged()
    }
}
