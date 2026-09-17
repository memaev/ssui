package com.ssui.backend.persistence

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter

/**
 * Stops Spring Data from writing a `_class` discriminator into every (sub)document, so the stored
 * screen looks exactly like the spec example and stays pleasant to edit in Compass / mongosh.
 * Safe here because the element tree is not polymorphic.
 */
@Configuration
class MongoConfig(private val mappingMongoConverter: MappingMongoConverter) {

    @PostConstruct
    fun disableTypeDiscriminator() {
        mappingMongoConverter.setTypeMapper(DefaultMongoTypeMapper(null))
    }
}
