package br.com.usedrip.techcase.george.transferagent.unittests

import br.com.usedrip.techcase.george.transferagent.ServletInitializer
import br.com.usedrip.techcase.george.transferagent.TransferAgentApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.builder.SpringApplicationBuilder

class ServletInitializerTest {
    @Test
    fun `Transfer agent application is added to the application builder`() {
        val servletInitializer = ServletInitializer()
        val springApplicationBuilder = mock<SpringApplicationBuilder> {
            on { sources(TransferAgentApplication::class.java) } doReturn it
        }

        val result = servletInitializer.configure(springApplicationBuilder)

        assertEquals(springApplicationBuilder, result)
        verify(springApplicationBuilder).sources(TransferAgentApplication::class.java)
    }
}
