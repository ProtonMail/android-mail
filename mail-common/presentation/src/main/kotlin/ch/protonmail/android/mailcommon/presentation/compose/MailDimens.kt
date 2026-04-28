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

package ch.protonmail.android.mailcommon.presentation.compose

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MailDimens {

    val ThinBorder = 0.5.dp
    val DefaultBorder = 1.dp
    val MediumBorder = 2.dp
    val AvatarBorderLine = 1.5.dp
    val OnboardingUpsellBestValueBorder = 2.dp

    val SeparatorHeight = 1.dp

    val DefaultTouchTargetSize = 32.dp

    val AvatarMinSize = 28.dp
    val AvatarCheckmarkSize = 20.dp
    val AvatarIconPadding = 6.dp
    val AvatarIconSize = 20.dp
    val AvatarSize = 40.dp

    val BottomBarHeight = 80.dp

    val ProgressDefaultSize = 24.dp
    val ProgressStrokeWidth = 2.dp

    val TinyIcon = 12.dp
    val AutolockIconWidth = 180.dp
    val AutolockIconHeight = 144.dp

    val ExtraLargeSpacing = 48.dp

    val ErrorIconBoxSize = 80.dp

    val ConversationMessageCollapseBarHeight = 20.dp
    val ConversationCollapseHeaderOverlapHeight = 32.dp
    val ConversationItemBottomPadding = 22.dp
    val ConversationCollapseHeaderElevation = 16.dp

    val IconWeakRoundBackgroundRadius = 28.dp

    val ListItemCircleFilledSize = 16.dp
    val ListItemCircleFilledPadding = 20.dp

    val MailboxItemLabelHeight = 20.dp

    val TextFieldSingleLineSize = 80.dp
    val TextFieldMultiLineSize = 128.dp

    val ExtraSmallNegativeOffset = (-4).dp

    object ColorPicker {

        val CircleBoxSize = 56.dp
        val CircleSize = 20.dp
        val SelectedCircleSize = 40.dp
        val SelectedCircleBorderSize = 2.dp
        val SelectedCircleInternalMargin = 10.dp
    }

    val ScrollableFormBottomButtonSpacing = 60.dp

    const val ActionButtonShapeRadius = 100

    val SingleLineTopAppBarHeight = 56.dp
    val SubjectHeaderMinHeight = 56.dp
    val MinOffsetForSubjectAlphaChange = 48.dp

    val pagerDotsCircleSize = 8.dp
    val onboardingBottomButtonHeight = 48.dp
    val OnboardingCloseButtonToolbarHeight = 48.dp
    const val OnboardingIllustrationWeight = 0.4f

    object AutoLockPinScreen {

        val SpacerSize = 48.dp
        val PinDotsGridHeight = 64.dp
        val KeyboardButtonBoxSize = 84.dp
        val DigitTextSize = 20.sp
        val BottomButtonSize = 24.dp
    }

    object Contacts {
        val SearchTopBarHeight = 72.dp
        val AvatarSize = 40.dp
    }

    val ContactAvatarSize = 100.dp
    val SmallContactAvatarSize = 40.dp
    val ContactAvatarIconSize = 40.dp

    val NotificationDotSize = 8.dp

    val ProtonCalendarIconSize = 40.dp

    object ColoredRadioButton {
        val CircleSize = 15.dp
        val SelectedCircleSize = 24.dp
        val SelectedCircleBorderSize = 2.dp
        val SelectedCircleInternalMargin = 4.dp
    }

    val DialogCardRadius = 16.dp

    val NarrowScreenWidth = 360.dp

    val PlanSwitcherHeight = 68.dp
    val OnboardingUpsellButtonHeight = 48.dp

    val MailboxFabRadius = 32.dp
    val SnackbarFabOffset = 56.dp

    val MessageBannerIconSize = 20.dp

    object MessageDetailsHeader {

        val ButtonSize = 36.dp
        val ButtonIconSize = 20.dp
        val CollapseExpandButtonSize = 16.dp
        val DetailsTitleWidth = 44.dp
    }

    val DetailsMoreQuickReplyButtonSize = 84.dp

    val LabelAsDoneButtonHeight = 52.dp

    object Composer {

        val FormFieldsRowHeight = 56.dp
    }

    object Attachment {
        val ItemRowHeight = 48.dp
        val UploadingSpinnerSize = 28.dp
    }

    val UnreadFilterChipHeight = 36.dp

    val MailboxSkeletonRowHeight = 40.dp

    val RsvpButtonHeight = 56.dp
    val RsvpCalendarLogoSize = 58.dp

    object CategoryView {
        val ItemHeight = 40.dp
        val IconSize = 20.dp
    }
}
