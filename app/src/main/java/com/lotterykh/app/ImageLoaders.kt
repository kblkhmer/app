package com.lotterykh.app

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * Shared Coil [ImageLoader] for ad/result images loaded from the network.
 *
 * Coil takes care of downsampling, memory/disk caching and off-main-thread
 * decoding for us, replacing the manual BitmapFactory/ImageDecoder
 * download+decode code that used to run on ad-hoc threads in MainActivity,
 * LotteryFragment and AdPopupDialog (flagged by Play Console as excessive
 * memory usage / crash risk).
 */
object ImageLoaders {

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}
