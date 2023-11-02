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

package ch.protonmail.android.benchmark.common.extensions

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

internal fun UiDevice.findUiObjectByText(text: String) = findObject(UiSelector().text(text))

internal fun UiDevice.findUiObjectByResource(resId: String) = findObject(UiSelector().resourceId(resId))

internal fun UiDevice.findUiObjectByClassWithParent(childClass: Class<*>, parentResourceId: String) = findObject(
    UiSelector().resourceId(parentResourceId).childSelector(UiSelector().className(childClass))
)

internal fun UiDevice.waitUntilGone(resId: String, timeout: Long) = wait(Until.gone(By.res(resId)), timeout)
