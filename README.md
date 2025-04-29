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