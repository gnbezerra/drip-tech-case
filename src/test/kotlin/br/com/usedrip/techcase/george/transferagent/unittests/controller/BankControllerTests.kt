package br.com.usedrip.techcase.george.transferagent.unittests.controller

import br.com.usedrip.techcase.george.transferagent.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.*
import org.mockito.kotlin.isA
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import uk.co.jemos.podam.api.PodamFactory

@SpringBootTest
@Import(TestConfig::class)
class BankControllerTests @Autowired constructor(val factory: PodamFactory) {
    @Mock
    private val bankRepository: BankRepository? = null

    @Spy
    private val modelMapper: ModelMapper? = null

    @InjectMocks
    private val bankController: BankController? = null

    @Captor
    private val bankCaptor: ArgumentCaptor<Bank>? = null

    @Nested
    inner class FindAll {
        private lateinit var banksResponse: Iterable<Bank>

        @BeforeEach
        fun setUp() {
            banksResponse =
                listOf(factory.manufacturePojoWithFullData(Bank::class.java), factory.manufacturePojo(Bank::class.java))
            whenever(bankRepository?.findAll()).thenReturn(banksResponse)
        }

        @Test
        fun `findAll() returns list of banks`() {
            val expectedResponse = modelMapper?.map(banksResponse, BankDTO::class.java)

            val bankDTOs = bankController?.findAll()

            assertEquals(expectedResponse, bankDTOs)
            verify(bankRepository)?.findAll()
        }
    }

    @Nested
    inner class Save {
        private lateinit var bankDTORequest: BankDTO

        @BeforeEach
        fun setUp() {
            bankDTORequest = factory.manufacturePojo(BankDTO::class.java)
        }

        @Test
        fun `save() calls bank repository save operation and returns saved bank`() {
            val mockedBankResponse = factory.manufacturePojoWithFullData(Bank::class.java)
            whenever(bankRepository?.save(isA())).thenReturn(mockedBankResponse)
            val expectedBankDTOResponse = modelMapper?.map(mockedBankResponse, BankDTO::class.java)

            val response = bankController?.save(bankDTORequest)

            assertEquals(expectedBankDTOResponse, response)

            verify(bankRepository)?.save(bankCaptor!!.capture())
            val bankToSave = bankCaptor?.value
            assertEquals(bankDTORequest.name, bankToSave?.name)
            assertEquals(bankDTORequest.code, bankToSave?.code)
            assertEquals(bankDTORequest.id, bankToSave?.id)
            assertEquals(bankDTORequest.createdAt, bankToSave?.createdAt)
            assertEquals(bankDTORequest.updatedAt, bankToSave?.updatedAt)
        }

        @Test
        fun `throws BankCodeAlreadyExistsException when the repository already has a bank with the same code`() {
            whenever(bankRepository?.existsByCode(isA())).thenReturn(true)

            assertThrows<BankCodeAlreadyExistsException> { bankController?.save(bankDTORequest) }
        }
    }
}
