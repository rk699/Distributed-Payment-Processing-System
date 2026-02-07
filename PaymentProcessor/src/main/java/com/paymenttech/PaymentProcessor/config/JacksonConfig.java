package com.paymenttech.PaymentProcessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

//What:
//This is a custom Jackson ObjectMapper configuration used by Spring Boot for JSON serialization and deserialization.
//
//Jackson = JSON engine
//ObjectMapper = heart of Jackson

//Why:
//Java 8 introduced new date/time APIs:
//LocalDate LocalDateTime
//ZonedDateTime
//Instant
//
//Jackson does NOT support these properly by default.
//
//If you don’t configure this:
//Your app may fail at runtime
//Kafka messages may not serialize
//Redis / REST responses may break


//Jackson core does not know how to serialize Java 8 time classes.
//Jackson does not support Java 8 date/time classes by default.
// Registering JavaTimeModule enables proper serialization and deserialization of LocalDateTime,
// LocalDate, etc. Disabling timestamp serialization ensures readable, schema-friendly ISO-8601 formats,
// which is critical for Kafka messages and distributed systems.