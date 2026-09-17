package com.ssui.backend.persistence

import org.springframework.data.mongodb.repository.MongoRepository

interface ScreenRepository : MongoRepository<ScreenEntity, String> {
    fun findByName(name: String): ScreenEntity?
    fun existsByName(name: String): Boolean
}
