package br.com.usedrip.techcase.george.transferagent.unittests.dtovalidation

import br.com.usedrip.techcase.george.transferagent.AccountRequestDTO
import br.com.usedrip.techcase.george.transferagent.TestConfig
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@SpringBootTest
@Import(TestConfig::class)
class AccountRequestDTOValidationTests @Autowired constructor(val validator: Validator) {
    @ParameterizedTest
    @ValueSource(strings = ["000", "712", "999"])
    fun `bank codes made of three digits are accepted`(bankCode: String) {
        val accountRequestDTO = AccountRequestDTO(bankCode, "12345678901", "0001", "12345-0")
        val result = validator.validate(accountRequestDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1", "0001", "12A", "A12", " 12", " 123", "123 "])
    fun `bank code made of anything but three digits is refused`(bankCode: String) {
        val accountRequestDTO = AccountRequestDTO(bankCode, "12345678901", "0001", "12345-0")
        val result = validator.validate(accountRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("bankCode", violation.propertyPath.toString())
        assertEquals("Bank code must be a number with 3 digits", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["00000000000", "12345678901", "99999999999"])
    fun `customer cpf made of 11 digits are accepted`(customerCpf: String) {
        val accountRequestDTO = AccountRequestDTO("001", customerCpf, "0001", "12345-0")
        val result = validator.validate(accountRequestDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " 12345678901", "12345678901 ", "1234567890", "123.456.789-01"])
    fun `customer cpf made of anything but 11 digits is refused`(customerCpf: String) {
        val accountRequestDTO = AccountRequestDTO("001", customerCpf, "0001", "12345-0")
        val result = validator.validate(accountRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("customerCpf", violation.propertyPath.toString())
        assertEquals("CPF must be a number with 11 digits, with no dashes or dots", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "0001", "Branch", " 0", "0 "])
    fun `non-blank branch is accepted`(branch: String) {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", branch, "12345-0")
        val result = validator.validate(accountRequestDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `branch cannot be blank`(branch: String) {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", branch, "12345-0")
        val result = validator.validate(accountRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("branch", violation.propertyPath.toString())
        assertEquals("Branch is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "123", "578156-X", " 0", "0 "])
    fun `non-blank account number is accepted`(accountNumber: String) {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", "0001", accountNumber)
        val result = validator.validate(accountRequestDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `account number cannot be blank`(accountNumber: String) {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", "0001", accountNumber)
        val result = validator.validate(accountRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("accountNumber", violation.propertyPath.toString())
        assertEquals("Account number is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "1.00", "123.45"])
    fun `money can be declared with up to two decimal digits`(number: String) {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", "0001", "12345-0", BigDecimal(number))
        val result = validator.validate(accountRequestDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["123.456", "0.000"])
    fun `money must have up to 2 decimal places`(number: String) {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", "0001", "12345-0", BigDecimal(number))
        val result = validator.validate(accountRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("money", violation.propertyPath.toString())
        assertEquals("Money can only have up to 2 decimal places", violation.message)
    }

    @Test
    fun `money cannot be negative`() {
        val accountRequestDTO = AccountRequestDTO("001", "12345678901", "0001", "12345-0", BigDecimal("-1"))
        val result = validator.validate(accountRequestDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("money", violation.propertyPath.toString())
        assertEquals("Money cannot be negative", violation.message)
    }

    @Test
    fun `empty constructor fails validations`() {
        val accountRequestDTO = AccountRequestDTO()
        val result = validator.validate(accountRequestDTO)

        assertEquals(4, result.size)
        val expectedFieldsWithErrors = setOf("bankCode", "customerCpf", "branch", "accountNumber")
        result.forEach { assertTrue(expectedFieldsWithErrors.contains(it.propertyPath.toString())) }
    }
}
