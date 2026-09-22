package com.moblin.android.various

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.moblin.android.various.network.httpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Request

interface FaxReceiverDelegate {
    fun faxReceiverPrint(image: Bitmap)
}

class FaxReceiver {
    var delegate: FaxReceiverDelegate? = null

    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun add(url: String) {
        httpRequest(
            Request.Builder().url(url).build(),
        ) { data, response, _ ->
            if (data == null || response?.http?.isSuccessful != true) {
                return@httpRequest
            }
            val image = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return@httpRequest
            mainScope.launch {
                delegate?.faxReceiverPrint(image)
            }
        }
    }
}
