package br.com.usedrip.techcase.george.transferagent

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import kotlin.random.Random

@Configuration
class AppConfig {
    @Bean
    fun clock(): Clock {
        return Clock.systemDefaultZone()
    }

    @Bean
    fun modelMapper(): ModelMapper {
        return ModelMapper()
    }

    @Bean
    fun randomNumberGenerator(): Random {
        return Random.Default
    }
}
