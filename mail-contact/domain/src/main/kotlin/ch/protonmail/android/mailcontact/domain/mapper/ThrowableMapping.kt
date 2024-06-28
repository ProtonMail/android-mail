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

package ch.protonmail.android.mailcontact.domain.mapper

import me.proton.core.network.domain.hasProtonErrorCode

fun Throwable.isAlreadyExistsApiError() = this.hasProtonErrorCode(PROTON_RESPONSE_CODE_ALREADY_EXISTS)

fun Throwable.isContactLimitReachedApiError() = this.hasProtonErrorCode(PROTON_RESPONSE_CODE_CONTACT_LIMIT_REACHED)

const val PROTON_RESPONSE_CODE_ALREADY_EXISTS = 2500

@Suppress("VariableMaxLength")
const val PROTON_RESPONSE_CODE_CONTACT_LIMIT_REACHED = 2024
