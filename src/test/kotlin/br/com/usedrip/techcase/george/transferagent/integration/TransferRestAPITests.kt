package br.com.usedrip.techcase.george.transferagent.integration

import br.com.usedrip.techcase.george.transferagent.*
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.util.stream.Stream

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferRestAPITests @Autowired constructor(val mockMvc: MockMvc) {
    companion object {
        private val bankDTO1 = BankDTO("Foobank", "123")
        private val bankDTO2 = BankDTO("My Bank", "456")
        private val customerDTO = CustomerDTO("Foo Bar", "32165498701")
        private val accountWithMoneyOnBank1 =
            AccountRequestDTO(bankDTO1.code, customerDTO.cpf, "0001", "12345-X", BigDecimal("5000.00"))
        private val accountWithoutMoneyOnBank1 = AccountRequestDTO(bankDTO1.code, customerDTO.cpf, "0001", "123-0")
        private val accountWithoutMoneyOnBank2 = AccountRequestDTO(bankDTO2.code, customerDTO.cpf, "0001", "1234-5")
        private val infoForAccountWithMoneyOnBank1 = AccountIdentificationDTO(
            bankDTO1.code,
            accountWithMoneyOnBank1.branch,
            accountWithMoneyOnBank1.accountNumber
        )
        private val infoForAccountWithoutMoneyOnBank1 = AccountIdentificationDTO(
            bankDTO1.code,
            accountWithoutMoneyOnBank1.branch,
            accountWithoutMoneyOnBank1.accountNumber
        )
        private val infoForAccountWithoutMoneyOnBank2 = AccountIdentificationDTO(
            bankDTO2.code,
            accountWithoutMoneyOnBank2.branch,
            accountWithoutMoneyOnBank2.accountNumber
        )

        @JvmStatic
        fun bogusAccountsData(): Stream<Arguments> {
            val infoForInexistentAccount = AccountIdentificationDTO("000", "0000", "00000")

            return Stream.of(
                Arguments.arguments(infoForAccountWithMoneyOnBank1, infoForInexistentAccount),
                Arguments.arguments(infoForInexistentAccount, infoForAccountWithMoneyOnBank1),
            )
        }
    }

    private val jsonMapper = JsonMapper.builder().addModule(JavaTimeModule()).build()

    @BeforeEach
    fun createBanksAndCustomerAndAccounts() {
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

        mockMvc.post("/customer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(customerDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountWithMoneyOnBank1)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountWithoutMoneyOnBank1)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }

        mockMvc.post("/account") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(accountWithoutMoneyOnBank2)
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isCreated() } }
    }

    @ParameterizedTest
    @MethodSource("bogusAccountsData")
    fun `returns status 422 Unprocessable Entity if source account or destination account is not in the database`(
        sourceAccountInfo: AccountIdentificationDTO,
        destinationAccountInfo: AccountIdentificationDTO,
    ) {
        val transferRequestDTO = TransferRequestDTO(sourceAccountInfo, destinationAccountInfo, BigDecimal.TEN)

        mockMvc.post("/transfer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(transferRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnprocessableEntity() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 422,
                        |"path": "/transfer",
                        |"error": "Account not found for bank code 000, branch 0000 and account number 00000"
                    |}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun `returns 400 Bad Request if trying to transfer from one account to the same account`() {
        val transferRequestDTO =
            TransferRequestDTO(infoForAccountWithMoneyOnBank1, infoForAccountWithMoneyOnBank1, BigDecimal.TEN)

        mockMvc.post("/transfer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(transferRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 400,
                        |"path": "/transfer",
                        |"error": "Transfers from one account to the same account are not allowed"
                    |}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun `returns 400 Bad Request if source account doesn't have enough funds for the transaction`() {
        val transferRequestDTO =
            TransferRequestDTO(infoForAccountWithoutMoneyOnBank1, infoForAccountWithMoneyOnBank1, BigDecimal.TEN)

        mockMvc.post("/transfer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(transferRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 400,
                        |"path": "/transfer",
                        |"error": "Transfer of R$ 10,00 requested, but source account has R$ 0,00"
                    |}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun `returns 400 Bad Request if source account doesn't have enough funds to pay the commission for inter-bank transfers`() {
        val transferRequestDTO =
            TransferRequestDTO(infoForAccountWithMoneyOnBank1, infoForAccountWithoutMoneyOnBank2, BigDecimal(5000))

        mockMvc.post("/transfer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(transferRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"status": 400,
                        |"path": "/transfer",
                        |"error": "Account does not have enough funds to pay the inter-bank R$ 5,00 commission"
                    |}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun `intra-bank transfers have no commission`() {
        val transferRequestDTO =
            TransferRequestDTO(infoForAccountWithMoneyOnBank1, infoForAccountWithoutMoneyOnBank1, BigDecimal(2000))

        mockMvc.post("/transfer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(transferRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"sourceAccount": {
                            |"bank": {
                                |"name": "Foobank",
                                |"code": "123"
                            |},
                            |"customer": {
                                |"fullName": "Foo Bar",
                                |"cpf": "32165498701"
                            |},
                            |"branch": "0001",
                            |"accountNumber": "12345-X",
                            |"money": 3000.00
                        |},
                        |"destinationAccount": {
                            |"bank": {
                                |"name": "Foobank",
                                |"code": "123"
                            |},
                            |"customer": {
                                |"fullName": "Foo Bar",
                                |"cpf": "32165498701"
                            |},
                            |"branch": "0001",
                            |"accountNumber":"123-0",
                            |"money": 2000.00
                        |},
                        |"amount": 2000.00,
                        |"commission": 0.00
                    |}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun `inter-bank transfers have commission`() {
        val transferRequestDTO =
            TransferRequestDTO(infoForAccountWithMoneyOnBank1, infoForAccountWithoutMoneyOnBank2, BigDecimal(2000))

        mockMvc.post("/transfer") {
            contentType = MediaType.APPLICATION_JSON
            content = jsonMapper.writeValueAsString(transferRequestDTO)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{
                        |"sourceAccount": {
                            |"bank": {
                                |"name": "Foobank",
                                |"code": "123"
                            |},
                            |"customer": {
                                |"fullName": "Foo Bar",
                                |"cpf": "32165498701"
                            |},
                            |"branch": "0001",
                            |"accountNumber": "12345-X",
                            |"money": 2995.00
                        |},
                        |"destinationAccount": {
                            |"bank": {
                                |"name": "My Bank",
                                |"code": "456"
                            |},
                            |"customer": {
                                |"fullName": "Foo Bar",
                                |"cpf": "32165498701"
                            |},
                            |"branch": "0001",
                            |"accountNumber":"1234-5",
                            |"money": 2000.00
                        |},
                        |"amount": 2000.00,
                        |"commission": 5.00
                    |}""".trimMargin()
                )
            }
        }
    }
}
