package yandex.praktikum.kafka.spark;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Component
public class KafkaHdfsSparkConsumer {

    public void process() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "hadoop-spark-consumer-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("hadoop-topic"));

        String hdfsUri = "hdfs://localhost:9000";
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        try (
                FileSystem hdfs = FileSystem.get(new URI(hdfsUri), conf, "root")) {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    String value = record.value();
                    System.out.println("Получено сообщение: " + value);

                    String hdfsFilePath = "/data/message_" + UUID.randomUUID();
                    Path path = new Path(hdfsFilePath);

                    try (FSDataOutputStream outputStream = hdfs.create(path, true)) {
                        outputStream.writeUTF(value);
                    }
                    System.out.println("Сообщение записано в HDFS по пути: " + hdfsFilePath);
                    processWithSpark(hdfsUri);
                }
            }

        } catch (
                Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }

    private static void processWithSpark(String hdfsUri) {
        SparkConf sparkConf = new SparkConf().setAppName("KafkaHdfsSparkConsumer").setMaster("local[*]");
        try (JavaSparkContext sc = new JavaSparkContext(sparkConf)) {
            JavaRDD<String> hdfsData = sc.textFile(hdfsUri + "/data/*");
            long count = hdfsData.count();
            System.out.println("Общее количество записей в HDFS: " + count);
            List<String> allData = new ArrayList<>(hdfsData.collect());
            Collections.reverse(allData);
            allData.stream()
                    .forEach(line -> System.out.println("Spark обработал строку: " + line));

        }
    }
}

