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

package ch.protonmail.android.maildetail.presentation.mapper

import android.content.Context
import androidx.core.content.FileProvider
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import androidx.annotation.VisibleForTesting
import ch.protonmail.android.mailmessage.presentation.ui.WEB_VIEW_SAFE_PHYSICAL_MAX_HEIGHT_PX
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

class MessageBodyContentUiModelMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val coroutineDispatcher: CoroutineDispatcher
) {
    private var densityProvider: () -> Float = { context.resources.displayMetrics.density }

    @VisibleForTesting
    internal constructor(
        context: Context,
        coroutineDispatcher: CoroutineDispatcher,
        densityProvider: () -> Float
    ) : this(context, coroutineDispatcher) {
        this.densityProvider = densityProvider
    }

    suspend fun toUiContent(
        body: String,
        messageId: MessageId,
        shouldRestrictHeight: Boolean
    ): MessageBodyContent {
        val finalBody = applyHeightRestrictionIfNeeded(body, shouldRestrictHeight)

        if (finalBody.length < LARGE_BODY_THRESHOLD_CHARS) {
            return MessageBodyContent.Text(finalBody)
        }

        return runCatching {
            withContext(coroutineDispatcher) {
                Timber.d(
                    "message-body-ui-mapper: Caching body content to file due to size (${finalBody.length} chars)"
                )
                val bodyCacheDir = File(context.cacheDir, BODY_CACHE_DIRECTORY).apply { mkdirs() }
                val file = File(bodyCacheDir, "message_body_${messageId.id}.html")
                file.writeText(finalBody, Charsets.UTF_8)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

                MessageBodyContent.File(contentUri)
            }
        }.getOrElse {
            Timber.e(it, "message-body-ui-mapper: Failed to cache body for $messageId")
            MessageBodyContent.Text(finalBody)
        }
    }

    private fun applyHeightRestrictionIfNeeded(body: String, shouldRestrictHeight: Boolean): String {
        if (!shouldRestrictHeight) return body

        Timber.d("message-body-ui-mapper: Applying height restriction to body content")
        // Convert the safe physical cap into CSS pixels for the injected style. Lower-density devices
        // end up with a larger preview window while high-density devices stay under the GPU texture
        // limit. physical_px = css_px * density, so css_px = physical_px / density.
        val density = densityProvider().coerceAtLeast(1f)
        val cssMaxHeightPx = (WEB_VIEW_SAFE_PHYSICAL_MAX_HEIGHT_PX / density).toInt()
        return heightRestrictionStyle(cssMaxHeightPx) + body
    }

    // Scoped to `body` only: overflow:hidden on `html` makes Android WebView report a measured height
    // of 0 (it uses documentElement for its scroll range), which then suppresses the page-ready signal
    // so the message card never reveals.
    private fun heightRestrictionStyle(cssMaxHeightPx: Int): String =
        "<style>body{max-height:${cssMaxHeightPx}px !important;overflow:hidden !important;}</style>"

    companion object {
        @VisibleForTesting
        internal const val LARGE_BODY_THRESHOLD_CHARS = 1024 * 1024
        private const val BODY_CACHE_DIRECTORY = "body_cache"
    }
}

