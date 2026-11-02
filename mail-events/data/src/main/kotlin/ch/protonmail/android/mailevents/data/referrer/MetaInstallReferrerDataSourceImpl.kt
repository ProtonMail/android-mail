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

package ch.protonmail.android.mailevents.data.referrer

import android.content.Context
import android.database.Cursor
import android.net.Uri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.MetaInstallReferrer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Instant

class MetaInstallReferrerDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : MetaInstallReferrerDataSource {

    override suspend fun getMetaInstallReferrer(): Either<DataError, MetaInstallReferrer> = withContext(ioDispatcher) {
        PROVIDER_AUTHORITIES.firstNotNullOfOrNull { authority -> queryProvider(authority) }?.right()
            ?: DataError.Local.NotFound.left()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun queryProvider(authority: String): MetaInstallReferrer? = try {
        val uri = Uri.parse("content://$authority/$FACEBOOK_APP_ID")
        context.contentResolver.query(uri, PROJECTION, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toMetaInstallReferrer() else null
        }
    } catch (e: Exception) {
        Timber.d(e, "Failed to query Meta install referrer from $authority")
        null
    }

    private fun Cursor.toMetaInstallReferrer(): MetaInstallReferrer? {
        val installReferrer = getString(getColumnIndexOrThrow(Columns.INSTALL_REFERRER)) ?: return null
        return MetaInstallReferrer(
            installReferrer = installReferrer,
            isClickThrough = getInt(getColumnIndexOrThrow(Columns.IS_CLICK_THROUGH)) == 1,
            actualTimestamp = Instant.fromEpochSeconds(getLong(getColumnIndexOrThrow(Columns.ACTUAL_TIMESTAMP)))
        )
    }

    // Provider names and columns from https://developers.facebook.com/docs/app-ads/meta-install-referrer/
    private companion object {

        const val FACEBOOK_APP_ID = "1444920889910716"

        val PROJECTION = arrayOf(Columns.INSTALL_REFERRER, Columns.IS_CLICK_THROUGH, Columns.ACTUAL_TIMESTAMP)

        val PROVIDER_AUTHORITIES = listOf(Providers.FACEBOOK, Providers.INSTAGRAM, Providers.FACEBOOK_LITE)

        object Columns {

            const val INSTALL_REFERRER = "install_referrer"
            const val IS_CLICK_THROUGH = "is_ct"
            const val ACTUAL_TIMESTAMP = "actual_timestamp"
        }

        object Providers {

            const val FACEBOOK = "com.facebook.katana.provider.InstallReferrerProvider"
            const val INSTAGRAM = "com.instagram.contentprovider.InstallReferrerProvider"
            const val FACEBOOK_LITE = "com.facebook.lite.provider.InstallReferrerProvider"
        }
    }
}
