package br.com.usedrip.techcase.george.transferagent.unittests.dtovalidation

import br.com.usedrip.techcase.george.transferagent.AccountIdentificationDTO
import br.com.usedrip.techcase.george.transferagent.TestConfig
import br.com.usedrip.techcase.george.transferagent.TransferRequestDTO
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@SpringBootTest
@Import(TestConfig::class)
class TransferRequestDTOValidationTests @Autowired constructor(val validator: Validator) {
    private val validAccountIdentificationDTO = AccountIdentificationDTO("001", "0001", "1010-0")

    @ParameterizedTest
    @ValueSource(strings = ["", "1", "0001", "12A", "A12", " 12", " 123", "123 "])
    fun `source account with a bank code made of anything but three digits is refused`(bankCode: String) {
        val transferRequestDTO = TransferRequestDTO(
            AccountIdentificationDTO(bankCode, "0001", "1010-0"),
            validAccountIdentificationDTO,
            BigDecimal.ONE
        )
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("sourceAccount.bankCode", violation.propertyPath.toString())
        assertEquals("Bank code must be a number with 3 digits", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1", "0001", "12A", "A12", " 12", " 123", "123 "])
    fun `destination account with a bank code made of anything but three digits is refused`(bankCode: String) {
        val transferRequestDTO = TransferRequestDTO(
            validAccountIdentificationDTO,
            AccountIdentificationDTO(bankCode, "0001", "1010-0"),
            BigDecimal.ONE
        )
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("destinationAccount.bankCode", violation.propertyPath.toString())
        assertEquals("Bank code must be a number with 3 digits", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `branch of source account cannot be blank`(branch: String) {
        val transferRequestDTO = TransferRequestDTO(
            AccountIdentificationDTO("001", branch, "1010-0"),
            validAccountIdentificationDTO,
            BigDecimal.ONE
        )
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("sourceAccount.branch", violation.propertyPath.toString())
        assertEquals("Branch is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `branch of destination account cannot be blank`(branch: String) {
        val transferRequestDTO = TransferRequestDTO(
            validAccountIdentificationDTO,
            AccountIdentificationDTO("001", branch, "1010-0"),
            BigDecimal.ONE
        )
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("destinationAccount.branch", violation.propertyPath.toString())
        assertEquals("Branch is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `account number of source account cannot be blank`(accountNumber: String) {
        val transferRequestDTO = TransferRequestDTO(
            AccountIdentificationDTO("001", "0001", accountNumber),
            validAccountIdentificationDTO,
            BigDecimal.ONE
        )
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("sourceAccount.accountNumber", violation.propertyPath.toString())
        assertEquals("Account number is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `account number of destination account cannot be blank`(accountNumber: String) {
        val transferRequestDTO = TransferRequestDTO(
            validAccountIdentificationDTO,
            AccountIdentificationDTO("001", "0001", accountNumber),
            BigDecimal.ONE
        )
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("destinationAccount.accountNumber", violation.propertyPath.toString())
        assertEquals("Account number is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "0.01", "123.45"])
    fun `amount can be declared with up to two decimal digits`(number: String) {
        val transferRequestDTO =
            TransferRequestDTO(validAccountIdentificationDTO, validAccountIdentificationDTO, BigDecimal(number))
        val result = validator.validate(transferRequestDTO)
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `amount must have up to 2 decimal places`() {
        val transferRequestDTO =
            TransferRequestDTO(validAccountIdentificationDTO, validAccountIdentificationDTO, BigDecimal("123.456"))
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("amount", violation.propertyPath.toString())
        assertEquals("Amount can only have up to 2 decimal places", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-1"])
    fun `amount must be greater than zero`(number: String) {
        val transferRequestDTO =
            TransferRequestDTO(validAccountIdentificationDTO, validAccountIdentificationDTO, BigDecimal(number))
        val result = validator.validate(transferRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("amount", violation.propertyPath.toString())
        assertEquals("Amount must be greater than zero", violation.message)
    }

    @Test
    fun `empty constructor fails validations`() {
        val transferRequestDTO = TransferRequestDTO()
        val result = validator.validate(transferRequestDTO)

        assertEquals(7, result.size)
        val expectedFieldsWithErrors = setOf(
            "sourceAccount.bankCode",
            "sourceAccount.branch",
            "sourceAccount.accountNumber",
            "destinationAccount.bankCode",
            "destinationAccount.branch",
            "destinationAccount.accountNumber",
            "amount"
        )
        result.forEach { Assertions.assertTrue(expectedFieldsWithErrors.contains(it.propertyPath.toString())) }
    }
}
