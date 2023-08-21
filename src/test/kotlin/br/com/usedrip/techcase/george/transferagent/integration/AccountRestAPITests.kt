package br.com.usedrip.techcase.george.transferagent.integration

import br.com.usedrip.techcase.george.transferagent.AccountDTO
import br.com.usedrip.techcase.george.transferagent.AccountRequestDTO
import br.com.usedrip.techcase.george.transferagent.BankDTO
import br.com.usedrip.techcase.george.transferagent.CustomerDTO
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import java.math.BigDecimal

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountRestAPITests @Autowired constructor(val mockMvc: MockMvc) {
    private val jsonMapper = JsonMapper.builder().addModule(JavaTimeModule()).build()

    @Test
    fun `GET on account endpoint returns empty list when there are no accounts`() {
        mockMvc.get("/account") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("[]") }
        }
    }

    @Test
    fun `need to create bank and customer before creating account`() {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", "0001", "12345-X", BigDecimal.TEN)
        val bankDTO = BankDTO("My Bank", "001")

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnprocessableEntity() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 422, "path": "/account", "error": "Bank with code 001 not found."}""") }
        }

        // Ok, let's create the bank
        mockMvc.post("/bank") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(bankDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }

        // We still don't have the customer in the database
        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnprocessableEntity() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json("""{"status": 422, "path": "/account", "error": "Customer with CPF 12345678901 not found."}""")
            }
        }
    }

    @Test
    fun `invalid account data results in status 400 response`() {
        val accountRequestDTO = AccountRequestDTO("1234", "5678", "", "", BigDecimal("12.345"))

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 400,
                        |"path": "/account",
                        |"error": {
                            |"bankCode": "Bank code must be a number with 3 digits",
                            |"customerCpf": "CPF must be a number with 11 digits, with no dashes or dots",
                            |"branch": "Branch is required",
                            |"accountNumber": "Account number is required",
                            |"money": "Money can only have up to 2 decimal places"
                        |}
                    |}""".trimMargin()
                )
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "{", "{}"])
    fun `invalid json results in status 400 response`(postBody: String) {
        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = postBody
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 400, "path": "/account", "error": "Bad Request"}""") }
        }
    }

    @Test
    fun `invalid content type results in status 415 response`() {
        val accountRequestDTO = AccountRequestDTO("456", "32165498701", "0002", "54321-0")

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_XML
            content = jsonMapper.writeValueAsString(accountRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnsupportedMediaType() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("""{"status": 415, "path": "/account", "error": "Unsupported Media Type"}""") }
        }
    }

    @Test
    fun `invalid accept type results in status 406 response`() {
        val accountRequestDTO = AccountRequestDTO("456", "32165498701", "0002", "54321-0")

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountRequestDTO)
            accept = MediaType.APPLICATION_XML
        }.andExpect { status { isNotAcceptable() } }
    }

    @Nested
    inner class BankAndCustomerAreAlreadyInTheDatabase {
        @BeforeEach
        fun setUp() {
            val bankDTO = BankDTO("Foobank", "456")
            val customerDTO = CustomerDTO("Foo Bar", "32165498701")

            mockMvc.post("/bank") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(bankDTO)
                accept = MediaType.APPLICATION_JSON
            }.andExpect { status { isCreated() } }

            mockMvc.post("/customer") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(customerDTO)
                accept = MediaType.APPLICATION_JSON
            }.andExpect { status { isCreated() } }
        }

        @Test
        fun `POST on account endpoint creates new account when body data is correct and both bank and customer exist in the database`() {
            val accountRequestDTO = AccountRequestDTO("456", "32165498701", "0001", "12345-X", BigDecimal.TEN)

            val mvcResult = mockMvc.post("/account") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(accountRequestDTO)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content {
                    json(
                        """{
                        |"bank": {"name": "Foobank", "code": "456"},
                        |"customer": {"fullName": "Foo Bar", "cpf": "32165498701"},
                        |"branch": "0001",
                        |"accountNumber": "12345-X",
                        |"money": 10.00
                    |}""".trimMargin()
                    )
                }
            }.andReturn()

            val response = mvcResult.response.contentAsString
            val accountDTOResponse = jsonMapper.readValue(response, AccountDTO::class.java)

            Assertions.assertNotNull(accountDTOResponse)
            Assertions.assertNotNull(accountDTOResponse.id)
        }

        @Test
        fun `not specifying money on account during POST makes money equals to zero`() {
            val accountRequestDTO = AccountRequestDTO("456", "32165498701", "0001", "12345-X")

            mockMvc.post("/account") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(accountRequestDTO)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content {
                    json(
                        """{
                        |"bank": {"name": "Foobank", "code": "456"},
                        |"customer": {"fullName": "Foo Bar", "cpf": "32165498701"},
                        |"branch": "0001",
                        |"accountNumber": "12345-X",
                        |"money": 0.00
                    |}""".trimMargin()
                    )
                }
            }
        }

        @Test
        fun `2 POSTs followed by a GET should list both accounts`() {
            val accountRequestDTO1 = AccountRequestDTO("456", "32165498701", "0001", "12345-X", BigDecimal.TEN)
            val accountRequestDTO2 = AccountRequestDTO("456", "32165498701", "0002", "54321-0")

            // POSTS
            mockMvc.post("/account") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(accountRequestDTO1)
                accept = MediaType.APPLICATION_JSON
            }.andExpect { status { isCreated() } }
            mockMvc.post("/account") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(accountRequestDTO2)
                accept = MediaType.APPLICATION_JSON
            }.andExpect { status { isCreated() } }

            // GET
            val mvcResult = mockMvc.get("/account") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content {
                    json(
                        """[
                        |{
                            |"bank": {"name": "Foobank", "code": "456"},
                            |"customer": {"fullName": "Foo Bar", "cpf": "32165498701"},
                            |"branch": "0001",
                            |"accountNumber": "12345-X",
                            |"money": 10.00
                        |},
                        |{
                            |"bank": {"name": "Foobank", "code": "456"},
                            |"customer": {"fullName": "Foo Bar", "cpf": "32165498701"},
                            |"branch": "0002",
                            |"accountNumber": "54321-0",
                            |"money": 0.00
                        |}
                    |]""".trimMargin()
                    )
                }
            }.andReturn()

            val response = mvcResult.response.contentAsString
            val accountDTOs = jsonMapper.readValue(response, Array<AccountDTO>::class.java)

            Assertions.assertNotNull(accountDTOs)
            Assertions.assertEquals(2, accountDTOs.size)
            Assertions.assertNotNull(accountDTOs[0].id)
            Assertions.assertNotNull(accountDTOs[1].id)
        }

        @Test
        fun `cannot save 2 accounts with the same bank code, branch and account number combination`() {
            val accountRequestDTO = AccountRequestDTO("456", "32165498701", "0001", "12345-X")

            mockMvc.post("/account") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(accountRequestDTO)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content {
                    json(
                        """{
                        |"bank": {"name": "Foobank", "code": "456"},
                        |"customer": {"fullName": "Foo Bar", "cpf": "32165498701"},
                        |"branch": "0001",
                        |"accountNumber": "12345-X",
                        |"money": 0.00
                    |}""".trimMargin()
                    )
                }
            }

            mockMvc.post("/account") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(accountRequestDTO)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isConflict() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content {
                    json(
                        """{
                            |"status": 409,
                            |"path": "/account",
                            |"error": "Account with bank code 456, branch 0001 and account number 12345-X already exists."
                        |}""".trimMargin()
                    )
                }
            }
        }
    }
}
