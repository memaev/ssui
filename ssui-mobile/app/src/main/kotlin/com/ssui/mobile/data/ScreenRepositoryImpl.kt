package com.ssui.mobile.data

import com.ssui.mobile.data.mapper.ScreenMapper
import com.ssui.mobile.data.remote.ScreenApi
import com.ssui.mobile.domain.Screen
import com.ssui.mobile.domain.ScreenRepository

class ScreenRepositoryImpl(
    private val api: ScreenApi,
    private val mapper: ScreenMapper,
) : ScreenRepository {

    override suspend fun getScreen(name: String): Screen = mapper.toDomain(api.getScreen(name))
}
