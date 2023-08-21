package br.com.usedrip.techcase.george.transferagent.integration

import br.com.usedrip.techcase.george.transferagent.TransferAgentApplication
import br.com.usedrip.techcase.george.transferagent.main
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TransferAgentApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferAgentApplicationTests {
    @Test
    fun contextLoads() {
        main(arrayOf())
    }
}
