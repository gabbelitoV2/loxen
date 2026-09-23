package com.moblin.android.view.controlbar.quickbutton.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatDate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.ChatPost
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getKickChatterInfo
import com.moblin.android.view.CloseButtonTopRightView
import com.moblin.android.view.utils.HCenter
import java.net.URI
import com.moblin.android.platform.Bundle
import com.moblin.android.streamingplatforms.Platform

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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = formPalette().gray,
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
                val imageName = post.platform?.imageName()
                if (imageName != null) {
                    val bitmap = remember(imageName) {
                        Bundle.image(imageName)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(16.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                post.userBadges.forEach { url ->
                    key(url) {
                        CacheAsyncImage(
                            url = URI(url),
                            content = { image: ImageBitmap ->
                                Image(
                                    bitmap = image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.height(18.dp),
                                )
                            },
                            placeholder = {},
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
                color = formPalette().gray,
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
        when (post.platform) {
            Platform.kick -> {
                model.getKickChatterInfo(user) { info ->
                    if (info != null) {
                        chatterInfo = info
                    } else {
                        setErrorMessage()
                    }
                    loading = false
                }
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Spacer(Modifier.height(1.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box {
                    val info = chatterInfo
                    val message = errorMessage
                    when {
                        loading -> {
                            HCenter {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                        message != null -> {
                            HCenter {
                                Text(
                                    text = message,
                                    color = formPalette().gray,
                                )
                            }
                        }
                        info != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
