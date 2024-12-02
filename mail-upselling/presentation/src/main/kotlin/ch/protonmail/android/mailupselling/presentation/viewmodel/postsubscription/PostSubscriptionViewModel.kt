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

package ch.protonmail.android.mailupselling.presentation.viewmodel.postsubscription

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import ch.protonmail.android.mailupselling.domain.repository.PostSubscriptionTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.AppUiModel
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.PostSubscriptionOperation
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.PostSubscriptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PostSubscriptionViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val postSubscriptionTelemetryRepository: PostSubscriptionTelemetryRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<PostSubscriptionState>(PostSubscriptionState.Loading)
    val state = mutableState.asStateFlow()

    init {
        val apps = listOf(
            toUiModel(PACKAGE_NAME_CALENDAR),
            toUiModel(PACKAGE_NAME_DRIVE),
            toUiModel(PACKAGE_NAME_VPN),
            toUiModel(PACKAGE_NAME_PASS)
        ).sortedBy { it.isInstalled }.toImmutableList()

        mutableState.tryEmit(
            PostSubscriptionState.Data(
                apps = apps
            )
        )
    }

    fun submit(operation: PostSubscriptionOperation) = when (operation) {
        is PostSubscriptionOperation.TrackTelemetryEvent -> handleTrackTelemetryEvent(operation)
    }

    private fun toUiModel(packageName: String): AppUiModel {
        return AppUiModel(
            packageName = packageName,
            logo = getLogoForPackage(packageName)!!,
            name = getNameForPackage(packageName)!!,
            message = getMessageForPackage(packageName)!!,
            isInstalled = isAppInstalled(packageName)
        )
    }

    private fun getLogoForPackage(packageName: String): Int? {
        return when (packageName) {
            PACKAGE_NAME_CALENDAR -> R.drawable.ic_logo_calendar
            PACKAGE_NAME_DRIVE -> R.drawable.ic_logo_drive
            PACKAGE_NAME_VPN -> R.drawable.ic_logo_vpn
            PACKAGE_NAME_PASS -> R.drawable.ic_logo_pass
            else -> null
        }
    }

    private fun getNameForPackage(packageName: String): Int? {
        return when (packageName) {
            PACKAGE_NAME_CALENDAR -> R.string.post_subscription_proton_calendar
            PACKAGE_NAME_DRIVE -> R.string.post_subscription_proton_drive
            PACKAGE_NAME_VPN -> R.string.post_subscription_proton_vpn
            PACKAGE_NAME_PASS -> R.string.post_subscription_proton_pass
            else -> null
        }
    }

    private fun getMessageForPackage(packageName: String): Int? {
        return when (packageName) {
            PACKAGE_NAME_CALENDAR -> R.string.post_subscription_proton_calendar_message
            PACKAGE_NAME_DRIVE -> R.string.post_subscription_proton_drive_message
            PACKAGE_NAME_VPN -> R.string.post_subscription_proton_vpn_message
            PACKAGE_NAME_PASS -> R.string.post_subscription_proton_pass_message
            else -> null
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            applicationContext.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (exception: PackageManager.NameNotFoundException) {
            Timber.d(exception)
            false
        }
    }

    private fun handleTrackTelemetryEvent(operation: PostSubscriptionOperation.TrackTelemetryEvent) {
        postSubscriptionTelemetryRepository.trackEvent(operation.eventType)
    }
}

internal const val PACKAGE_NAME_CALENDAR = "me.proton.android.calendar"
internal const val PACKAGE_NAME_DRIVE = "me.proton.android.drive"
internal const val PACKAGE_NAME_VPN = "ch.protonvpn.android"
internal const val PACKAGE_NAME_PASS = "proton.android.pass"
