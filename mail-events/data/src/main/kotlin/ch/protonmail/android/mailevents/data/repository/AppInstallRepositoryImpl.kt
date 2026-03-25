/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailevents.data.repository

import android.content.Context
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.data.referrer.InstallReferrerDataSource
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import ch.protonmail.android.mailevents.domain.repository.AppInstallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppInstallRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installReferrerDataSource: InstallReferrerDataSource
) : AppInstallRepository {

    private val packageInfo by lazy {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
    }

    override fun getFirstInstallTime(): Long = packageInfo.firstInstallTime

    override fun getLastUpdateTime(): Long = packageInfo.lastUpdateTime

    override suspend fun getInstallReferrer(): Either<DataError, InstallReferrer> =
        installReferrerDataSource.getInstallReferrer()
}
