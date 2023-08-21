package br.com.usedrip.techcase.george.transferagent

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.random.Random

@Suppress("unused", "RedundantSuppression")
@Service
class TransferService @Autowired constructor(
    @field:Qualifier("intraBankTransferStrategy") var intraBankTransferStrategy: TransferStrategy,
    @field:Qualifier("intraBankTransferStrategy") var interBankTransferStrategy: TransferStrategy,
) {
    fun transfer(
        sourceAccount: Account,
        moneyDecreaseOnSource: BigDecimal,
        destinationAccount: Account,
        moneyIncreaseOnDestination: BigDecimal,
    ) {
        val transferContext = TransferContext()

        val isSameBankTransfer = sourceAccount.bank.id == destinationAccount.bank.id
        transferContext.strategy = when {
            isSameBankTransfer -> intraBankTransferStrategy
            else -> interBankTransferStrategy
        }

        transferContext.executeStrategy(sourceAccount, destinationAccount)
    }
}

class TransferContext(var strategy: TransferStrategy? = null) {
    fun executeStrategy(sourceAccount: Account, destinationAccount: Account) {
        strategy?.execute(sourceAccount, destinationAccount)
    }
}

interface TransferStrategy {
    fun execute(sourceAccount: Account, destinationAccount: Account)
}

@Component("intraBankTransferStrategy")
class IntraBankDummyTransferStrategy : TransferStrategy {
    override fun execute(sourceAccount: Account, destinationAccount: Account) {
        // This is the part where we would make the actual transfer transaction
    }
}

@Component("interBankTransferStrategy")
class InterBankDummyTransferStrategy @Autowired constructor(var randomNumberGenerator: Random) :
    TransferStrategy {
    companion object {
        const val INTER_BANK_TRANSFER_FAILURE_CHANCE = 0.3

        /* Retry constants */
        const val MAX_RETRY_ATTEMPTS = 6  // enough for a success rate of 99.93%, use 8 for 99.99%

        // low delay times for this tech case just so that tests don't take too long
        // should be taken to a separate backoff template class if reused
        const val INITIAL_RETRY_DELAY_IN_MILLISECONDS = 100L
        const val MAX_RETRY_DELAY_IN_MILLISECONDS = 1000L
        const val EXPONENTIAL_BACKOFF_MULTIPLIER = 2.0
        const val SHOULD_ADD_RANDOMNESS_TO_DELAY_TIME = true
        /* End retry constants */
    }

    private val logger: Logger = LoggerFactory.getLogger(InterBankDummyTransferStrategy::class.java)

    @Retryable(
        retryFor = [TransferServiceFailureException::class],
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = Backoff(
            delay = INITIAL_RETRY_DELAY_IN_MILLISECONDS,
            maxDelay = MAX_RETRY_DELAY_IN_MILLISECONDS,
            multiplier = EXPONENTIAL_BACKOFF_MULTIPLIER,
            random = SHOULD_ADD_RANDOMNESS_TO_DELAY_TIME,
        )
    )
    override fun execute(sourceAccount: Account, destinationAccount: Account) {
        if (randomNumberGenerator.nextDouble() < INTER_BANK_TRANSFER_FAILURE_CHANCE) {
            logger.debug("Inter-bank transfer between accounts ${sourceAccount.id} and ${destinationAccount.id} failed!")
            throw TransferServiceFailureException()
        }

        // This is the part where we would make the actual transfer transaction
    }
}
