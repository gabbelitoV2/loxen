package com.moblin.android.view.controlbar.quickbutton.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatDate
import com.moblin.android.localized
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.ChatPost
import com.moblin.android.various.model.Model
import com.moblin.android.view.CloseButtonTopRightView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.LocalModel

enum class ChatterRole {
    owner,
    staff,
    moderator,
    viewer;

    fun localized(): String = when (this) {
        owner -> localized("Owner")
        staff -> localized("Staff")
        moderator -> localized("Moderator")
        viewer -> localized("Viewer")
    }
}

data class ChatterInfo(
    var profilePicture: String? = null,
    var bio: String? = null,
    var accountCreated: String? = null,
    var role: ChatterRole,
    var followingSince: String? = null,
    var subscribedMonths: Int,
    var giftedSubs: Int? = null,
    var followers: Int? = null,
)

@Composable
private fun InfoRowView(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = Color.Gray,
        )
        Spacer(Modifier.weight(1f))
        Text(value)
    }
}

@Composable
private fun profileHeader(model: Model = LocalModel.current, post: ChatPost, info: ChatterInfo) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChannelImageView(image = info.profilePicture)
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = post.displayName(
                        nicknames = model.database.chat.nicknames,
                        displayStyle = model.database.chat.displayStyle,
                    ),
                    fontWeight = FontWeight.Bold,
                )
                val context = LocalContext.current
                val imageName = post.platform?.imageName()
                val resourceId = remember(imageName) {
                    if (imageName != null) {
                        context.resources.getIdentifier(imageName, "drawable", context.packageName)
                    } else {
                        0
                    }
                }
                if (resourceId != 0) {
                    Image(
                        painter = painterResource(id = resourceId),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(16.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                post.userBadges.forEach { url ->
                    CacheAsyncImage(url = url) { image ->
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(18.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun infoRows(info: ChatterInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val accountCreated = info.accountCreated
        if (accountCreated != null) {
            val formatted = formatDate(accountCreated)
            if (formatted != null) {
                InfoRowView(label = localized("Account created"), value = formatted)
            }
        }
        InfoRowView(label = localized("Role"), value = info.role.localized())
        val followed = info.followingSince?.let { formatDate(it) }
        if (followed != null) {
            InfoRowView(label = localized("Followed"), value = followed)
        } else {
            InfoRowView(label = localized("Followed"), value = localized("No"))
        }
        if (info.subscribedMonths > 0) {
            InfoRowView(
                label = localized("Subscribed"),
                value = "${info.subscribedMonths} months",
            )
        } else {
            InfoRowView(label = localized("Subscribed"), value = localized("No"))
        }
        val giftedSubs = info.giftedSubs
        if (giftedSubs != null) {
            InfoRowView(label = localized("Gifted subs"), value = giftedSubs.toString())
        }
        val followers = info.followers
        if (followers != null) {
            InfoRowView(label = localized("Followers"), value = followers.toString())
        }
        val bio = info.bio
        if (bio != null && bio.isNotEmpty()) {
            Text(
                text = localized("About"),
                color = Color.Gray,
            )
            Text(
                text = bio,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun QuickButtonChatChatterInfoView(
    model: Model = LocalModel.current,
    post: ChatPost,
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
) {
    var chatterInfo by remember { mutableStateOf<ChatterInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun setErrorMessage() {
        errorMessage = localized("Failed to load chatter info")
    }

    suspend fun fetchInfo() {
        val user = post.user
        if (user == null) {
            setErrorMessage()
            loading = false
            return
        }
        when (post.platform?.name?.lowercase()) {
            "kick" -> {
                val info = model.getKickChatterInfo(user)
                if (info != null) {
                    chatterInfo = info
                } else {
                    setErrorMessage()
                }
                loading = false
            }
            else -> {
                setErrorMessage()
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchInfo()
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(
            modifier = Modifier.background(Color.Black),
        ) {
            Spacer(Modifier.height(1.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box {
                    val info = chatterInfo
                    val message = errorMessage
                    when {
                        loading -> {
                            HCenter {
                                CircularProgressIndicator()
                            }
                        }
                        message != null -> {
                            HCenter {
                                Text(
                                    text = message,
                                    color = Color.Gray,
                                )
                            }
                        }
                        info != null -> {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .padding(top = 8.dp),
                            ) {
                                profileHeader(model = model, post = post, info = info)
                                infoRows(info = info)
                            }
                        }
                    }
                    CloseButtonTopRightView {
                        onPresentingChange(false)
                    }
                }
            }
            Spacer(Modifier.height(1.dp))
        }
    }
}
