package yandex.praktikum.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import yandex.praktikum.kafka.config.KafkaProperties;
import yandex.praktikum.kafka.dto.ClientInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationConsumer {

    private final KafkaProperties kafkaProperties;

    public List<ClientInfo> getRecommendations() throws JsonProcessingException {
        // Настройка консьюмера – адрес сервера, сериализаторы для ключа и значения
        Properties props = getProperties();

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);

        // Подписка на топик
        consumer.subscribe(Collections.singletonList(kafkaProperties.getTopicClientsRecommendations()));

        // Устанавливаем таймаут ожидания, если на момент вызова метода poll нет доступных записей
        ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(120_000));
        List<ClientInfo> clientInfos = new ArrayList<>();
        var objectMapper = new ObjectMapper();
        for (ConsumerRecord<Integer, String> record : records) {
           log.info("Получено значение из топика {}: key = {}, value = {}",
                   kafkaProperties.getTopicClientsRecommendations(), record.key(), record.value());
            consumer.commitAsync();
            ClientInfo clientInfo = objectMapper.readValue(record.value(), ClientInfo.class);
            clientInfos.add(clientInfo);
        }
        return clientInfos;
    }

    @NotNull
    private Properties getProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29193, localhost:29195, localhost:29197");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "recommendation-consumer-group");     // Наименование группы консьюмеров
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");        // Начало чтения с самого начала
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");          // Автоматический коммит смещений (для pull модели консьюмера отключен)
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "6000");           // Время ожидания активности от консьюмера
        return props;
    }
}
