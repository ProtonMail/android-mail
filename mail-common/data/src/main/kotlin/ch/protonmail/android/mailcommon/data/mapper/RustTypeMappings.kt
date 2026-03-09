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

package ch.protonmail.android.mailcommon.data.mapper

import uniffi.mail_uniffi.AlmostAllMail
import uniffi.mail_uniffi.AppProtection
import uniffi.mail_uniffi.AppSettings
import uniffi.mail_uniffi.AppSettingsDiff
import uniffi.mail_uniffi.AttachmentData
import uniffi.mail_uniffi.AttachmentDataError
import uniffi.mail_uniffi.AttachmentMetadata
import uniffi.mail_uniffi.AttachmentMimeType
import uniffi.mail_uniffi.AutoDeleteBanner
import uniffi.mail_uniffi.AutoDeleteState
import uniffi.mail_uniffi.AutoLock
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.ComposerDirection
import uniffi.mail_uniffi.ComposerMode
import uniffi.mail_uniffi.ComposerRecipient
import uniffi.mail_uniffi.ContactDate
import uniffi.mail_uniffi.ContactDetailAddress
import uniffi.mail_uniffi.ContactDetailCard
import uniffi.mail_uniffi.ContactDetailsEmail
import uniffi.mail_uniffi.ContactDetailsTelephones
import uniffi.mail_uniffi.ContactEmailItem
import uniffi.mail_uniffi.ContactField
import uniffi.mail_uniffi.ContactGroup
import uniffi.mail_uniffi.ContactItem
import uniffi.mail_uniffi.ContactItemType
import uniffi.mail_uniffi.ContactSuggestion
import uniffi.mail_uniffi.Conversation
import uniffi.mail_uniffi.DecryptedAttachment
import uniffi.mail_uniffi.DeviceContact
import uniffi.mail_uniffi.Disposition
import uniffi.mail_uniffi.DraftSendResult
import uniffi.mail_uniffi.ExclusiveLocation
import uniffi.mail_uniffi.ExtendedName
import uniffi.mail_uniffi.GenderKind
import uniffi.mail_uniffi.GroupedContacts
import uniffi.mail_uniffi.HiddenMessagesBanner
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.IssueReport
import uniffi.mail_uniffi.LabelAsAction
import uniffi.mail_uniffi.MailScrollerError
import uniffi.mail_uniffi.MailSettings
import uniffi.mail_uniffi.Message
import uniffi.mail_uniffi.MessageBanner
import uniffi.mail_uniffi.MessageButtons
import uniffi.mail_uniffi.MimeType
import uniffi.mail_uniffi.MimeTypeCategory
import uniffi.mail_uniffi.MobileAction
import uniffi.mail_uniffi.MobileSettings
import uniffi.mail_uniffi.MobileSignature
import uniffi.mail_uniffi.MobileSignatureStatus
import uniffi.mail_uniffi.NonDefaultWeekStart
import uniffi.mail_uniffi.PartialDate
import uniffi.mail_uniffi.PgpScheme
import uniffi.mail_uniffi.PmSignature
import uniffi.mail_uniffi.PrivacyLock
import uniffi.mail_uniffi.PrivacyLockColor
import uniffi.mail_uniffi.PrivacyLockIcon
import uniffi.mail_uniffi.PrivacyLockTooltip
import uniffi.mail_uniffi.RemoteId
import uniffi.mail_uniffi.RsvpAnswer
import uniffi.mail_uniffi.RsvpAttendance
import uniffi.mail_uniffi.RsvpAttendee
import uniffi.mail_uniffi.RsvpAttendeeStatus
import uniffi.mail_uniffi.RsvpCalendar
import uniffi.mail_uniffi.RsvpEvent
import uniffi.mail_uniffi.RsvpOccurrence
import uniffi.mail_uniffi.RsvpOrganizer
import uniffi.mail_uniffi.RsvpProgress
import uniffi.mail_uniffi.RsvpState
import uniffi.mail_uniffi.RsvpUnanswerableReason
import uniffi.mail_uniffi.ShowImages
import uniffi.mail_uniffi.ShowMoved
import uniffi.mail_uniffi.SpamOrTrash
import uniffi.mail_uniffi.SwipeAction
import uniffi.mail_uniffi.SystemLabel
import uniffi.mail_uniffi.User
import uniffi.mail_uniffi.UserSettings
import uniffi.mail_uniffi.VCardUrl
import uniffi.mail_uniffi.VCardUrlValue
import uniffi.mail_uniffi.VcardPropType
import uniffi.mail_uniffi.ViewLayout
import uniffi.mail_uniffi.ViewMode

typealias LocalUser = User
typealias LocalUserId = String
typealias LocalConversation = Conversation
typealias LocalConversationId = Id
typealias LocalLabelId = Id
typealias LocalViewMode = ViewMode
typealias LocalMessageId = Id
typealias LocalMessageMetadata = Message
typealias LocalMessageBanner = MessageBanner
typealias LocalMessageBannerAutoDelete = MessageBanner.AutoDelete
typealias LocalMessageBannerBlockedSender = MessageBanner.BlockedSender
typealias LocalMessageBannerEmbeddedImages = MessageBanner.EmbeddedImages
typealias LocalMessageBannerExpiry = MessageBanner.Expiry
typealias LocalMessageBannerPhishingAttempt = MessageBanner.PhishingAttempt
typealias LocalMessageBannerRemoteContent = MessageBanner.RemoteContent
typealias LocalMessageBannerScheduledSend = MessageBanner.ScheduledSend
typealias LocalMessageBannerSnoozed = MessageBanner.Snoozed
typealias LocalMessageBannerSpam = MessageBanner.Spam
typealias LocalMessageBannerUnsubscribeNewsletter = MessageBanner.UnsubscribeNewsletter
typealias LocalMessageBannerUnableToDecrypt = MessageBanner.UnableToDecrypt
typealias LocalMessageBannerDomainAuthFail = MessageBanner.DomainAuthFail
typealias LocalAlmostAllMail = AlmostAllMail
typealias LocalAttachmentMetadata = AttachmentMetadata
typealias LocalAttachmentId = Id
typealias LocalAttachmentDisposition = Disposition
typealias LocalAttachmentMimeType = AttachmentMimeType
typealias LocalDecryptedAttachment = DecryptedAttachment
typealias LocalMimeTypeCategory = MimeTypeCategory
typealias LocalMimeType = MimeType
typealias LocalMailSettings = MailSettings
typealias LocalMobileSettings = MobileSettings
typealias LocalPmSignature = PmSignature
typealias LocalComposerMode = ComposerMode
typealias LocalMessageButtons = MessageButtons
typealias LocalShowImages = ShowImages
typealias LocalShowMoved = ShowMoved
typealias LocalViewLayout = ViewLayout
typealias LocalSwipeAction = SwipeAction
typealias LocalPgpScheme = PgpScheme
typealias LocalComposerDirection = ComposerDirection
typealias LocalSystemLabel = SystemLabel
typealias LocalAddressId = Id
typealias LocalAvatarInformation = AvatarInformation
typealias LocalExclusiveLocation = ExclusiveLocation
typealias LocalExclusiveLocationSystem = ExclusiveLocation.System
typealias LocalExclusiveLocationCustom = ExclusiveLocation.Custom
typealias LocalLabelAsAction = LabelAsAction
typealias LocalGroupedContacts = GroupedContacts
typealias LocalContactId = Id
typealias LocalContactGroupId = Id
typealias LocalContactEmail = ContactEmailItem
typealias LocalContactItemType = ContactItemType
typealias LocalContactItemTypeContact = ContactItemType.Contact
typealias LocalContactItemTypeGroup = ContactItemType.Group
typealias LocalComposerRecipient = ComposerRecipient
typealias LocalDraftSendResult = DraftSendResult
typealias LocalDeviceContact = DeviceContact
typealias LocalContactSuggestion = ContactSuggestion
typealias LocalContactItem = ContactItem
typealias LocalIssueReport = IssueReport
typealias RemoteMessageId = RemoteId
typealias LocalAttachmentData = AttachmentData
typealias LocalAppSettings = AppSettings
typealias LocalAppSettingsDiff = AppSettingsDiff
typealias LocalMobileAction = MobileAction
typealias LocalAutoLock = AutoLock
typealias LocalProtection = AppProtection
typealias LocalAutoLockPin = List<UInt>
typealias LocalAutoDeleteBanner = AutoDeleteBanner
typealias LocalAutoDeleteState = AutoDeleteState
typealias LocalSpamOrTrash = SpamOrTrash
typealias LocalContactDetailCard = ContactDetailCard
typealias LocalExtendedName = ExtendedName
typealias LocalContactField = ContactField
typealias LocalContactFieldAnniversary = ContactField.Anniversary
typealias LocalContactFieldBirthday = ContactField.Birthday
typealias LocalContactDate = ContactDate
typealias LocalContactDateDate = ContactDate.Date
typealias LocalContactDateString = ContactDate.String
typealias LocalPartialDate = PartialDate
typealias LocalContactFieldGender = ContactField.Gender
typealias LocalGenderKind = GenderKind
typealias LocalGenderKindMale = GenderKind.Male
typealias LocalGenderKindFemale = GenderKind.Female
typealias LocalGenderKindOther = GenderKind.Other
typealias LocalGenderKindNotApplicable = GenderKind.NotApplicable
typealias LocalGenderKindUnknown = GenderKind.Unknown
typealias LocalGenderKindNone = GenderKind.None
typealias LocalGenderKindString = GenderKind.String
typealias LocalContactFieldAddresses = ContactField.Addresses
typealias LocalContactDetailAddress = ContactDetailAddress
typealias LocalContactFieldEmails = ContactField.Emails
typealias LocalContactDetailsEmail = ContactDetailsEmail
typealias LocalContactGroup = ContactGroup
typealias LocalContactFieldLanguages = ContactField.Languages
typealias LocalContactFieldLogos = ContactField.Logos
typealias LocalContactFieldMembers = ContactField.Members
typealias LocalContactFieldNotes = ContactField.Notes
typealias LocalContactFieldOrganizations = ContactField.Organizations
typealias LocalContactFieldTelephones = ContactField.Telephones
typealias LocalContactDetailsTelephones = ContactDetailsTelephones
typealias LocalContactFieldPhotos = ContactField.Photos
typealias LocalContactFieldRoles = ContactField.Roles
typealias LocalContactFieldTimeZones = ContactField.TimeZones
typealias LocalContactFieldTitles = ContactField.Titles
typealias LocalContactFieldUrls = ContactField.Urls
typealias LocalVCardUrl = VCardUrl
typealias LocalVCardUrlValue = VCardUrlValue
typealias LocalVCardUrlValueHttp = VCardUrlValue.Http
typealias LocalVCardUrlValueNotHttp = VCardUrlValue.NotHttp
typealias LocalVCardUrlValueText = VCardUrlValue.Text
typealias LocalVCardPropType = VcardPropType
typealias LocalVCardPropTypeHome = VcardPropType.Home
typealias LocalVCardPropTypeWork = VcardPropType.Work
typealias LocalVCardPropTypeText = VcardPropType.Text
typealias LocalVCardPropTypeVoice = VcardPropType.Voice
typealias LocalVCardPropTypeFax = VcardPropType.Fax
typealias LocalVCardPropTypeCell = VcardPropType.Cell
typealias LocalVCardPropTypeVideo = VcardPropType.Video
typealias LocalVCardPropTypePager = VcardPropType.Pager
typealias LocalVCardPropTypeTextPhone = VcardPropType.TextPhone
typealias LocalVCardPropTypeString = VcardPropType.String
typealias LocalMailScrollerError = MailScrollerError
typealias LocalUserSettings = UserSettings
typealias LocalNonDefaultWeekStart = NonDefaultWeekStart
typealias LocalRsvpEvent = RsvpEvent
typealias LocalRsvpOccurrence = RsvpOccurrence
typealias LocalRsvpOrganizer = RsvpOrganizer
typealias LocalRsvpAttendee = RsvpAttendee
typealias LocalRsvpAttendeeStatus = RsvpAttendeeStatus
typealias LocalRsvpCalendar = RsvpCalendar
typealias LocalRsvpState = RsvpState
typealias LocalRsvpStateAnswerableInvite = RsvpState.AnswerableInvite
typealias LocalRsvpStateCancelledInvite = RsvpState.CancelledInvite
typealias LocalRsvpStateCancelledReminder = RsvpState.CancelledReminder
typealias LocalRsvpStateReminder = RsvpState.Reminder
typealias LocalRsvpStateUnanswerableInvite = RsvpState.UnanswerableInvite
typealias LocalRsvpProgress = RsvpProgress
typealias LocalRsvpAttendance = RsvpAttendance
typealias LocalRsvpUnanswerableReason = RsvpUnanswerableReason
typealias LocalRsvpAnswer = RsvpAnswer
typealias LocalMobileSignatureStatus = MobileSignatureStatus
typealias LocalMobileSignature = MobileSignature
typealias LocalHiddenMessagesBanner = HiddenMessagesBanner
typealias LocalAttachmentDataError = AttachmentDataError
typealias LocalAttachmentDataErrorProxyFailed = AttachmentDataError.ProxyFailed
typealias LocalAttachmentDataErrorOther = AttachmentDataError.Other
typealias LocalPrivacyLock = PrivacyLock
typealias LocalPrivacyLockColor = PrivacyLockColor
typealias LocalPrivacyLockIcon = PrivacyLockIcon
typealias LocalPrivacyLockTooltip = PrivacyLockTooltip
