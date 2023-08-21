package br.com.usedrip.techcase.george.transferagent.unittests

import br.com.usedrip.techcase.george.transferagent.Bank
import br.com.usedrip.techcase.george.transferagent.BankDTO
import br.com.usedrip.techcase.george.transferagent.ModelMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ModelMapperTests {
    private val modelMapper = ModelMapper()

    @Test
    fun `maps an Iterable to another Iterable and maps back to an identical iterable`() {
        val now = Instant.now()
        val bankDTOs: Iterable<BankDTO> = listOf(BankDTO("", ""), BankDTO("Foobank", "123", 1L, now, now))

        val banks = modelMapper.map(bankDTOs, Bank::class.java)
        val banksIterator = banks.iterator()

        assertTrue(banksIterator.hasNext())
        var bank = banksIterator.next()
        assertInstanceOf(Bank::class.java, bank)
        assertEquals("", bank.name)
        assertEquals("", bank.code)
        assertNull(bank.id)
        assertNull(bank.createdAt)
        assertNull(bank.updatedAt)

        assertTrue(banksIterator.hasNext())
        bank = banksIterator.next()
        assertInstanceOf(Bank::class.java, bank)
        assertEquals("Foobank", bank.name)
        assertEquals("123", bank.code)
        assertEquals(1L, bank.id)
        assertEquals(now, bank.createdAt)
        assertEquals(now, bank.updatedAt)

        val mappedBankDTOs = modelMapper.map(banks, BankDTO::class.java)

        assertEquals(bankDTOs, mappedBankDTOs)
    }

    @Test
    fun `mapping an empty iterable returns an empty iterable`() {
        val emptyIterable = listOf<String>()

        val resultingIterable = modelMapper.map(emptyIterable, Int::class.java)

        assertFalse(resultingIterable.iterator().hasNext())
    }
}
