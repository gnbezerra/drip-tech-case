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
class AccountControllerTests @Autowired constructor(val factory: PodamFactory) {
    @Mock
    private val accountRepository: AccountRepository? = null

    @Mock
    private val bankRepository: BankRepository? = null

    @Mock
    private val customerRepository: CustomerRepository? = null

    @Spy
    private val modelMapper: ModelMapper? = null

    @InjectMocks
    private val accountController: AccountController? = null

    @Captor
    private val accountCaptor: ArgumentCaptor<Account>? = null

    @Nested
    inner class FindAll {
        private lateinit var accountsResponse: Iterable<Account>

        @BeforeEach
        fun setUp() {
            accountsResponse =
                listOf(factory.manufacturePojo(Account::class.java), factory.manufacturePojo(Account::class.java))
            whenever(accountRepository?.findAll()).thenReturn(accountsResponse)
        }

        @Test
        fun `findAll() returns list of accounts`() {
            val expectedResponse = modelMapper?.map(accountsResponse, AccountDTO::class.java)

            val accountDTOs = accountController?.findAll()

            assertEquals(expectedResponse, accountDTOs)
            verify(accountRepository)?.findAll()
        }
    }

    @Nested
    inner class Save {
        private lateinit var accountResponse: Account
        private lateinit var accountRequestDTO: AccountRequestDTO

        @BeforeEach
        fun setUp() {
            accountResponse = factory.manufacturePojo(Account::class.java)
            accountRequestDTO = factory.manufacturePojo(AccountRequestDTO::class.java)
        }

        @Test
        fun `save() calls account repository save operation and returns saved customer`() {
            whenever(bankRepository?.findByCode(isA())).thenReturn(accountResponse.bank)
            whenever(customerRepository?.findByCpf(isA())).thenReturn(accountResponse.customer)
            whenever(accountRepository?.save(isA())).thenReturn(accountResponse)
            val expectedAccountResponse = modelMapper?.map(accountResponse, AccountDTO::class.java)

            val response = accountController?.save(accountRequestDTO)

            assertEquals(expectedAccountResponse, response)
            verify(bankRepository)?.findByCode(accountRequestDTO.bankCode)
            verify(customerRepository)?.findByCpf(accountRequestDTO.customerCpf)

            verify(accountRepository)?.save(accountCaptor!!.capture())
            val accountToSave = accountCaptor?.value
            assertEquals(accountResponse.bank.id, accountToSave?.bank?.id)
            assertEquals(accountResponse.customer.id, accountToSave?.customer?.id)
            assertEquals(accountRequestDTO.branch, accountToSave?.branch)
            assertEquals(accountRequestDTO.accountNumber, accountToSave?.accountNumber)
            assertEquals(accountRequestDTO.money, accountToSave?.money)
        }

        @Test
        fun `throws BankNotFoundException if bank code does not exist`() {
            whenever(bankRepository?.findByCode(isA())).thenReturn(null)

            assertThrows<BankNotFoundException> { accountController?.save(accountRequestDTO) }
        }

        @Test
        fun `throws CustomerNotFoundException if customer cpf does not exist`() {
            whenever(bankRepository?.findByCode(isA())).thenReturn(accountResponse.bank)
            whenever(customerRepository?.findByCpf(isA())).thenReturn(null)

            assertThrows<CustomerNotFoundException> { accountController?.save(accountRequestDTO) }
        }

        @Test
        fun `throws AccountAlreadyExistsException if the bank code, branch and account number combination already exists`() {
            whenever(accountRepository?.existsByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA())).thenReturn(true)

            assertThrows<AccountAlreadyExistsException> { accountController?.save(accountRequestDTO) }
        }
    }
}
