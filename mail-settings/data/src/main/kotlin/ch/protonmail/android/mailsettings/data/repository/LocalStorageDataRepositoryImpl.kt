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

package ch.protonmail.android.mailsettings.data.repository

import androidx.work.Constraints
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailsettings.data.local.ClearLocalDataWorker
import ch.protonmail.android.mailsettings.domain.model.ClearDataAction
import ch.protonmail.android.mailsettings.domain.repository.LocalStorageDataRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class LocalStorageDataRepositoryImpl @Inject constructor(
    private val enqueuer: Enqueuer
) : LocalStorageDataRepository {

    override fun performClearData(userId: UserId, clearDataAction: ClearDataAction) {
        enqueuer.enqueueUniqueWork<ClearLocalDataWorker>(
            userId = userId,
            params = ClearLocalDataWorker.params(clearDataAction),
            workerId = ClearLocalDataWorker.id(clearDataAction),
            constraints = Constraints.Builder().build() // No specific constraints.
        )
    }
}
