# praktikum_final_project
Финальный проект Яндекс Практикум

Инфраструктура проекта расположена в ресурсах в папке infra.
Для запуска контейнеров в докер необходимо выполнить команду docker-compose up -d.
Далее необходимо какое-то время подождать и затем проверить, что контейнеры стартанули и работают.

Реализовано java-приложение на spring boot, запуск в классе KafkaApplication.

В классе Producer реализован процесс, который один раз в 20 секунд отправляет в топик products 
несколько объектов ShopInfo с описанием товара/продукта. Класс ShopInfo по структуре соответствует json-объекту
из задания. Перечень продуктов закреплён в константе PRODUCT_NAMES, откуда наименования выбираются случайным образом.
Это следует учитывать при тестировании API, например, при попытке найти описание товара (он должен быть из списка, 
чтобы что-то действительно вернулось) или при проверке фильтрации товаров (в список запрещённых можно добавить один
или несколько товаров из списка и через просмотр содержимого топика в kafka ui убедиться, что сообщения с запрещённым 
товаром не поступают).
Сервис предоставляет api для получения данных о товарах, реализованы методы:

/info - получить данные о товаре по наименованию. На вход метод принимает имя пользователя userName 
        и наименование искомого продукта productName. На выход отдаётся объект ShopInfo, если товар найден, 
        либо ошибка 404, если не найден;

/deprecate - добавить запрещённый товар в список. На вход метод принимает наименование запрещённого товара 
        deprecatedProductName, запрещённые товары попадают в отдельный топик deprecated-products. 
        Далее в процессе потоковой обработки сообщений из топика products товары, с указанным
        наименованием будут исключены, т.е. не попадут в топик filtered-products.

/recommendation - получить рекомендацию о товаре. На вход метод принимает имя пользователя userName. На выход метод 
                  отдаёт объект ShopInfo, содержащий информацию о товаре, рекомендованном к покупке на основе 
                  предыдущих поисковых запросов данного пользователя.

Swagger открывается в браузере по адресу http://localhost:8888.

Для потоковой фильтрации/блокировки запрещённых товаров реализован класс KafkaStreamsMessageFilter.
Отфильтрованные товары из топика filtered-products с помощью FileStreamSinkConnector попадают в файл products.out.
Для конфигурации коннектора использовал следующий запрос:
curl -X PUT \
-H "Content-Type: application/json" \
--data '{
"name": "file-stream-sink",
"connector.class": "org.apache.kafka.connect.file.FileStreamSinkConnector",
"tasks.max": "1",
"topics": "filtered-products",
"file": "products.out",
"value.converter": "org.apache.kafka.connect.storage.StringConverter"
}' \
http://localhost:8083/connectors/file-stream-sink/config

При поиске товара с помощью метода /info - анализируется содержимое файла products.out и при нахождении совпадения
возвращается объект ShopInfo.
Также сам поисковый запрос в виде объекта ClientInfo с помощью FileStreamSinkConnector из топика clients-request 
попадает в файл clients_request.out. 
Конфигурация коннектора:
curl -X PUT \
-H "Content-Type: application/json" \
--data '{
"name": "file-stream-sink",
"connector.class": "org.apache.kafka.connect.file.FileStreamSinkConnector",
"tasks.max": "1",
"topics": "clients-request",
"file": "clients_request.out",
"value.converter": "org.apache.kafka.connect.storage.StringConverter"
}' \
http://localhost:8083/connectors/file-stream-sink/config

Для кластера kafka реализован сбор метрик с помощью prometheus, собранные метрики визуализируется на дашбордах в 
grafana. Grafana открывается по ссылке http://localhost:3001/, логин admin, пароль kafka. В grafana настроен Alerting
на падение одного из брокеров. Если все 3 брокера запущены, то состояние правила normal (скриншот normal_grafana.png),
после остановки одного брокера состояние правила изменилось на firing (скриншот firing_grafana.png).

С помощью mirror-maker настроено дублирование данных всех топиков на второй кластер.

Для первого кластера реализована безопасная передача данных через TLS, выданы права для работы с топиками (ACL):

[appuser@kafka-0 ~]$ kafka-acls --authorizer-properties zookeeper.connect=zookeeper-source:2181 --add --allow-principal User:kafka --operation Write --topic products
Warning: support for ACL configuration directly through the authorizer is deprecated and will be removed in a future release. Please use --bootstrap-server instead to set ACLs through the admin client.
Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=products, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=products, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

kafka-acls --authorizer-properties zookeeper.connect=zookeeper-source:2181 --add --allow-principal User:kafka --operation Write --topic deprecated-products
Warning: support for ACL configuration directly through the authorizer is deprecated and will be removed in a future release. Please use --bootstrap-server instead to set ACLs through the admin client.
Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=deprecated-products, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=deprecated-products, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

kafka-acls --authorizer-properties zookeeper.connect=zookeeper-source:2181 --add --allow-principal User:kafka --operation Write --topic filtered-products
Warning: support for ACL configuration directly through the authorizer is deprecated and will be removed in a future release. Please use --bootstrap-server instead to set ACLs through the admin client.
Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=filtered-products, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=filtered-products, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

kafka-acls --authorizer-properties zookeeper.connect=zookeeper-source:2181 --add --allow-principal User:kafka --operation Write --topic clients-request
Warning: support for ACL configuration directly through the authorizer is deprecated and will be removed in a future release. Please use --bootstrap-server instead to set ACLs through the admin client.
Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=clients-request, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=clients-request, patternType=LITERAL)`:
(principal=User:kafka, host=*, operation=WRITE, permissionType=ALLOW)

Kafka UI доступен по ссылке http://localhost:8080/ - можно просмотреть брокеров, топики, их содержимое и т.д.

Для анализа поисковых запросов и вычисления рекомендаций реализован класс RecommendationService.
Метод calculateRecommendation() запускается каждые 30 секунд, анализирует содержимое файла clients_request.out и 
формирует для каждого пользователя одну рекомендацию, которую в виде объекта ClientInfo отправляет в топик
clients-recommendation, ключом при этом является кол-во запросов на поиск данного товара со стороны соответствующего 
клиента.
При вызове метода /recommendation запускается RecommendationProducer, которые получает записи из топика 
clients-recommendation и выбирает из них нужную исходя из имени клиента, от которого поступил запрос на рекомендацию.
Если товар, который чаще всего искал пользователь, представлен в магазине, то возвращается объект ShopInfo с описанием
данного товара, иначе ошибка 404, т.к. рекомендацию сформировать невозможно.

Другой способ получить рекомендацию - с помощью запросов ksql.
Для этого в терминале потребуется выполнить:
docker exec -it final_project-ksqldb-cli-1 ksql http://ksqldb-server:8088

Далее создать необходимый стрим:
CREATE OR REPLACE STREAM recommendation_stream (
id INTEGER KEY,
user_name STRING
) WITH (
KAFKA_TOPIC='clients-recommendation',
KEY_FORMAT='KAFKA',
VALUE_FORMAT='KAFKA',
PARTITIONS=1
);

И выполнить запрос:
SELECT * FROM recommendation_stream;

Результат выполнения запроса приведён на скриншоте ksql_query_result.png.
На текущий момент в приложении сформированы следующие рекомендации:
пользователь danila -> смартфон
пользователь artem -> умные часы
пользователь vitaliy -> айфон

Для корректной работы приложения необходимо создать топики, это можно сделать через консоль:
bin/kafka-topics.sh --create --topic deprecated-products --bootstrap-server localhost:9093, localhost:9095, localhost:9097 --partitions 3 --replication-factor 2;

Все топики, используемые в приложении, указаны в application.yml с префиксом topic.
Необходимые топики можно также создать через kafka ui, либо топики будут созданы автоматически при первом использовании,
кроме топика deprecated-products, его нужно создать до запуска приложения для корректной работы kafka streams.

После запуска KafkaApplication нужно подождать пару минут, убедиться в отсутствии критичных ошибок в логах.