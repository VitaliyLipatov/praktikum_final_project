package yandex.praktikum.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import yandex.praktikum.kafka.dto.ShopInfo;

import javax.management.openmbean.OpenMBeanAttributeInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@Service
public class ShopService {

    public Optional<ShopInfo> getShopInfo(String productName) throws IOException {
        try (InputStream inputStream = ShopService.class.getResourceAsStream("products.out")) {
            String product = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] shopInfos = product.split("\\r?\\n");
            for (String s : shopInfos) {
                if (StringUtils.containsIgnoreCase(s, productName)) {
                    var objectMapper = new ObjectMapper()
                            .registerModule(new ParameterNamesModule())
                            .registerModule(new Jdk8Module())
                            .registerModule(new JavaTimeModule());
                    return Optional.ofNullable(objectMapper.readValue(s, ShopInfo.class));
                }
            }
        }
        return Optional.empty();
    }
}
