package yandex.praktikum.kafka.streams;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import yandex.praktikum.kafka.config.KafkaProperties;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.*;
import static org.apache.kafka.streams.StoreQueryParameters.fromNameAndType;
import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
import static yandex.praktikum.kafka.producer.Producer.JAAS_TEMPLATE;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class KafkaDeprecatedProductsHandler {

    private final KafkaProperties kafkaProperties;
    private final ReadOnlyKeyValueStore<String, String> readOnlyKeyValueStore;

    @Autowired
    public KafkaDeprecatedProductsHandler(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
        this.readOnlyKeyValueStore = setReadOnlyKeyValueStore();
    }

    private ReadOnlyKeyValueStore<String, String> setReadOnlyKeyValueStore() {
        try {
            // Конфигурация Kafka Streams
            Properties config = getProperties();

            // Создание топологии
            StreamsBuilder builder = new StreamsBuilder();
            // Создание потока из Kafka-топика
            KStream<String, String> stream = builder.stream(kafkaProperties.getTopicDeprecatedProducts(),
                    Consumed.with(Serdes.String(), Serdes.String()));
            // Преобразование потока в таблицу с помощью метода toTable()
            stream.toTable(Materialized.<String, String>as(Stores.inMemoryKeyValueStore("deprecated-products-store"))
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.String()));

            // Инициализация Kafka Streams
            KafkaStreams streams = new KafkaStreams(builder.build(), config);

            // Устанавливаем обработчик необработанных исключений, чтобы при ошибках поток останавливался корректно.
            streams.setUncaughtExceptionHandler(exception -> {
                log.error("Ошибка при работе deprecated-products-to-table", exception);
                return SHUTDOWN_CLIENT;
            });
            // Очищаем состояние Kafka Streams перед запуском
            streams.cleanUp();
            streams.start();
            log.info("Kafka Streams приложение запущено успешно.");


            while (streams.state() != KafkaStreams.State.RUNNING) {
                Thread.sleep(5000); // Ждём инициализации состояния
                log.info("Текущее состояние stream deprecated-words-to-table : {}", streams.state());
                if (streams.state() == KafkaStreams.State.ERROR || streams.state() == KafkaStreams.State.NOT_RUNNING) {
                    throw new RuntimeException("Stream is not started");
                }
            }

            // Запрашиваем хранилище с именем deprecated-word-store для извлечения данных.
            return streams.store(
                    fromNameAndType("deprecated-products-store", QueryableStoreTypes.keyValueStore())
            );
        } catch (Exception ex) {
            log.error("Ошибка при работе Kafka Streams приложения: ", ex);
            throw new RuntimeException(ex);
        }
    }

    @NotNull
    private Properties getProperties() {
        Properties config = new Properties();
        // Уникальный идентификатор приложения Kafka Streams.
        // Помогает координировать и отслеживать состояния приложения внутри Kafka.
        // Все экземпляры приложения с одинаковым ID считаются частью одной группы.
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "deprecated-products-to-table");
        // Список адресов Kafka-брокеров, к которым будет подключаться приложение.
        // Здесь указывается минимум один брокер для начальной связи с кластером Kafka.
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        config.put(SASL_MECHANISM, "PLAIN");
        config.put(SASL_JAAS_CONFIG, String.format(JAAS_TEMPLATE, "kafka", "bitnami"));
        config.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        config.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, "password_0");
        config.put(SSL_PROTOCOL_CONFIG, "TLS");
        config.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        config.put(SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
        config.put(SSL_TRUSTSTORE_LOCATION_CONFIG,
                "C:\\Users\\vital\\спринт_5\\kafka-0-creds\\etc\\kafka\\secrets\\kafka.kafka-0.truststore.jks");

        return config;
    }

    public KeyValueIterator<String, String> getDeprecatedProductsIterator() {
        return readOnlyKeyValueStore.all();
    }
}
