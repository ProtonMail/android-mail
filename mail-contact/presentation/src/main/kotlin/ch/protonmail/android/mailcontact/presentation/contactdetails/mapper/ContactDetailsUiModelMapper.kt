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

package ch.protonmail.android.mailcontact.presentation.contactdetails.mapper

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactDate
import ch.protonmail.android.mailcontact.domain.model.ContactDetailAddress
import ch.protonmail.android.mailcontact.domain.model.ContactDetailCard
import ch.protonmail.android.mailcontact.domain.model.ContactDetailEmail
import ch.protonmail.android.mailcontact.domain.model.ContactDetailTelephone
import ch.protonmail.android.mailcontact.domain.model.ContactField
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.ExtendedName
import ch.protonmail.android.mailcontact.domain.model.GenderKind
import ch.protonmail.android.mailcontact.domain.model.VCardPropType
import ch.protonmail.android.mailcontact.domain.model.VCardUrl
import ch.protonmail.android.mailcontact.domain.model.VCardUrlValue
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.AvatarUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemBadgeUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemGroupUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemType
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.HeaderUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.QuickActionType
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.QuickActionUiModel
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class ContactDetailsUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper
) {

    fun toUiModel(contactDetailCard: ContactDetailCard) = ContactDetailsUiModel(
        remoteId = contactDetailCard.remoteId,
        avatarUiModel = toAvatarUiModel(contactDetailCard.avatarInformation),
        headerUiModel = toHeaderUiModel(contactDetailCard.extendedName, contactDetailCard.fields.getPrimaryEmail()),
        quickActionUiModels = toQuickActionUiModels(contactDetailCard.fields),
        contactDetailsItemGroupUiModels = contactDetailCard.fields.map { toContactDetailsItemGroupUiModel(it) }
    )

    private fun toAvatarUiModel(avatarInformation: AvatarInformation) = AvatarUiModel.Initials(
        value = avatarInformation.initials,
        color = colorMapper.toColor(avatarInformation.color).getOrElse { Color.Unspecified }
    )

    private fun toHeaderUiModel(extendedName: ExtendedName, primaryEmail: String?) = HeaderUiModel(
        displayName = listOfNotNull(extendedName.first, extendedName.last).joinToString(" "),
        displayEmailAddress = primaryEmail
    )

    private fun toQuickActionUiModels(fields: List<ContactField>) = listOf(
        QuickActionUiModel(
            quickActionType = QuickActionType.Message,
            icon = R.drawable.ic_proton_pen_square,
            label = R.string.contact_details_quick_action_message,
            isEnabled = fields.hasEmailAddresses()
        ),
        QuickActionUiModel(
            quickActionType = QuickActionType.Call,
            icon = R.drawable.ic_proton_phone,
            label = R.string.contact_details_quick_action_call,
            isEnabled = fields.hasTelephoneNumbers()
        ),
        QuickActionUiModel(
            quickActionType = QuickActionType.Share,
            icon = R.drawable.ic_proton_arrow_up_from_square,
            label = R.string.contact_details_quick_action_share,
            isEnabled = true
        )
    )

    private fun toContactDetailsItemGroupUiModel(field: ContactField) = ContactDetailsItemGroupUiModel(
        contactDetailsItemUiModels = when (field) {
            is ContactField.Addresses -> field.list.map { it.toUiModel() }
            is ContactField.Anniversary -> listOf(field.date.toUiModel(R.string.contact_property_anniversary))
            is ContactField.Birthday -> listOf(field.date.toUiModel(R.string.contact_property_birthday))
            is ContactField.Emails -> field.list.map { it.toUiModel() }
            is ContactField.Gender -> listOf(field.type.toUiModel())
            is ContactField.Languages -> field.list.map { it.toUiModel(R.string.contact_property_language) }
            is ContactField.Logos -> emptyList() // To be implemented later
            is ContactField.Members -> field.list.map { it.toUiModel(R.string.contact_property_member) }
            is ContactField.Notes -> field.list.map { it.toUiModel(R.string.contact_property_note) }
            is ContactField.Organizations -> field.list.map { it.toUiModel(R.string.contact_property_organization) }
            is ContactField.Photos -> emptyList() // To be implemented later
            is ContactField.Roles -> field.list.map { it.toUiModel(R.string.contact_property_role) }
            is ContactField.Telephones -> field.list.map { it.toUiModel() }
            is ContactField.TimeZones -> field.list.map { it.toUiModel(R.string.contact_property_time_zone) }
            is ContactField.Titles -> field.list.map { it.toUiModel(R.string.contact_property_title) }
            is ContactField.Urls -> field.list.map { it.toUiModel() }
        }
    )

    private fun ContactDetailAddress.toUiModel() = ContactDetailsItemUiModel(
        contactDetailsItemType = ContactDetailsItemType.Other,
        label = this.addressTypes.firstOrNull()?.toTextUiModel()
            ?: TextUiModel.TextRes(R.string.contact_type_address),
        value = TextUiModel.Text(this.toFormattedAddress())
    )

    private fun ContactDate.toUiModel(@StringRes label: Int): ContactDetailsItemUiModel {
        val formattedDate = when (this) {
            is ContactDate.Date -> this.partialDate.toFormattedPartialDate().takeIfNotEmpty()?.let {
                TextUiModel.Text(it)
            } ?: TextUiModel.TextRes(R.string.contact_details_missing_date)
            is ContactDate.Text -> TextUiModel.Text(this.text)
        }

        return ContactDetailsItemUiModel(
            contactDetailsItemType = ContactDetailsItemType.Other,
            label = TextUiModel.TextRes(label),
            value = formattedDate
        )
    }

    private fun ContactDetailEmail.toUiModel() = ContactDetailsItemUiModel(
        contactDetailsItemType = ContactDetailsItemType.Email,
        label = this.emailType.firstOrNull()?.toTextUiModel()
            ?: TextUiModel.TextRes(R.string.contact_type_email),
        value = TextUiModel.Text(this.email),
        badges = this.groups.distinct().map { it.toUiModel() }
    )

    private fun ContactGroup.toUiModel() = ContactDetailsItemBadgeUiModel(
        name = this.name,
        color = colorMapper.toColor(this.color).getOrElse { Color.Unspecified }
    )

    private fun GenderKind.toUiModel(): ContactDetailsItemUiModel {
        val value = when (this) {
            is GenderKind.Custom -> TextUiModel.Text(this.value)
            is GenderKind.Female -> TextUiModel.TextRes(R.string.contact_gender_female)
            is GenderKind.Male -> TextUiModel.TextRes(R.string.contact_gender_male)
            is GenderKind.None -> TextUiModel.TextRes(R.string.contact_gender_none)
            is GenderKind.NotApplicable -> TextUiModel.TextRes(R.string.contact_gender_not_applicable)
            is GenderKind.Other -> TextUiModel.TextRes(R.string.contact_gender_other)
            is GenderKind.Unknown -> TextUiModel.TextRes(R.string.contact_gender_unknown)
        }

        return ContactDetailsItemUiModel(
            contactDetailsItemType = ContactDetailsItemType.Other,
            label = TextUiModel.TextRes(R.string.contact_property_gender),
            value = value
        )
    }

    private fun ContactDetailTelephone.toUiModel() = ContactDetailsItemUiModel(
        contactDetailsItemType = ContactDetailsItemType.Phone,
        label = this.telephoneTypes.firstOrNull()?.toTextUiModel()
            ?: TextUiModel.TextRes(R.string.contact_type_phone),
        value = TextUiModel.Text(this.number)
    )

    private fun String.toUiModel(@StringRes label: Int) = ContactDetailsItemUiModel(
        contactDetailsItemType = ContactDetailsItemType.Other,
        label = TextUiModel.TextRes(label),
        value = TextUiModel.Text(this)
    )

    private fun VCardUrl.toUiModel() = ContactDetailsItemUiModel(
        contactDetailsItemType = when (this.url) {
            is VCardUrlValue.Http -> ContactDetailsItemType.Url
            else -> ContactDetailsItemType.Other
        },
        label = this.urlTypes.firstOrNull()?.toTextUiModel()
            ?: TextUiModel.TextRes(R.string.contact_property_url),
        value = TextUiModel.Text(this.url.value)
    )

    private fun VCardPropType.toTextUiModel() = when (this) {
        is VCardPropType.Cell -> TextUiModel.TextRes(R.string.contact_type_mobile)
        is VCardPropType.Custom -> TextUiModel.Text(this.value)
        is VCardPropType.Fax -> TextUiModel.TextRes(R.string.contact_type_fax)
        is VCardPropType.Home -> TextUiModel.TextRes(R.string.contact_type_home)
        is VCardPropType.Pager -> TextUiModel.TextRes(R.string.contact_type_pager)
        is VCardPropType.Text -> TextUiModel.TextRes(R.string.contact_type_text)
        is VCardPropType.TextPhone -> TextUiModel.TextRes(R.string.contact_type_text_phone)
        is VCardPropType.Video -> TextUiModel.TextRes(R.string.contact_type_video)
        is VCardPropType.Voice -> TextUiModel.TextRes(R.string.contact_type_voice)
        is VCardPropType.Work -> TextUiModel.TextRes(R.string.contact_type_work)
    }
}
