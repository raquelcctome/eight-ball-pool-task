package com.github.raquelcctome.kafka.Producers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.mongodb.client.model.Filters.eq;

/**
 * EventProducer will read all data from events Collection in MongoDB
 * and will send to a Kafka topic.
 */
public class EventProducer {

    Logger logger = LoggerFactory.getLogger(EventProducer.class.getName());

    public static void main(String[] args) {
        new EventProducer().run();
    }

    private void run() {

        // Get Mongo Collection
        MongoCollection<Document> collection = getDocumentMongoCollection();

        // Create Kafka Producer
        KafkaProducer<String, String> producer = createKafkaProducer();

        // Select all from Collection of MongoDB
        List<Document> matches = collection.find().into(new ArrayList<>());

        // Send records to the created topic
        for (Document match : matches) {
            producer.send(new ProducerRecord<>("event-topic", null, match.toJson()), this::onCompletion);
            // flush data
            producer.flush();
        }
        logger.info("End of application!");
    }

    /**
     * This method will return some logs after send record to topic.
     * @param recordMetadata Record data
     * @param e Exception if record was successfully sent it will be null
     */
    private void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e == null) {
            // the record was successfully sent
            logger.info("Received new metadata. \n" +
                    "Topic: " + recordMetadata.topic() + "\n" +
                    "Partition: " + recordMetadata.partition() + "\n" +
                    "Offset: " + recordMetadata.offset() + "\n" +
                    "Timestamp: " + recordMetadata.timestamp());
        } else {
            logger.error("Error while producing", e);
        }
    }

    /**
     * Connect to Mongo Database and get Collection.
     * @return Event Collection from MongoDB
     */
    private MongoCollection<Document> getDocumentMongoCollection() {
        MongoClient mongoClient = MongoClients.create();
        MongoDatabase database = mongoClient.getDatabase("RawDB");
        return database.getCollection("events");
    }

    /**
     * Create Kafka Producer with required properties.
     * @return Kafka Producer
     */
    private KafkaProducer<String, String> createKafkaProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }
}
