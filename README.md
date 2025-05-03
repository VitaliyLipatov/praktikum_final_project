# praktikum_final_project
Финальный проект Яндекс Практикум

Инфраструктура проекта расположена в ресурсах в папке infra.
Для запуска контейнеров в докер необходимо выполнить команду docker-compose up -d.

Реализовано java-приложение на spring boot, запуск в классе KafkaApplication.

В классе Producer реализован процесс, который один раз в 20 секунд отправляет в топик products 
несколько объектов ShopInfo с описанием товара/продукта. Класс ShopInfo по структуре соответствует json-объекту
из задания.
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

http://localhost:3001/ grafana

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

