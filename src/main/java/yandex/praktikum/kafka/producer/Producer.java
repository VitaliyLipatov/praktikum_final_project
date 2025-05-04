package yandex.praktikum.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yandex.praktikum.kafka.config.KafkaProperties;
import yandex.praktikum.kafka.dto.*;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import yandex.praktikum.kafka.streams.KafkaStreamsMessageFilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class Producer {

    private static final List<String> PRODUCT_NAMES = List.of("Умные часы XYZ", "Смартфон Samsung", "Набор отвёрток",
            "Бензопила", "Айфон", "Кувалда");
    private static final List<String> CATEGORIES = List.of("Электроника", "Инструменты");
    private static final List<String> TAGS = List.of("умные часы", "гаджеты", "технологии");

    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    public static final String JAAS_TEMPLATE =
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";

    private final KafkaProperties kafkaProperties;
    private final KafkaStreamsMessageFilter filter;

    @SneakyThrows
    @Scheduled(fixedDelayString = "20", timeUnit = TimeUnit.SECONDS)
    public void sendShopInfoToTopic() {
        KafkaProducer<Long, ShopInfo> shopInfoProducer = getShopInfoProducer();

        // Отправка сообщений в топик
        int randomNumber = getRandomNumber(10);
        for (long i = 0; i < randomNumber; i++) {
            var shopInfo = buildShopInfo(i);
            ProducerRecord<Long, ShopInfo> record = new ProducerRecord<>(kafkaProperties.getTopicProducts(), i,
                    shopInfo);
            shopInfoProducer.send(record);
            log.info("Сообщение {} успешно отправлено в топик {}", record.value(), kafkaProperties.getTopicProducts());
        }
        shopInfoProducer.close();
        filter.filterProducts();
    }

    public void sendDeprecatedProduct(String deprecatedProduct) {
        KafkaProducer<String, String> stringProducer = getStringProducer();
        ProducerRecord<String, String> record = new ProducerRecord<>(kafkaProperties.getTopicDeprecatedProducts(),
                deprecatedProduct, deprecatedProduct);
        stringProducer.send(record);
        log.info("Запрещённый товар {} успешно отправлен в топик {}", record.value(),
                kafkaProperties.getTopicDeprecatedProducts());
        stringProducer.close();
    }

    public void sendClientInfoToTopic(ClientInfo clientInfo) {
        KafkaProducer<String, ClientInfo> clientInfoProducer = getClientInfoProducer();
        ProducerRecord<String, ClientInfo> clientInfoRecord = new ProducerRecord<>(
                kafkaProperties.getTopicClientsRequest(), clientInfo.getUserName(), clientInfo);
        clientInfoProducer.send(clientInfoRecord);
        log.info("Сообщение {} успешно отправлено в топик {}", clientInfoRecord.value(),
                kafkaProperties.getTopicClientsRequest());
        clientInfoProducer.close();
    }

    private KafkaProducer<Long, ShopInfo> getShopInfoProducer() {
        Properties properties = getCommonProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());
        properties.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        properties.put("auto.register.schemas", true);
        properties.put("use.latest.version", true);

        // Создание продюсера
        return new KafkaProducer<>(properties);
    }

    private KafkaProducer<String, String> getStringProducer() {
        Properties properties = getCommonProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Создание продюсера
        return new KafkaProducer<>(properties);
    }

    private KafkaProducer<String, ClientInfo> getClientInfoProducer() {
        Properties properties = getCommonProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());

        properties.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        properties.put("auto.register.schemas", true);
        properties.put("use.latest.version", true);

        return new KafkaProducer<>(properties);
    }

    private Properties getCommonProducerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        // Синхронная репликация - требует, чтобы все реплики синхронно подтвердили получение сообщения,
        // только после этого оно считается успешно отправленным
        properties.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getAcks());
        // Количество повторных попыток при отправке сообщений, если возникает ошибка.
        // Если три раза произошли ошибки, то сообщение считается неотправленным и ошибка будет возвращена продюсеру.
        properties.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getRetries());
        // Минимум 2 реплики должны подтвердить запись
        properties.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, kafkaProperties.getReplicas());

        properties.put(SASL_MECHANISM, "PLAIN");
        properties.put(SASL_JAAS_CONFIG, String.format(JAAS_TEMPLATE, "kafka", "bitnami"));
        properties.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, "password_0");
        properties.put(SSL_PROTOCOL_CONFIG, "TLS");
        properties.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        properties.put(SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
        properties.put(SSL_TRUSTSTORE_LOCATION_CONFIG,
                "C:\\Users\\vital\\IdeaProjects\\praktikum_final_project\\src\\main\\resources\\infra\\kafka-0-creds\\etc\\kafka\\secrets\\kafka.kafka-0.truststore.jks");

        return properties;
    }

    private int getRandomNumber(int max) {
        Random random = new Random();
        return random.nextInt(max - 1) + 1;
    }

    private String getRandomItemFromList(List<String> items) {
        Random rand = new Random();
        return items.get(rand.nextInt(items.size()));
    }

    private ShopInfo buildShopInfo(Long productId) {
        return ShopInfo.builder()
                .productId(productId)
                .name(getRandomItemFromList(PRODUCT_NAMES))
                .description("Характеристики и описание товара")
                .price(buildPrice())
                .category(getRandomItemFromList(CATEGORIES))
                .brand("XYZ")
                .stock(buildStock())
                .sku("XYZ-12345")
                .tags(TAGS)
                .images(buildImages())
                .specifications(buildSpecifications())
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .index("products")
                .storeId(String.valueOf(productId))
                .build();
    }

    private Price buildPrice() {
        Random random = new Random();
        return Price.builder()
                .amount(BigDecimal.valueOf(random.nextDouble()))
                .currency("RUB")
                .build();
    }

    private Stock buildStock() {
        Random random = new Random();
        return Stock.builder()
                .available(random.nextLong())
                .reserved(random.nextLong())
                .build();
    }

    private List<Images> buildImages() {
        return List.of(
                Images.builder()
                        .url("https://example.com/images/product1.jpg")
                        .alt("фото 1")
                        .build(),
                Images.builder()
                        .url("https://example.com/images/product2.jpg")
                        .alt("фото 2")
                        .build());
    }

    private Specifications buildSpecifications() {
        return Specifications.builder()
                .weight("500g")
                .dimensions("42mm x 36mm x 10mm")
                .build();
    }
}
