package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized

@Composable
fun IconAndTextView(image: String, text: String, longDivider: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (longDivider) {
            Spacer(Modifier.weight(1f))
        }
        Icon(
            imageVector = sfSymbolToImageVector(image),
            contentDescription = null,
            modifier = Modifier.width(iconWidth.dp),
        )
        Text(text)
    }
}

@Composable
fun IconAndTextLocalizedView(image: String, text: String, longDivider: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (longDivider) {
            Spacer(Modifier.weight(1f))
        }
        Icon(
            imageVector = sfSymbolToImageVector(image),
            contentDescription = null,
            modifier = Modifier.width(iconWidth.dp),
        )
        Text(localized(text))
    }
}

@Composable
fun IconAndTextSettingView(image: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.width(25.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Icon(imageVector = sfSymbolToImageVector(image), contentDescription = null)
            Spacer(Modifier.weight(1f))
        }
        Text(localized(text), modifier = Modifier.padding(start = 3.dp))
    }
}

private fun sfSymbolToImageVector(name: String): ImageVector {
    return when (name) {
        "gear", "gearshape", "gearshape.fill" -> Icons.Default.Settings
        "star", "star.fill" -> Icons.Default.Star
        "person", "person.fill" -> Icons.Default.Person
        "person.crop.circle" -> Icons.Default.AccountCircle
        "person.crop.circle.badge.plus" -> Icons.Default.AccountCircle
        "house", "house.fill" -> Icons.Default.Home
        "magnifyingglass" -> Icons.Default.Search
        "plus", "plus.circle", "plus.circle.fill" -> Icons.Default.Add
        "xmark", "xmark.circle", "xmark.circle.fill" -> Icons.Default.Close
        "checkmark", "checkmark.circle", "checkmark.circle.fill" -> Icons.Default.Check
        "info", "info.circle" -> Icons.Default.Info
        "bell", "bell.fill" -> Icons.Default.Notifications
        "trash", "trash.fill" -> Icons.Default.Delete
        "square.and.arrow.up" -> Icons.Default.Share
        "square.and.pencil" -> Icons.Default.Create
        "pencil" -> Icons.Default.Edit
        "arrow.clockwise" -> Icons.Default.Refresh
        "arrow.left" -> Icons.Default.ArrowBack
        "arrow.right" -> Icons.Default.ArrowForward
        "chevron.left" -> Icons.Default.KeyboardArrowLeft
        "chevron.right" -> Icons.Default.KeyboardArrowRight
        "chevron.up" -> Icons.Default.KeyboardArrowUp
        "chevron.down" -> Icons.Default.KeyboardArrowDown
        "play", "play.fill" -> Icons.Default.PlayArrow
        "location", "location.fill" -> Icons.Default.LocationOn
        "envelope", "envelope.fill" -> Icons.Default.Email
        "phone", "phone.fill" -> Icons.Default.Phone
        "lock", "lock.fill" -> Icons.Default.Lock
        "heart", "heart.fill" -> Icons.Default.Favorite
        "hand.thumbsup", "hand.thumbsup.fill" -> Icons.Default.ThumbUp
        "paperplane", "paperplane.fill" -> Icons.Default.Send
        "list.bullet" -> Icons.Default.List
        "ellipsis", "ellipsis.circle" -> Icons.Default.MoreVert
        "calendar" -> Icons.Default.DateRange
        "cart", "cart.fill" -> Icons.Default.ShoppingCart
        "warning", "exclamationmark.triangle" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
}
