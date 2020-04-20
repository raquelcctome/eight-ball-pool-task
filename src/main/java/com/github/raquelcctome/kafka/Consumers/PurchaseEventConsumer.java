package com.github.raquelcctome.kafka.Consumers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * PurchaseEventConsumer will consume purchase events and bulk them into MongoDB
 */
public class PurchaseEventConsumer {

    Logger logger = LoggerFactory.getLogger(PurchaseEventConsumer.class.getName());

    public static void main(String[] args) {
        new PurchaseEventConsumer().run();
    }

    private void run() {
        KafkaConsumer<String, String> consumer = createConsumer("my-second-group");

        MongoDatabase client = getMongoClient();
        MongoCollection<Document> purchaseCollection = client.getCollection("in-app-purchase");

        List<Document> purchaseEventList = new ArrayList<>();

        // subscribe consumer to our topics
        consumer.subscribe(Collections.singleton("in-app-purchase"));
        // subscribe to more topics
        //consumer.subscribe(Arrays.asList("init", "in-app-purchase", "match"));

        final int giveUp = 100;
        int noRecordsCount = 0;
        JSONObject jsonObject = null;

        // poll for the new data
        while (true) {

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            if (records.count() == 0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp) break;
                else continue;
            }

            for (ConsumerRecord<String, String> record : records) {
                logger.info("Key: {}, Value: {}", record.key(), record.value());
                logger.info("Partition: {}, Offset: {}", record.partition(), record.offset());

                JSONParser parser = new JSONParser();
                try {
                    jsonObject = (JSONObject) parser.parse(record.value());
                } catch (ParseException e) {
                    logger.error("Error parsing record", e);
                }
                purchaseEventList.add(Document.parse(jsonObject.toJSONString()));
            }
        }

        // Insert list of documents into MongoBD
        if (!purchaseEventList.isEmpty()) {
            purchaseCollection.insertMany(purchaseEventList);
        }

        logger.info("End of application!");
    }

    /**
     * Creates Kafka Consumer with required properties.
     *
     * @param group Consumer Group
     * @return Kafka Consumer
     */
    private KafkaConsumer<String, String> createConsumer(String group) {
        // create consumer config
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // earliest:want to read from very beginning of topic
        // latest: read only the new message onwards
        // none: returns an error, if there's no offsets being send

        // create consumer
        return new KafkaConsumer<>(properties);
    }

    /**
     * Create connection to Mongo Database
     * @return Mongo Database
     */
    private MongoDatabase getMongoClient() {
        MongoClient mongoClient = MongoClients.create();
        MongoDatabase database = mongoClient.getDatabase("BallPoolGame");
        return database;
    }
}
