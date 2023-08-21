package br.com.usedrip.techcase.george.transferagent.unittests.dtovalidation

import br.com.usedrip.techcase.george.transferagent.CustomerDTO
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
class CustomerDTOValidationTests @Autowired constructor(val validator: Validator) {
    @ParameterizedTest
    @ValueSource(strings = ["0", "FULANO 1", "Foo", " A", "A "])
    fun `non-blank customer full name is accepted`(fullName: String) {
        val customerDTO = CustomerDTO(fullName, "12345678901")
        val result = validator.validate(customerDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "     "])
    fun `customer full name cannot be blank`(name: String) {
        val customerDTO = CustomerDTO(name, "12345678901")
        val result = validator.validate(customerDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("fullName", violation.propertyPath.toString())
        assertEquals("Full name is required", violation.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["00000000000", "12345678901", "99999999999"])
    fun `customer cpf made of 11 digits are accepted`(cpf: String) {
        val customerDTO = CustomerDTO("Foo Customer", cpf)
        val result = validator.validate(customerDTO)
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " 12345678901", "12345678901 ", "1234567890", "123.456.789-01"])
    fun `customer cpf made of anything but 11 digits is refused`(cpf: String) {
        val customerDTO = CustomerDTO("Foo Customer", cpf)
        val result = validator.validate(customerDTO)

        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("cpf", violation.propertyPath.toString())
        assertEquals("CPF must be a number with 11 digits, with no dashes or dots", violation.message)
    }

    @Test
    fun `customer id must be null`() {
        var customerDTO = CustomerDTO("Foo Customer", "12345678901", id = null)
        var result = validator.validate(customerDTO)
        assertTrue(result.isEmpty())

        customerDTO = CustomerDTO("Foo Customer", "12345678901", id = 1L)
        result = validator.validate(customerDTO)
        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("id", violation.propertyPath.toString())
        assertEquals("Field id is not allowed", violation.message)
    }

    @Test
    fun `customer createdAt must be null`() {
        var customerDTO = CustomerDTO("Foo Customer", "12345678901", createdAt = null)
        var result = validator.validate(customerDTO)
        assertTrue(result.isEmpty())

        customerDTO = CustomerDTO("Foo Customer", "12345678901", createdAt = Instant.now())
        result = validator.validate(customerDTO)
        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("createdAt", violation.propertyPath.toString())
        assertEquals("Field createdAt is not allowed", violation.message)
    }

    @Test
    fun `customer updatedAt must be null`() {
        var customerDTO = CustomerDTO("Foo Customer", "12345678901", updatedAt = null)
        var result = validator.validate(customerDTO)
        assertTrue(result.isEmpty())

        customerDTO = CustomerDTO("Foo Customer", "12345678901", updatedAt = Instant.now())
        result = validator.validate(customerDTO)
        assertEquals(1, result.size)
        val violation = result.iterator().next()
        assertEquals("updatedAt", violation.propertyPath.toString())
        assertEquals("Field updatedAt is not allowed", violation.message)
    }

    @Test
    fun `empty constructor fails validations`() {
        val customerDTO = CustomerDTO()
        val result = validator.validate(customerDTO)

        assertEquals(2, result.size)
        val expectedFieldsWithErrors = setOf("fullName", "cpf")
        result.forEach { assertTrue(expectedFieldsWithErrors.contains(it.propertyPath.toString())) }
    }
}
