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

package ch.protonmail.android.mailcommon.data.file

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a [Uri] received from an inbound share intent may be dereferenced.
 *
 * Only `content://` URIs served by a *foreign* authority are accepted:
 * - other schemes (`file://`, `android.resource://`) are rejected, as [ContentResolver.openInputStream] resolves
 *   those against the filesystem/our own package rather than against a provider that could have granted access;
 * - any authority declared by this package is rejected outright.
 *
 * This only applies at the intent boundary. URIs the app produces for itself (camera capture, SAF picker results,
 * the aggregated logs zip attached from the debug screen) never pass through here, so legitimate
 * self-sharing is handled in process rather than by relaxing this check.
 */
@Singleton
class ShareUriValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val ownAuthorities: Set<String> by lazy { resolveOwnAuthorities() }

    fun isAllowed(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme != ContentResolver.SCHEME_CONTENT) {
            Timber.w("share-uri-validator: rejected shared uri with non-content scheme '$scheme'")
            return false
        }

        val authority = uri.authority?.lowercase()
        val rejectionReason = when {
            authority.isNullOrEmpty() -> "it carries no authority"
            authority in ownAuthorities -> "it targets our own authority '$authority'"
            else -> null
        }

        if (rejectionReason != null) {
            Timber.w("share-uri-validator: rejected shared uri because $rejectionReason")
            return false
        }

        return true
    }

    fun filterAllowed(uris: List<Uri>): List<Uri> = uris.filter { isAllowed(it) }

    private fun resolveOwnAuthorities(): Set<String> {
        val declared = runCatching { queryDeclaredProviderAuthorities() }.getOrElse {
            Timber.e(it, "share-uri-validator: could not enumerate own providers, using the known authorities only")
            emptySet()
        }

        // The FileProvider authority is denied even if the package manager lookup failed above,
        // since that is the provider holding the app's private files.
        return declared + "${context.packageName}$FileProviderAuthoritySuffix".lowercase()
    }

    private fun queryDeclaredProviderAuthorities(): Set<String> {
        val flags = PackageManager.GET_PROVIDERS or PackageManager.MATCH_DISABLED_COMPONENTS
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags)
        }

        // A single provider may declare several authorities, separated by semicolons.
        return (packageInfo.providers ?: emptyArray())
            .flatMap { it.authority?.split(AuthoritySeparator).orEmpty() }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private companion object {

        const val AuthoritySeparator = ";"
        const val FileProviderAuthoritySuffix = ".provider"
    }
}
