package ch.protonmail.android.maildetail.presentation.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBannersUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallInverted

@Composable
fun MessageBanners(messageBannersUiModel: MessageBannersUiModel) {
    if (messageBannersUiModel.shouldShowPhishingBanner) {
        MessageBanner(
            icon = R.drawable.ic_proton_hook,
            iconTint = ProtonTheme.colors.iconInverted,
            text = R.string.message_phishing_banner_text,
            textStyle = ProtonTheme.typography.defaultSmallInverted,
            backgroundColor = ProtonTheme.colors.notificationError
        )
    }
}

@Composable
fun MessageBanner(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    iconTint: Color,
    @StringRes text: Int,
    textStyle: TextStyle,
    backgroundColor: Color,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .testTag(MessageBodyTestTags.MessageBodyBanner)
            .padding(ProtonDimens.DefaultSpacing)
            .background(color = backgroundColor, shape = ProtonTheme.shapes.medium)
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        Row {
            Icon(
                modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerIcon),
                painter = painterResource(id = icon),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
            Text(
                modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerText),
                text = stringResource(id = text),
                style = textStyle
            )
        }

        content()
    }
}
