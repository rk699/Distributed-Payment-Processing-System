package com.paymenttech.PaymentProcessor.config;


import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.paymenttech.PaymentProcessor.dto.PaymentEvent;

//@EnableKafka activates Spring’s Kafka listener infrastructure, which is NOT enabled by default in plain Spring.
@Configuration
@EnableKafka
public class KafkaConfig {

//    Spring starts
//    @EnableKafka registers listener processor
//    Spring scans beans
//    Finds @KafkaListener
//    Creates KafkaMessageListenerContainer
//    Consumer polls Kafka continuously

//    Spring Boot auto-enables Kafka via:
//    THEN WHY USE IT AT ALL?
//    Production-grade answer
    //    You explicitly use @EnableKafka when:
    //    You want clarity
    //    You’re not relying on auto-config
    //    You’re writing library or framework code
    //    You want predictable behavior
//@EnableKafka enables Spring’s Kafka listener infrastructure by
// registering the internal components required to process @KafkaListener annotations.
// Without it, Kafka consumers are never started. In Spring Boot it’s often enabled automatically,
// but explicitly using it improves clarity and portability.

//    KafkaTemplate → works without @EnableKafka
//    @KafkaListener → needs @EnableKafka

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.partitions:10}")
    private int partitions;

    @Value("${kafka.replication-factor:1}")
    private short replicationFactor;

//    FLow
//    App starts
//    ProducerFactory bean created
//    KafkaTemplate uses it
//    Producer created lazily
//    Messages batched
//    Compressed
//    Sent to leader
//    Replicated
//    ACK returned

    @Bean
    public ProducerFactory<String, PaymentEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
//        WHY
//        Producer doesn’t know the whole cluster initially
//        It asks one broker for metadata
//        Then learns about leaders for partitions
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); //Initial broker list used to discover the Kafka cluster.
//
//        WHY keys matter (deep Kafka truth)
//        Keys decide partition
//        Partition decides ordering//
//        Ordering decides correctness
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);//Defines how message keys are converted to bytes.
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);//Converts PaymentEvent → JSON → byte[]

//        0	Fire-and-forget	💀 data loss
//        1	Leader only	⚠️ leader crash
//        all	Leader + ISR	✅ safest
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); //Controls durability guarantee

//        Retries + acks=all + no idempotence = duplicate messages
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);//Retries sending message on retryable errors

//        Why snappy:
//        Fast
//        Low CPU
//        Balanced compression ratio
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");//Compresses message batches before sending.
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    //    Runtime flow
//    Spring creates KafkaTemplate
//    Template requests a producer from ProducerFactory
//    Producer is cached & reused
//    Messages are sent asynchronously
//    Kafka handles batching + retries
    @Bean
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate() {

        return new KafkaTemplate<>(producerFactory());
    }

//    ConsumerFactory is Spring Kafka’s consumer creator and lifecycle manager.
//    Creates KafkaConsumer instances
//    Injected into KafkaListenerContainerFactory
//    Used by @KafkaListener

//    Note: Kafka remembers only what you commit, not what you process.

    @Bean
    public ConsumerFactory<String, PaymentEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);//Consumer later talks directly to partition leaders

//        Topic: payment-events (6 partitions)
//        Group: payment-processor-group
//        Consumers: 3
//        Each consumer gets 2 partitions
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-processor-group");//Consumer Group decides: Parallelism,Scalability,Delivery semantics

//        Converts bytes → String
//        Must match producer’s key serializer
//        Mismatch = runtime crash.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

//        Converts JSON → Java object
//        Used with PaymentEvent
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());//Kafka messages do not carry Java class info. so we need to define were to be mapped

//        AUTO_OFFSET_RESET_CONFIG//
//        Used only when no offset exists//
//        earliest → read from beginning (safe)//
//        latest → read new messages only
//        none → fail
//        Prefer earliest for business data
//        Avoid latest for critical systems
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

//        ENABLE_AUTO_COMMIT_CONFIG//
//        Kafka commits offsets automatically//
//        Commit happens before processing finishes//
//        Crash after commit → data loss
//
//        Auto-commit = at-most-once
//        Manual commit = at-least-once ie., false
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);//batch size per poll
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800);//Max data fetched per request

//        Thread-safe
//        Integrates with Spring listener containers
//        Handles deserializer lifecycle
        return new DefaultKafkaConsumerFactory<>(props);//
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, PaymentEvent>>  kafkaListenerContainerFactory() {
//        Creates Kafka consumer containers
//        Required for @KafkaListener
//        Controls threads, concurrency, commits, error handling
//
//        ConcurrentKafkaListenerContainerFactory – WHY
//        Enables parallel consumption
//        Creates multiple KafkaConsumer instances
//        Used for high-throughput topics
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(10);
        return factory;
    }


//    NewTopic lets Spring create Kafka topics programmatically, where partitions control scalability and replicas ensure fault tolerance.

//    Topic = Table
//    Partition = Shard
//    Replica = Backup

    @Bean
    public NewTopic paymentEventTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic paymentRetryTopic() {
        return TopicBuilder.name("payment-retry")
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic paymentDlqTopic() {
        return TopicBuilder.name("payment-dlq")
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}