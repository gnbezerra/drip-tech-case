package br.com.usedrip.techcase.george.transferagent.unittests

import br.com.usedrip.techcase.george.transferagent.RestExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class RestExceptionHandlerTests {
    @Mock
    private val clock: Clock? = null

    @InjectMocks
    private val handler: RestExceptionHandler? = null

    private val exception = Exception("Error explanation here")
    private val request = MockHttpServletRequest()
    private val fixedDate = LocalDate.of(1985, 7, 19)
    private val zoneId = ZoneId.of("America/Sao_Paulo")
    private val fixedClock = Clock.fixed(fixedDate.atStartOfDay(zoneId).toInstant(), zoneId)

    @BeforeEach
    fun setUp() {
        whenever(clock?.instant()).thenReturn(fixedClock.instant())
        whenever(clock?.zone).thenReturn(fixedClock.zone)
        request.pathInfo = "/foo"
    }

    @Test
    fun `method handleValidationExceptions() returns wrapped response with list of errors per field`() {
        val errorList: List<ObjectError> = listOf(
            FieldError("fooDTO", "foo", "foo cannot be empty"),
            FieldError("fooDTO", "bar", "bar must have 2 characters")
        )
        val exception = mock<MethodArgumentNotValidException>(defaultAnswer = RETURNS_DEEP_STUBS) {
            on { bindingResult.allErrors } doReturn (errorList)
        }

        val response = handler!!.handleValidationExceptions(exception, request)

        assertEquals("1985-07-19T00:00-03:00", response["timestamp"])
        assertEquals(400, response["status"])
        assertEquals("/foo", response["path"])
        assertInstanceOf(Map::class.java, response["error"])
        assertEquals(2, (response["error"] as Map<*, *>).size)
        assertEquals("foo cannot be empty", (response["error"] as Map<*, *>)["foo"])
        assertEquals("bar must have 2 characters", (response["error"] as Map<*, *>)["bar"])
    }

    @Test
    fun `method handleDatabaseConstraintViolationExceptions() returns wrapped response`() {
        val response = handler!!.handleDatabaseConstraintViolationExceptions(exception, request)

        assertEquals("1985-07-19T00:00-03:00", response["timestamp"])
        assertEquals(409, response["status"])
        assertEquals("Error explanation here", response["error"])
        assertEquals("/foo", response["path"])
    }

    @Test
    fun `method handleInexistentRelationExceptions() returns wrapped response`() {
        val response = handler!!.handleInexistentRelationExceptions(exception, request)

        assertEquals("1985-07-19T00:00-03:00", response["timestamp"])
        assertEquals(422, response["status"])
        assertEquals("Error explanation here", response["error"])
        assertEquals("/foo", response["path"])
    }

    @Test
    fun `method handleBadRequestsThrownByOurselves() returns wrapped response`() {
        val response = handler!!.handleBadRequestsThrownByOurselves(exception, request)

        assertEquals("1985-07-19T00:00-03:00", response["timestamp"])
        assertEquals(400, response["status"])
        assertEquals("Error explanation here", response["error"])
        assertEquals("/foo", response["path"])
    }

    @Test
    fun `method handleInvalidPostBody() returns wrapped response`() {
        val response = handler!!.handleInvalidPostBody(exception, request)

        assertEquals("1985-07-19T00:00-03:00", response["timestamp"])
        assertEquals(400, response["status"])
        assertEquals("Bad Request", response["error"])
        assertEquals("/foo", response["path"])
    }

    @Test
    fun `method handleUnsupportedMediaType() returns wrapped response`() {
        val response = handler!!.handleUnsupportedMediaType(exception, request)

        assertEquals("1985-07-19T00:00-03:00", response["timestamp"])
        assertEquals(415, response["status"])
        assertEquals("Unsupported Media Type", response["error"])
        assertEquals("/foo", response["path"])
    }
}
