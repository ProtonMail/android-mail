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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.ClearDataAction
import ch.protonmail.android.mailsettings.domain.repository.LocalStorageDataRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ClearLocalStorage @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val localStorageDataRepository: LocalStorageDataRepository
) {

    suspend operator fun invoke(clearDataAction: ClearDataAction) {
        val userId = observePrimaryUserId().firstOrNull() ?: return
        localStorageDataRepository.performClearData(userId, clearDataAction)
    }
}
