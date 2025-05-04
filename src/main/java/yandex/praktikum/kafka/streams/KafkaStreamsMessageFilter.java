package yandex.praktikum.kafka.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import yandex.praktikum.kafka.config.KafkaProperties;
import yandex.praktikum.kafka.dto.ShopInfo;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.*;
import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
import static yandex.praktikum.kafka.producer.Producer.JAAS_TEMPLATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStreamsMessageFilter {

    private final KafkaProperties kafkaProperties;
    private final KafkaDeprecatedProductsHandler kafkaDeprecatedProductsHandler;

    public void filterProducts() {
        try {
            // Конфигурация Kafka Streams
            Properties properties = getProperties();

            // Создание StreamsBuilder
            StreamsBuilder builder = new StreamsBuilder();

            KStream<Long, String> productsStream = builder.stream(kafkaProperties.getTopicProducts(),
                    Consumed.with(Serdes.Long(), Serdes.String()));

            // Отправка отфильтрованных данных в другой топик
            productsStream.filter((key, productMessage) -> filterProducts(productMessage))
                    .to(kafkaProperties.getTopicFilteredProducts());

            // Инициализация Kafka Streams
            KafkaStreams streams = new KafkaStreams(builder.build(), properties);

            // Устанавливаем обработчик необработанных исключений, чтобы при ошибках поток останавливался корректно.
            streams.setUncaughtExceptionHandler(exception -> {
                log.error("Ошибка при работе praktikum-streams-app", exception);
                return SHUTDOWN_CLIENT;
            });

            // Запуск приложения
            streams.start();
            log.info("Kafka Streams приложение запущено успешно!");
        } catch (Exception ex) {
            log.error("Ошибка при запуске Kafka Streams приложения: ", ex);
        }
    }

    @NotNull
    private Properties getProperties() {
        Properties config = new Properties();
        // Уникальный идентификатор приложения Kafka Streams.
        // Помогает координировать и отслеживать состояния приложения внутри Kafka.
        // Все экземпляры приложения с одинаковым ID считаются частью одной группы.
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "deprecated-products-streams-filter");
        // Список адресов Kafka-брокеров, к которым будет подключаться приложение.
        // Здесь указывается минимум один брокер для начальной связи с кластером Kafka.
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        config.put(SASL_MECHANISM, "PLAIN");
        config.put(SASL_JAAS_CONFIG, String.format(JAAS_TEMPLATE, "kafka", "bitnami"));
        config.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        config.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, "password_0");
        config.put(SSL_PROTOCOL_CONFIG, "TLS");
        config.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        config.put(SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
        config.put(SSL_TRUSTSTORE_LOCATION_CONFIG,
                "C:\\Users\\vital\\IdeaProjects\\praktikum_final_project\\src\\main\\resources\\infra\\kafka-0-creds\\etc\\kafka\\secrets\\kafka.kafka-0.truststore.jks");

        return config;
    }

    @SneakyThrows
    private boolean filterProducts(String productMessage) {
        KeyValueIterator<String, String> keyValueIterator = kafkaDeprecatedProductsHandler.getDeprecatedProductsIterator();
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        while (keyValueIterator.hasNext()) {
            String currDeprProduct = keyValueIterator.next().value;
            ShopInfo shopInfo = objectMapper.readValue(productMessage.trim(), ShopInfo.class);
            if (StringUtils.containsIgnoreCase(shopInfo.getName(), currDeprProduct)) {
                return false;
            }
        }
        return true;
    }
}
