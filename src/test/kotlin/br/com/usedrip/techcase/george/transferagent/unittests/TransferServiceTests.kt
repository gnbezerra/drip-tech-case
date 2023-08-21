package br.com.usedrip.techcase.george.transferagent.unittests

import br.com.usedrip.techcase.george.transferagent.*
import br.com.usedrip.techcase.george.transferagent.InterBankDummyTransferStrategy.Companion.MAX_RETRY_ATTEMPTS
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import uk.co.jemos.podam.api.PodamFactory
import java.math.BigDecimal.TEN
import kotlin.random.Random

@SpringBootTest
@Import(TestConfig::class)
class TransferServiceTests @Autowired constructor(
    @field:MockBean val randomNumberGenerator: Random,
    @field:SpyBean @field:Qualifier("intraBankTransferStrategy") val intraBankTransferStrategy: TransferStrategy,
    @field:SpyBean @field:Qualifier("interBankTransferStrategy") val interBankTransferStrategy: TransferStrategy,
    val transferService: TransferService,
    val factory: PodamFactory,
) {
    private lateinit var sourceAccount: Account
    private lateinit var destinationAccount: Account

    @BeforeEach
    fun setUp() {
        sourceAccount = factory.manufacturePojo(Account::class.java)
        destinationAccount = factory.manufacturePojo(Account::class.java)
    }

    @Test
    fun `creates transfer service with all dependencies`() {
        assertNotNull(transferService)
        assertNotNull(randomNumberGenerator)
        assertNotNull(transferService.intraBankTransferStrategy)
        assertNotNull(transferService.interBankTransferStrategy)
        assertNotNull(
            (transferService.interBankTransferStrategy as? InterBankDummyTransferStrategy)?.randomNumberGenerator
        )
    }

    @Nested
    inner class IntraBankTransferTests {
        @BeforeEach
        fun setUp() {
            sourceAccount.bank.id = destinationAccount.bank.id
        }

        @Test
        fun `intra bank dummy strategy does nothing`() {
            transferService.transfer(sourceAccount, TEN, destinationAccount, TEN)

            verify(intraBankTransferStrategy).execute(sourceAccount, destinationAccount)
        }
    }

    @Nested
    inner class InterBankTranferTests {
        @BeforeEach
        fun setUp() {
            sourceAccount.bank.id = 1L
            destinationAccount.bank.id = 2L
        }

        @Test
        fun `inter bank dummy strategy runs once when RNG succeeds in the first call`() {
            whenever(randomNumberGenerator.nextDouble()).thenReturn(1.0)

            assertDoesNotThrow { transferService.transfer(sourceAccount, TEN, destinationAccount, TEN) }

            verify(interBankTransferStrategy).execute(sourceAccount, destinationAccount)
        }

        @Test
        fun `inter bank dummy strategy runs three times when RNG fails in the first two calls`() {
            whenever(randomNumberGenerator.nextDouble()).thenReturn(0.0, 0.0, 1.0)

            assertDoesNotThrow { transferService.transfer(sourceAccount, TEN, destinationAccount, TEN) }

            verify(interBankTransferStrategy, times(3)).execute(sourceAccount, destinationAccount)
        }

        @Test
        fun `inter bank dummy strategy throws exception after failing many times`() {
            whenever(randomNumberGenerator.nextDouble()).thenReturn(0.0)

            assertThrows<TransferServiceFailureException> {
                transferService.transfer(sourceAccount, TEN, destinationAccount, TEN)
            }

            verify(interBankTransferStrategy, times(MAX_RETRY_ATTEMPTS)).execute(sourceAccount, destinationAccount)
        }
    }
}
