# praktikum_final_project
Финальный проект Яндекс Практикум

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

