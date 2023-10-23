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

package ch.protonmail.android.mailnotifications

import android.app.Notification

internal val Notification.title: String? get() = extras.getString("android.title")

internal val Notification.summaryText: String? get() = extras.getString("android.summaryText")

internal val Notification.text: String? get() = extras.getString("android.text")

internal val Notification.bigText: String? get() = this.extras.getString("android.bigText")

internal val Notification.subText: String? get() = this.extras.getString("android.subText")
