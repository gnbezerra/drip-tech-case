package br.com.usedrip.techcase.george.transferagent

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.OffsetDateTime

@ControllerAdvice
@RestController
class RestExceptionHandler @Autowired constructor(val clock: Clock) {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: HttpServletRequest): Map<String, Any> {
        val errors: MutableMap<String, String?> = LinkedHashMap()
        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage
            errors[fieldName] = errorMessage
        }
        return responseWrapper(errors, HttpStatus.BAD_REQUEST.value(), request.pathInfo)
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(
        DataIntegrityViolationException::class,
        AccountAlreadyExistsException::class,
        BankCodeAlreadyExistsException::class,
        CPFAlreadyExistsException::class
    )
    fun handleDatabaseConstraintViolationExceptions(ex: Exception, request: HttpServletRequest): Map<String, Any> {
        return responseWrapper(ex.message.toString(), HttpStatus.CONFLICT.value(), request.pathInfo)
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(AccountNotFoundException::class, BankNotFoundException::class, CustomerNotFoundException::class)
    fun handleInexistentRelationExceptions(ex: Exception, request: HttpServletRequest): Map<String, Any> {
        return responseWrapper(ex.message.toString(), HttpStatus.UNPROCESSABLE_ENTITY.value(), request.pathInfo)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InsufficientFundsException::class, OperationNotAllowedException::class)
    fun handleBadRequestsThrownByOurselves(ex: Exception, request: HttpServletRequest): Map<String, Any> {
        return responseWrapper(ex.message.toString(), HttpStatus.BAD_REQUEST.value(), request.pathInfo)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidPostBody(ex: Exception, request: HttpServletRequest): Map<String, Any> {
        return responseWrapper("Bad Request", HttpStatus.BAD_REQUEST.value(), request.pathInfo)
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(ex: Exception, request: HttpServletRequest): Map<String, Any> {
        return responseWrapper("Unsupported Media Type", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), request.pathInfo)
    }

    private fun responseWrapper(error: Any, status: Int, path: String): Map<String, Any> {
        val response: MutableMap<String, Any> = LinkedHashMap()
        response["timestamp"] = OffsetDateTime.now(clock).toString()
        response["status"] = status
        response["error"] = error
        response["path"] = path
        return response
    }
}
