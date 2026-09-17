package com.ssui.backend.api

import com.ssui.backend.domain.ScreenService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/screens", produces = [MediaType.APPLICATION_JSON_VALUE])
class ScreenController(
    private val screenService: ScreenService,
    private val screenValidator: ScreenValidator,
) {

    /** Debug helper: `{ id, name }` of every stored screen. */
    @GetMapping
    fun listScreens(): List<ScreenSummaryDto> = screenService.listAll()

    /** Returns the full element tree of the screen; 404 if it does not exist. */
    @GetMapping("/{name}")
    fun getScreen(@PathVariable name: String): ScreenDto = screenService.getByName(name)

    /**
     * Creates or fully replaces the screen. The `name` in the path wins over the body.
     * Returns 400 when the body is malformed or fails [ScreenValidator].
     */
    @PutMapping("/{name}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun putScreen(@PathVariable name: String, @RequestBody body: ScreenDto): ScreenDto {
        screenValidator.validate(body)
        return screenService.upsert(name, body)
    }
}
