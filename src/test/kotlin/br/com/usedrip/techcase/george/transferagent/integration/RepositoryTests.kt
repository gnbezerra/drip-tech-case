package br.com.usedrip.techcase.george.transferagent.integration

import br.com.usedrip.techcase.george.transferagent.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.math.BigDecimal
import java.util.stream.Stream

@DataJpaTest
class BankRepositoryTests @Autowired constructor(val bankRepository: BankRepository) {
    private var bank: Bank? = null

    @BeforeEach
    fun setUp() {
        bank = bankRepository.save(Bank("Foobank", "123"))
    }

    @Test
    fun `find bank by code`() {
        val result = bankRepository.findByCode("123")

        assertNotNull(result)
        assertEquals(bank?.id, result?.id)
        assertEquals(bank?.name, result?.name)
        assertEquals(bank?.code, result?.code)
        assertEquals(bank?.createdAt, result?.createdAt)
        assertEquals(bank?.updatedAt, result?.updatedAt)
    }

    @Test
    fun `bank code does not exist`() {
        val result = bankRepository.findByCode("456")

        assertNull(result)
    }
}

@DataJpaTest
class CustomerRepositoryTests @Autowired constructor(val customerRepository: CustomerRepository) {
    private var customer: Customer? = null

    @BeforeEach
    fun setUp() {
        customer = customerRepository.save(Customer("Foolano de Tal", "98765432101"))
    }

    @Test
    fun `find customer by cpf`() {
        val result = customerRepository.findByCpf("98765432101")

        assertNotNull(result)
        assertEquals(customer?.id, result?.id)
        assertEquals(customer?.fullName, result?.fullName)
        assertEquals(customer?.cpf, result?.cpf)
        assertEquals(customer?.createdAt, result?.createdAt)
        assertEquals(customer?.updatedAt, result?.updatedAt)
    }

    @Test
    fun `customer cpf does not exist`() {
        val result = customerRepository.findByCpf("01234567890")

        assertNull(result)
    }
}

@DataJpaTest
class AccountRepositoryTests @Autowired constructor(
    val accountRepository: AccountRepository,
    val bankRepository: BankRepository,
    val customerRepository: CustomerRepository,
) {
    private var account: Account? = null

    @BeforeEach
    fun setUp() {
        val bank = bankRepository.save(Bank("Itafoo", "001"))
        val customer = customerRepository.save(Customer("Foormiga", "00100200304"))
        account = accountRepository.save(Account(bank, customer, "0001", "12345-X", BigDecimal.TEN))
    }

    @Test
    fun `find account by identification data`() {
        val result = accountRepository.findByBankCodeAndBranchAndAccountNumber("001", "0001", "12345-X")

        assertNotNull(result)
        assertEquals(account?.id, result?.id)
        assertEquals(account?.bank?.id, result?.bank?.id)
        assertEquals(account?.customer?.id, result?.customer?.id)
        assertEquals(account?.branch, result?.branch)
        assertEquals(account?.accountNumber, result?.accountNumber)
        assertEquals(account?.createdAt, result?.createdAt)
        assertEquals(account?.updatedAt, result?.updatedAt)
    }

    @ParameterizedTest
    @MethodSource("bogusAccountIdentificationDataProvider")
    fun `account identification data does not exist`(bankCode: String, branch: String, accountNumber: String) {
        val result = accountRepository.findByBankCodeAndBranchAndAccountNumber(bankCode, branch, accountNumber)

        assertNull(result)
    }

    companion object {
        @JvmStatic
        fun bogusAccountIdentificationDataProvider(): Stream<Arguments> {
            return Stream.of(
                arguments("001", "0001", "12345-6"),
                arguments("001", "0002", "12345-X"),
                arguments("002", "0001", "12345-X")
            )
        }
    }
}

@DataJpaTest
class TransferLogRepositoryTests @Autowired constructor(val transferLogRepository: TransferLogRepository) {
    @Test
    fun `can create transfer log repository`() {
        assertNotNull(transferLogRepository)
    }
}
