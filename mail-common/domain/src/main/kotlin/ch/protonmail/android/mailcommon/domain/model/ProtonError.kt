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

package ch.protonmail.android.mailcommon.domain.model

/**
 * Error related to Proton error codes
 * See the `/error-codes.html` page of API docs for further details
 */
sealed interface ProtonError {

    object Base64Format : ProtonError

    object PermissionDenied : ProtonError

    object InputInvalid : ProtonError

    object AttachmentTooLarge : ProtonError

    object NumOfRecipientsTooLarge : ProtonError

    object SendingLimitReached : ProtonError

    object AddressDoesNotExist : ProtonError

    object InsufficientScope : ProtonError

    object Banned : ProtonError

    object UploadFailure : ProtonError

    object PayloadTooLarge : ProtonError

    object PaidSubscriptionRequired : ProtonError

    object MessageUpdateDraftNotDraft : ProtonError

    object MessageValidateKeyNotAssociated : ProtonError

    object MessageSearchQuerySyntax : ProtonError

    object MessageAlreadySent : ProtonError

    object ExternalAddressSendDisabled : ProtonError

    object AttachmentUploadMessageAlreadySent : ProtonError

    /**
     * This object is not meant to be actively used.
     * Its purpose is to notify the logging tool that a case that should be handled
     * is not and to allow dedicated handling to be put in place.
     */
    object Unknown : ProtonError

    companion object
}
