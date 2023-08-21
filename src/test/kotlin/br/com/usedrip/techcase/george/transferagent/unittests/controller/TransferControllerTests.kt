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
import java.math.BigDecimal

@SpringBootTest
@Import(TestConfig::class)
class TransferControllerTests @Autowired constructor(val factory: PodamFactory) {
    @Mock
    private val accountRepository: AccountRepository? = null

    @Spy
    private val modelMapper: ModelMapper? = null

    @Mock
    private val transferLogRepository: TransferLogRepository? = null

    @Mock
    private val transferService: TransferService? = null

    @InjectMocks
    private val transferController: TransferController? = null

    @Captor
    private val accountIterableCaptor: ArgumentCaptor<Iterable<Account>>? = null

    @Captor
    private val transferLogCaptor: ArgumentCaptor<TransferLog>? = null

    private lateinit var sourceAccount: Account
    private lateinit var destinationAccount: Account
    private lateinit var transferRequestDTO: TransferRequestDTO

    @BeforeEach
    fun setUp() {
        sourceAccount = factory.manufacturePojo(Account::class.java)
        destinationAccount = factory.manufacturePojo(Account::class.java)
        transferRequestDTO = factory.manufacturePojo(TransferRequestDTO::class.java)
    }

    @Test
    fun `cannot transfer from an account that does not exist`() {
        whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
            .thenReturn(null, destinationAccount)

        assertThrows<AccountNotFoundException> { transferController?.transfer(transferRequestDTO) }
    }

    @Test
    fun `cannot transfer to an account that does not exist`() {
        whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
            .thenReturn(sourceAccount, null)

        assertThrows<AccountNotFoundException> { transferController?.transfer(transferRequestDTO) }
    }

    @Test
    fun `cannot transfer from an account to the same account`() {
        whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
            .thenReturn(sourceAccount)

        assertThrows<OperationNotAllowedException> { transferController?.transfer(transferRequestDTO) }
    }

    @Test
    fun `cannot transfer if funds are insuficient`() {
        sourceAccount.money = BigDecimal("500.00")
        destinationAccount.bank.id = sourceAccount.bank.id
        whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
            .thenReturn(sourceAccount, destinationAccount)
        transferRequestDTO.amount = BigDecimal("500.01")

        assertThrows<InsufficientFundsException> { transferController?.transfer(transferRequestDTO) }
    }

    @Test
    fun `cannot perform inter-bank transfer if there are no leftover funds to pay commission`() {
        sourceAccount.money = BigDecimal("500.00")
        sourceAccount.bank.id = 1L
        destinationAccount.bank.id = 2L
        whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
            .thenReturn(sourceAccount, destinationAccount)
        transferRequestDTO.amount = BigDecimal("495.01")

        assertThrows<InsufficientFundsException> { transferController?.transfer(transferRequestDTO) }
    }

    @Test
    fun `test transfer service failure`() {
        sourceAccount.money = BigDecimal("500.00")
        whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
            .thenReturn(sourceAccount, destinationAccount)
        whenever(transferService?.transfer(isA(), isA(), isA(), isA())).then { throw TransferServiceFailureException() }
        transferRequestDTO.amount = BigDecimal("100.00")

        assertThrows<TransferServiceFailureException> { transferController?.transfer(transferRequestDTO) }
    }

    @Nested
    inner class SuccessCases {
        private var transferLog = factory.manufacturePojo(TransferLog::class.java)
        private var expectedResult = TransferLogDTO()

        @BeforeEach
        fun setUp() {
            whenever(accountRepository?.findByBankCodeAndBranchAndAccountNumber(isA(), isA(), isA()))
                .thenReturn(sourceAccount, destinationAccount)
            transferLog = factory.manufacturePojo(TransferLog::class.java)
            whenever(transferLogRepository?.save(isA())).thenReturn(transferLog)
            expectedResult = modelMapper?.map(transferLog, TransferLogDTO::class.java)!!
        }

        @Test
        fun `performs intra-bank transfer when all business rules pass`() {
            sourceAccount.money = BigDecimal("500.00")
            destinationAccount.bank.id = sourceAccount.bank.id
            destinationAccount.money = BigDecimal("300.00")
            transferRequestDTO.amount = BigDecimal("499.99")

            val result = transferController?.transfer(transferRequestDTO)

            assertEquals(expectedResult, result)
            verify(transferService)?.transfer(
                sourceAccount = sourceAccount,
                moneyDecreaseOnSource = BigDecimal("499.99"),
                destinationAccount = destinationAccount,
                moneyIncreaseOnDestination = BigDecimal("499.99")
            )

            verify(accountRepository)?.saveAll(accountIterableCaptor!!.capture())
            val accountsToSave = accountIterableCaptor?.value?.toList()
            assertEquals(sourceAccount.id, accountsToSave?.get(0)?.id)
            assertEquals(BigDecimal("0.01"), accountsToSave?.get(0)?.money)
            assertEquals(destinationAccount.id, accountsToSave?.get(1)?.id)
            assertEquals(BigDecimal("799.99"), accountsToSave?.get(1)?.money)

            verify(transferLogRepository)?.save(transferLogCaptor!!.capture())
            val transferLogToSave = transferLogCaptor?.value
            assertEquals(sourceAccount.id, transferLogToSave?.sourceAccount?.id)
            assertEquals(destinationAccount.id, transferLogToSave?.destinationAccount?.id)
            assertEquals(BigDecimal("499.99"), transferLogToSave?.amount)
            assertEquals(BigDecimal.ZERO, transferLogToSave?.commission)
        }

        @Test
        fun `performs inter-bank transfer when all business rules pass`() {
            sourceAccount.bank.id = 1L
            sourceAccount.money = BigDecimal("500.00")
            destinationAccount.bank.id = 2L
            destinationAccount.money = BigDecimal("300.00")
            transferRequestDTO.amount = BigDecimal("400.00")

            val result = transferController?.transfer(transferRequestDTO)

            assertEquals(expectedResult, result)
            verify(transferService)?.transfer(
                sourceAccount = sourceAccount,
                moneyDecreaseOnSource = BigDecimal("400.00") + TransferController.INTER_BANK_TRANSFER_COMMISSION,
                destinationAccount = destinationAccount,
                moneyIncreaseOnDestination = BigDecimal("400.00")
            )

            verify(accountRepository)?.saveAll(accountIterableCaptor!!.capture())
            val accountsToSave = accountIterableCaptor?.value?.toList()
            assertEquals(sourceAccount.id, accountsToSave?.get(0)?.id)
            assertEquals(
                BigDecimal("100.00") - TransferController.INTER_BANK_TRANSFER_COMMISSION,
                accountsToSave?.get(0)?.money
            )
            assertEquals(destinationAccount.id, accountsToSave?.get(1)?.id)
            assertEquals(BigDecimal("700.00"), accountsToSave?.get(1)?.money)

            verify(transferLogRepository)?.save(transferLogCaptor!!.capture())
            val transferLogToSave = transferLogCaptor?.value
            assertEquals(sourceAccount.id, transferLogToSave?.sourceAccount?.id)
            assertEquals(destinationAccount.id, transferLogToSave?.destinationAccount?.id)
            assertEquals(BigDecimal("400.00"), transferLogToSave?.amount)
            assertEquals(TransferController.INTER_BANK_TRANSFER_COMMISSION, transferLogToSave?.commission)
        }
    }
}
