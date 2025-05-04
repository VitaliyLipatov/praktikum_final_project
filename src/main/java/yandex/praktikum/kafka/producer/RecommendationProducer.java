package yandex.praktikum.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import yandex.praktikum.kafka.config.KafkaProperties;
import yandex.praktikum.kafka.dto.ClientInfo;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationProducer {

    private final KafkaProperties kafkaProperties;

    public void sendRecommendationTopic(Map<ClientInfo, Integer> recommendationMap) {
        try {
            // Конфигурация продюсера – адрес сервера, сериализаторы для ключа и значения.
            KafkaProducer<Integer, String> producer = getRecommendationProducer();
            var objectMapper = new ObjectMapper();

            // Отправка рекомендаций в отдельный топик
            for (Map.Entry<ClientInfo, Integer> entry : recommendationMap.entrySet()) {
                Integer key = entry.getValue();
                ClientInfo value = entry.getKey();
                String valueAsString = objectMapper.writeValueAsString(value);
                ProducerRecord<Integer, String> record = new ProducerRecord<>(
                        kafkaProperties.getTopicClientsRecommendations(), key, valueAsString);
                producer.send(record);
                log.info("Рекомендация {} успешно отправлена в топик {} ", record,
                        kafkaProperties.getTopicClientsRecommendations());
            }

            // Закрытие продюсера
            producer.close();
        } catch (Exception ex) {
            log.error("Ошибка при отправки рекомендации в топик ", ex);
        }
    }

    @NotNull
    private KafkaProducer<Integer, String> getRecommendationProducer() {
        Properties properties = new Properties();
        // брокеры второго кластера kafka
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29193, localhost:29195, localhost:29197");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Синхронная репликация - требует, чтобы все реплики синхронно подтвердили получение сообщения,
        // только после этого оно считается успешно отправленным
        properties.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getAcks());
        // Количество повторных попыток при отправке сообщений, если возникает ошибка.
        // Если три раза произошли ошибки, то сообщение считается неотправленным и ошибка будет возвращена продюсеру.
        properties.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getRetries());
        // Минимум 2 реплики должны подтвердить запись
        properties.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, kafkaProperties.getReplicas());

        // Создание продюсера
        return new KafkaProducer<>(properties);
    }
}
