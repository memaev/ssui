package com.ssui.mobile

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.ssui.mobile.data.remote.dto.DtoParseIssueReporter
import com.ssui.mobile.di.appModule
import com.ssui.mobile.domain.Logger
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SsuiApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SsuiApp)
            modules(appModule)
        }
        val logger: Logger = get()
        DtoParseIssueReporter.sink = { message -> logger.w("SsuiDto", message) }
    }

    /** Coil 3 image loader with the OkHttp network fetcher (spec 6.1). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
}
