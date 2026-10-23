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

package ch.protonmail.android.mailsession.domain.model

import me.proton.core.domain.entity.UserId

data class User(
    val userId: UserId,
    val displayName: String?,
    val email: String,
    val name: String?,
    val services: Int,
    val subscribed: Int,
    val delinquent: Int,
    val createTimeUtc: Long,
    val isOrganizationMember: Boolean,
    private val usedSpace: Long,
    private val maxSpace: Long
) {

    val usagePercent: Percent = if (maxSpace > 0) {
        Percent(usedSpace.toDouble() / maxSpace.toDouble() * 100.0)
    } else {
        Percent(0.0)
    }

    val maxStorage: Storage = Storage.fromBytes(maxSpace)
}

fun User.hasService(): Boolean = services > 0
fun User.hasSubscription(): Boolean = subscribed > 0

fun User.hasServiceForMail(): Boolean = hasServiceFor(USER_SERVICE_MASK_MAIL)
fun User.hasServiceForVpn(): Boolean = hasServiceFor(USER_SERVICE_MASK_VPN)
fun User.hasServiceForDrive(): Boolean = hasServiceFor(USER_SERVICE_MASK_DRIVE)

fun User.hasSubscriptionForMail(): Boolean = hasSubscriptionFor(USER_SERVICE_MASK_MAIL)
fun User.hasSubscriptionForVpn(): Boolean = hasSubscriptionFor(USER_SERVICE_MASK_VPN)
fun User.hasSubscriptionForDrive(): Boolean = hasSubscriptionFor(USER_SERVICE_MASK_DRIVE)

private const val USER_SERVICE_MASK_MAIL = 1 // 0001
private const val USER_SERVICE_MASK_DRIVE = 2 // 0010
private const val USER_SERVICE_MASK_VPN = 4 // 0100

private fun User.hasServiceFor(mask: Int): Boolean = mask.and(services) == mask
private fun User.hasSubscriptionFor(mask: Int): Boolean = mask.and(subscribed) == mask
