package com.ssui.backend.api

import com.fasterxml.jackson.core.JsonProcessingException
import com.ssui.backend.domain.ScreenNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Error body for every non-2xx response: `{ "status": 404, "message": "Screen 'home' not found" }`. */
data class ApiError(
    val status: Int,
    val message: String,
)

@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ScreenNotFoundException::class)
    fun handleNotFound(ex: ScreenNotFoundException): ResponseEntity<ApiError> =
        respond(HttpStatus.NOT_FOUND, ex.message ?: "Not found")

    @ExceptionHandler(ScreenValidationException::class)
    fun handleValidation(ex: ScreenValidationException): ResponseEntity<ApiError> =
        respond(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid screen")

    /** Unparseable body, missing required fields, unknown enum values, wrong JSON types. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        val cause = ex.mostSpecificCause
        val detail = (cause as? JsonProcessingException)?.originalMessage ?: cause.message
        val message = if (detail.isNullOrBlank()) "Malformed JSON request body" else "Malformed JSON request body: $detail"
        return respond(HttpStatus.BAD_REQUEST, message)
    }

    /** Two screens can never share an `_id` or a `name` (unique index). */
    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKey(ex: DuplicateKeyException): ResponseEntity<ApiError> =
        respond(HttpStatus.CONFLICT, "A screen with the same id or name already exists")

    /** Built-in Spring MVC errors (unknown path, unsupported method / media type, ...) in the same body shape. */
    @ExceptionHandler(ErrorResponseException::class)
    fun handleSpringWebError(ex: ErrorResponseException): ResponseEntity<ApiError> {
        val status = ex.statusCode
        val message = ex.body.detail ?: HttpStatus.resolve(status.value())?.reasonPhrase ?: "Request failed"
        return respond(status, message)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiError> {
        log.error("Unhandled exception while processing request", ex)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error")
    }

    private fun respond(status: HttpStatusCode, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiError(status.value(), message))
}
