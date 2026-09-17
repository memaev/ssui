package com.ssui.backend.seed

import com.ssui.backend.api.ScreenValidator
import com.ssui.backend.mapping.ScreenMapper
import com.ssui.backend.persistence.ScreenRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Inserts the `home` screen from `seed/home.json` on startup, only if no screen with that name exists.
 * Existing data is never overwritten, so edits made directly in Mongo survive restarts.
 */
@Component
class DataSeeder(
    private val screenRepository: ScreenRepository,
    private val screenMapper: ScreenMapper,
    private val screenValidator: ScreenValidator,
    private val seedScreenReader: SeedScreenReader,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(vararg args: String) {
        if (screenRepository.existsByName(HOME_SCREEN_NAME)) {
            log.info("Screen '{}' already exists, skipping seed", HOME_SCREEN_NAME)
            return
        }

        val seed = seedScreenReader.read().copy(name = HOME_SCREEN_NAME)
        requireNotNull(seed.id) { "Seed ${SeedScreenReader.HOME_SEED_PATH} must contain an _id" }
        screenValidator.validate(seed)

        val saved = screenRepository.insert(screenMapper.toEntity(seed))
        log.info("Seeded screen '{}' with id {}", saved.name, saved.id)
    }

    companion object {
        const val HOME_SCREEN_NAME = "home"
    }
}
