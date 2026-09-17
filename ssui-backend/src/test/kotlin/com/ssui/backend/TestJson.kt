package com.ssui.backend

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/** ObjectMapper configured like the production one (Kotlin module, NON_NULL, lenient unknown properties). */
object TestJson {
    val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json()
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build()
}
