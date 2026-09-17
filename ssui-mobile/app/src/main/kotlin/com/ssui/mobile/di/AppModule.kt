package com.ssui.mobile.di

import com.ssui.mobile.BuildConfig
import com.ssui.mobile.data.ScreenRepositoryImpl
import com.ssui.mobile.data.mapper.ScreenMapper
import com.ssui.mobile.data.remote.ScreenApi
import com.ssui.mobile.data.remote.createHttpClient
import com.ssui.mobile.domain.Logger
import com.ssui.mobile.domain.ScreenRepository
import com.ssui.mobile.ui.screen.ScreenViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<Logger> { AndroidLogger() }
    single<HttpClient> { createHttpClient() }
    single { ScreenApi(client = get(), baseUrl = BuildConfig.BASE_URL) }
    single { ScreenMapper(logger = get()) }
    single<ScreenRepository> { ScreenRepositoryImpl(api = get(), mapper = get()) }
    viewModel { ScreenViewModel(repository = get(), logger = get()) }
}
