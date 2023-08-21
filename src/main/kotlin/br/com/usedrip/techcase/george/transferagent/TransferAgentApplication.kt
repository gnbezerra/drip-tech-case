package br.com.usedrip.techcase.george.transferagent

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@SpringBootApplication
class TransferAgentApplication

fun main(args: Array<String>) {
    runApplication<TransferAgentApplication>(*args)
}
