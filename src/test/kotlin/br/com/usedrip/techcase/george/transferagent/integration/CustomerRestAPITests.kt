package br.com.usedrip.techcase.george.transferagent.integration

import br.com.usedrip.techcase.george.transferagent.CustomerDTO
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerRestAPITests @Autowired constructor(val mockMvc: MockMvc) {
    private val jsonMapper = JsonMapper.builder().addModule(JavaTimeModule()).build()

    @Test
    fun `POST on customer endpoint creates new customer when body data is correct`() {
        val customerDTO = CustomerDTO("Foo Bar", "32165498701")

        val mvcResult = mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isCreated() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"fullName": "Foo Bar", "cpf": "32165498701"}""") }
        }.andReturn()

        val response = mvcResult.response.contentAsString
        val customerDTOResponse = jsonMapper.readValue(response, CustomerDTO::class.java)

        assertNotNull(customerDTOResponse)
        assertNotNull(customerDTOResponse.id)
    }

    @Test
    fun `GET on customer endpoint returns empty list when there are no customers`() {
        mockMvc.get("/customer") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("[]") }
        }
    }

    @Test
    fun `2 POSTs followed by a GET should list both customers`() {
        val customerDTO1 = CustomerDTO("Foo Bar", "32165498701")
        val customerDTO2 = CustomerDTO("Baz Qux", "12345678987")

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO1)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }
        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO2)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }

        val mvcResult = mockMvc.get("/customer") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """[{"fullName": "Foo Bar", "cpf": "32165498701"}, {"fullName": "Baz Qux", "cpf": "12345678987"}]"""
                )
            }
        }.andReturn()

        val response = mvcResult.response.contentAsString
        val customerDTOs = jsonMapper.readValue(response, Array<CustomerDTO>::class.java)

        assertNotNull(customerDTOs)
        assertEquals(2, customerDTOs.size)
        assertNotNull(customerDTOs[0].id)
        assertNotNull(customerDTOs[1].id)
    }

    @Test
    fun `invalid customer data results in status 400 response`() {
        val customerDTO = CustomerDTO(" ", "123.456.789-01")

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 400,
                        |"path": "/customer",
                        |"error": {
                            |"fullName": "Full name is required",
                            |"cpf": "CPF must be a number with 11 digits, with no dashes or dots"
                        |}
                    |}""".trimMargin()
                )
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "{", "{}"])
    fun `invalid json results in status 400 response`(postBody: String) {
        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = postBody
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 400, "path": "/customer", "error": "Bad Request"}""") }
        }
    }

    @Test
    fun `invalid content type results in status 415 response`() {
        val customerDTO = CustomerDTO("Foo Bar", "32165498701")

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_XML
            content = jsonMapper.writeValueAsString(customerDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnsupportedMediaType() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 415, "path": "/customer", "error": "Unsupported Media Type"}""") }
        }
    }

    @Test
    fun `invalid accept type results in status 406 response`() {
        val customerDTO = CustomerDTO("Foo Bar", "32165498701")

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO)
            accept = MediaType.APPLICATION_XML
        }.andExpect { status { isNotAcceptable() } }
    }

    @Test
    fun `cannot save 2 customers with the same cpf`() {
        val customerDTO1 = CustomerDTO("My Customer", "00100200304")
        val customerDTO2 = CustomerDTO("Another Customer", "00100200304")

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO1)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isCreated() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"fullName": "My Customer", "cpf": "00100200304"}""") }
        }

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO2)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isConflict() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{"status": 409, "path": "/customer", "error": "Customer with CPF 00100200304 already exists"}"""
                )
            }
        }
    }
}
