package br.com.usedrip.techcase.george.transferagent.integration

import br.com.usedrip.techcase.george.transferagent.BankDTO
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
class BankRestAPITests @Autowired constructor(val mockMvc: MockMvc) {
    private val jsonMapper = JsonMapper.builder().addModule(JavaTimeModule()).build()

    @Test
    fun `POST on bank endpoint creates new bank when body data is correct`() {
        val bankDTO = BankDTO("Foobank", "456")

        val mvcResult = mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isCreated() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"name": "Foobank", "code": "456"}""") }
        }.andReturn()

        val response = mvcResult.response.contentAsString
        val bankDTOResponse = jsonMapper.readValue(response, BankDTO::class.java)

        assertNotNull(bankDTOResponse)
        assertNotNull(bankDTOResponse.id)
    }

    @Test
    fun `GET on bank endpoint returns empty list when there are no banks`() {
        mockMvc.get("/bank") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("[]") }
        }
    }

    @Test
    fun `2 POSTs followed by a GET should list both banks`() {
        val bankDTO1 = BankDTO("FooBank", "001")
        val bankDTO2 = BankDTO("BarDesco", "987")

        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO1)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }
        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO2)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }

        val mvcResult = mockMvc.get("/bank") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""[{"name": "FooBank", "code": "001"}, {"name": "BarDesco", "code": "987"}]""") }
        }.andReturn()

        val response = mvcResult.response.contentAsString
        val bankDTOs = jsonMapper.readValue(response, Array<BankDTO>::class.java)

        assertNotNull(bankDTOs)
        assertEquals(2, bankDTOs.size)
        assertNotNull(bankDTOs[0].id)
        assertNotNull(bankDTOs[1].id)
    }

    @Test
    fun `invalid bank data results in status 400 response`() {
        val bankDTO = BankDTO(" ", "1")

        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 400,
                        |"path": "/bank",
                        |"error": {
                            |"name": "Name is required",
                            |"code": "Bank code must be a number with 3 digits"
                        |}
                    |}""".trimMargin()
                )
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "{", "{}"])
    fun `invalid json results in status 400 response`(postBody: String) {
        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = postBody
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 400, "path": "/bank", "error": "Bad Request"}""") }
        }
    }

    @Test
    fun `invalid content type results in status 415 response`() {
        val bankDTO = BankDTO("Foobank", "456")

        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_XML
            content = jsonMapper.writeValueAsString(bankDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnsupportedMediaType() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 415, "path": "/bank", "error": "Unsupported Media Type"}""") }
        }
    }

    @Test
    fun `invalid accept type results in status 406 response`() {
        val bankDTO = BankDTO("Foobank", "456")

        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO)
            accept = MediaType.APPLICATION_XML
        }.andExpect { status { isNotAcceptable() } }
    }

    @Test
    fun `cannot save 2 banks with the same bank code`() {
        val bankDTO1 = BankDTO("My Bank", "001")
        val bankDTO2 = BankDTO("Another Bank", "001")

        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO1)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isCreated() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"name": "My Bank", "code": "001"}""") }
        }

        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO2)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isConflict() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 409,
                        |"path": "/bank",
                        |"error": "Bank with code 001 already exists. Bank codes are unique."
                    |}""".trimMargin()
                )
            }
        }
    }
}
