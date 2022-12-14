package it.polito.wa2.payment_service

import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator

@SpringBootApplication
class PaymentServiceApplication{
    @Bean
    fun initializer(@Qualifier("connectionFactory") connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)
        val resource = ResourceDatabasePopulator(ClassPathResource("schema_data.sql"))
        initializer.setDatabasePopulator(resource)
        return initializer
    }
}

fun main(args: Array<String>) {
    runApplication<PaymentServiceApplication>(*args)
}
