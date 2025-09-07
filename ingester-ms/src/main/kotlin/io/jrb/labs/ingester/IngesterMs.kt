package io.jrb.labs.ingester

import io.jrb.labs.ingester.datafill.IngesterDatafill
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(IngesterDatafill::class)
class IngesterMs

fun main(args: Array<String>) {
    runApplication<IngesterMs>(*args)
}
