/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.websettings

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import ch.protonmail.android.mailsettings.presentation.websettings.UpsellJsHelper.UpsellFoldersActionName
import ch.protonmail.android.mailsettings.presentation.websettings.UpsellJsHelper.UpsellLabelsActionName
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility

@Keep
class WebSettingsJavaScriptInterface(
    private val onUpsell: (UpsellingEntryPoint.Feature, UpsellingVisibility) -> Unit,
    private val upsellingVisibility: UpsellingVisibility
) {

    @JavascriptInterface
    fun postMessage(message: String) {
        // Enforce the navigation to run on the Main thread
        Handler(Looper.getMainLooper()).post {
            when (message) {
                UpsellLabelsActionName ->
                    onUpsell(UpsellingEntryPoint.Feature.Folders, upsellingVisibility)

                UpsellFoldersActionName ->
                    onUpsell(UpsellingEntryPoint.Feature.Folders, upsellingVisibility)
            }
        }
    }
}

object UpsellJsHelper {

    const val UpsellInterfaceName = "UpsellInterface"

    const val UpsellLabelsActionName = "labels-action"
    const val UpsellFoldersActionName = "folders-action"

    val upsellJavascriptCode = """
        (function() {
            // Create webkit messageHandlers compatibility for Android
            if (!window.webkit) {
                window.webkit = {
                    messageHandlers: {
                        upsell: {
                            postMessage: function(message) {
                                if (window.$UpsellInterfaceName) {
                                    window.$UpsellInterfaceName.postMessage(message);
                                }
                            }
                        }
                    }
                };
            }
        })();
    """.trimIndent()
}
