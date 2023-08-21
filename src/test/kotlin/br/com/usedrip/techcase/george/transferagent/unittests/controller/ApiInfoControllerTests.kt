package br.com.usedrip.techcase.george.transferagent.unittests.controller

import br.com.usedrip.techcase.george.transferagent.ApiInfoController
import br.com.usedrip.techcase.george.transferagent.ApiInfoDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@ExtendWith(MockitoExtension::class)
class ApiInfoControllerTests {
    private val apiInfoController = ApiInfoController()

    @BeforeEach
    fun setUp() {
        val request = MockHttpServletRequest()
        request.scheme = "http"
        request.serverName = "foobar"
        request.serverPort = 42
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @Test
    fun `info() returns ApiInfoDTO with swagger-ui url`() {
        val info = apiInfoController.info()

        assertInstanceOf(ApiInfoDTO::class.java, info)
        assertEquals("http://foobar:42/swagger-ui.html", info.swaggerUrl)
    }
}
