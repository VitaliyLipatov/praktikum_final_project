package yandex.praktikum.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import yandex.praktikum.kafka.consumer.RecommendationConsumer;
import yandex.praktikum.kafka.dto.ClientInfo;
import yandex.praktikum.kafka.dto.ShopInfo;
import yandex.praktikum.kafka.producer.RecommendationProducer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationProducer recommendationProducer;
    private final RecommendationConsumer recommendationConsumer;
    private final ShopService shopService;

    public Optional<ShopInfo> geRecommendation(String userName) throws IOException {
        List<ClientInfo> recommendations = recommendationConsumer.getRecommendations();
        for (ClientInfo recommendation : recommendations) {
            if (recommendation.getUserName().equalsIgnoreCase(userName)) {
                return shopService.getShopInfo(recommendation.getProductName());
            }
        }
        return Optional.empty();
    }

    @Scheduled(fixedDelayString = "30", timeUnit = TimeUnit.SECONDS)
    public void calculateRecommendation() throws IOException {
        try (InputStream inputStream = ShopService.class.getResourceAsStream("clients_request.out")) {
            String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] clientInfoStrings = string.split("\\r?\\n");
            var objectMapper = new ObjectMapper();
            List<ClientInfo> clientInfoList = new ArrayList<>();
            for (String c : clientInfoStrings) {
                ClientInfo info = objectMapper.readValue(c, ClientInfo.class);
                clientInfoList.add(info);
            }
            Map<String, List<ClientInfo>> userVsClientIfo = clientInfoList.stream()
                    .collect(Collectors.groupingBy(ClientInfo::getUserName));
            Map<ClientInfo, Integer> recommendationMap = new HashMap<>();
            for (Map.Entry<String, List<ClientInfo>> e : userVsClientIfo.entrySet()) {
                List<ClientInfo> clientInfos = e.getValue();
                Map<String, List<ClientInfo>> productNameVsSameUser = clientInfos.stream()
                        .collect(Collectors.groupingBy(ClientInfo::getProductName));
                int requestCount = 0;
                for (Map.Entry<String, List<ClientInfo>> entry : productNameVsSameUser.entrySet()) {
                    int size = entry.getValue().size();
                    if (size > requestCount) {
                        ClientInfo currClientInfo = entry.getValue().get(0);
                        requestCount = size;
                        recommendationMap.remove(currClientInfo);
                        recommendationMap.put(currClientInfo, size);
                    }
                }
            }
            recommendationProducer.sendRecommendationTopic(recommendationMap);
        }
    }
}

