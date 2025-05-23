package yandex.praktikum.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yandex.praktikum.kafka.dto.ClientInfo;
import yandex.praktikum.kafka.dto.ShopInfo;
import yandex.praktikum.kafka.producer.Producer;
import yandex.praktikum.kafka.service.RecommendationService;
import yandex.praktikum.kafka.service.ShopService;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Tag(name = "Данные о товарах")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/product")
public class ShopController {

    private final Producer producer;
    private final ShopService shopService;
    private final RecommendationService recommendationService;

    @Operation(summary = "Получить данные о товаре по наименованию")
    @GetMapping(path = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о товаре получена"),
            @ApiResponse(responseCode = "404", description = "Товар не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка микросервиса")
    })
    public ResponseEntity<ShopInfo> getInfo(@RequestParam String userName,
                            @RequestParam String productName) throws IOException {
        var clientInfo = ClientInfo.builder()
                .userName(userName)
                .productName(productName)
                .build();
        log.info("Получен запрос на получение информации о товаре {}", clientInfo);
        producer.sendClientInfoToTopic(clientInfo);
        Optional<ShopInfo> shopInfo = shopService.getShopInfo(productName);
        return shopInfo.map(info -> new ResponseEntity<>(info, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Получить рекомендацию о товаре")
    @GetMapping(path = "/recommendation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о товаре получена"),
            @ApiResponse(responseCode = "404", description = "Рекомендация для указанного пользователя не найдена"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка микросервиса")
    })
    public ResponseEntity<ShopInfo> getRecommendation(@RequestParam String userName) throws IOException {
        log.info("Запрос рекомендации для пользователя {}", userName);
        Optional<ShopInfo> shopInfo = recommendationService.geRecommendation(userName);
        return shopInfo.map(info -> new ResponseEntity<>(info, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Добавить запрещённый товар в список")
    @PostMapping(path = "/deprecate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Товар успешно добавлен в список запрещённых"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка микросервиса")
    })
    public void deprecate(@RequestParam String deprecatedProductName) {
        log.info("Запрос на добавление товара {} в список запрещённых", deprecatedProductName);
        producer.sendDeprecatedProduct(deprecatedProductName);
    }
}
