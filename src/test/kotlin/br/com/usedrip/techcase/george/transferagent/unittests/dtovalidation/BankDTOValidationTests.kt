package br.com.usedrip.techcase.george.transferagent.unittests.dtovalidation

import br.com.usedrip.techcase.george.transferagent.BankDTO
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
import java.time.Instant

@SpringBootTest
@Import(TestConfig::class)
class BankDTOValidationTests @Autowired constructor(val validator: Validator) {
    @ParameterizedTest
    @ValueSource(strings = ["0", "FOO 1", "Itaú", " A", "A "])
    fun `non-blank bank name is accepted`(name: String) {
        val bankDTO = BankDTO(name, "001")
        val result = validator.validate(bankDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `bank name cannot be blank`(name: String) {
        val bankDTO = BankDTO(name, "001")
        val result = validator.validate(bankDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("name", violation.propertyPath.toString())
        assertEquals("Name is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["000", "712", "999"])
    fun `bank codes made of three digits are accepted`(code: String) {
        val bankDTO = BankDTO("Foobank", code)
        val result = validator.validate(bankDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1", "0001", "12A", "A12", " 12", " 123", "123 "])
    fun `bank code made of anything but three digits is refused`(code: String) {
        val bankDTO = BankDTO("Foobank", code)
        val result = validator.validate(bankDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("code", violation.propertyPath.toString())
        assertEquals("Bank code must be a number with 3 digits", violation.message)
    }

    @Test
    fun `bank id must be null`() {
        var bankDTO = BankDTO("Foobank", "123", id = null)
        var result = validator.validate(bankDTO)
        assertTrue(result.isEmpty())

        bankDTO = BankDTO("Foobank", "123", id = 1L)
        result = validator.validate(bankDTO)
        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("id", violation.propertyPath.toString())
        assertEquals("Field id is not allowed", violation.message)
    }

    @Test
    fun `bank createdAt must be null`() {
        var bankDTO = BankDTO("Foobank", "123", createdAt = null)
        var result = validator.validate(bankDTO)
        assertTrue(result.isEmpty())

        bankDTO = BankDTO("Foobank", "123", createdAt = Instant.now())
        result = validator.validate(bankDTO)
        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("createdAt", violation.propertyPath.toString())
        assertEquals("Field createdAt is not allowed", violation.message)
    }

    @Test
    fun `bank updatedAt must be null`() {
        var bankDTO = BankDTO("Foobank", "123", updatedAt = null)
        var result = validator.validate(bankDTO)
        assertTrue(result.isEmpty())

        bankDTO = BankDTO("Foobank", "123", updatedAt = Instant.now())
        result = validator.validate(bankDTO)
        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("updatedAt", violation.propertyPath.toString())
        assertEquals("Field updatedAt is not allowed", violation.message)
    }

    @Test
    fun `empty constructor fails validations`() {
        val bankDTO = BankDTO()
        val result = validator.validate(bankDTO)

        assertEquals(2, result.size)
        val expectedFieldsWithErrors = setOf("name", "code")
        result.forEach { assertTrue(expectedFieldsWithErrors.contains(it.propertyPath.toString())) }
    }
}
