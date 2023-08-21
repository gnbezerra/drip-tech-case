package br.com.usedrip.techcase.george.transferagent.unittests.controller

import br.com.usedrip.techcase.george.transferagent.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ControllerCreationTests @Autowired constructor(
    private val apiInfoController: ApiInfoController,
    private val bankController: BankController,
    private val customerController: CustomerController,
    private val accountController: AccountController,
    private val transferController: TransferController,
) {
    @Test
    fun `can create API info controller`() {
        assertNotNull(apiInfoController)
    }

    @Test
    fun `can create bank controller`() {
        assertNotNull(bankController)
    }

    @Test
    fun `can create customer controller`() {
        assertNotNull(customerController)
    }

    @Test
    fun `can create account controller`() {
        assertNotNull(accountController)
    }

    @Test
    fun `can create transfer controller`() {
        assertNotNull(transferController)
    }
}
