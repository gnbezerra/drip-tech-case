package br.com.usedrip.techcase.george.transferagent

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import uk.co.jemos.podam.api.PodamFactory
import uk.co.jemos.podam.api.PodamFactoryImpl

@TestConfiguration
class TestConfig {
    @Bean
    fun factory(): PodamFactory {
        return PodamFactoryImpl()
    }

    @Bean
    fun validator(): Validator {
        return Validation.buildDefaultValidatorFactory().validator
    }
}
