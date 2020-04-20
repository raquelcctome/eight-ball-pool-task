package com.github.raquelcctome.kafka.Consumers;

import com.github.raquelcctome.EnumGameTier;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;


/**
 * EventConsumerProducer will consume Kafka topic, make some transformation
 * in the data and send which event to the respective topic.
 */
public class EventConsumerProducer {

    Logger logger = LoggerFactory.getLogger(EventConsumerProducer.class.getName());

    public static void main(String[] args) {
        new EventConsumerProducer().run();
    }

    private void run() {
        KafkaConsumer<String, String> consumer = createConsumer("my-second-group");
        KafkaProducer<String, String> producer = createKafkaProducer();

        // subscribe consumer to our topics
        consumer.subscribe(Collections.singleton("event-topic"));
        // subscribe to more topics
        //consumer.subscribe(Arrays.asList("event-topic", "purchase-topic", "match-topic"));

        final int giveUp = 1000;
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

                jsonObject = uppercaseUser(jsonObject);
                jsonObject = getGameTier(jsonObject);

                sendToTopic(producer, jsonObject, (String) jsonObject.get("event-type"));
            }
        }
        logger.info("End of application!");
    }

    /**
     * Send jsonObject to Kafka topic
     * @param producer Kafka Producer
     * @param jsonObject JSONObject modified
     * @param topic Kafka topic
     */
    private void sendToTopic(KafkaProducer<String, String> producer, JSONObject jsonObject, String topic) {
        producer.send(new ProducerRecord<>(topic, null, jsonObject.toJSONString()), this::onCompletion);
        // flush data
        producer.flush();
    }

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
     * Modifies user id to uppercase.
     * @param jsonObject Json consumed
     * @return Json Object modified to be sent to topic
     */
    private JSONObject uppercaseUser(JSONObject jsonObject) {
        if (jsonObject.get("user-id") != null) {
            String user = (String) jsonObject.get("user-id");
            jsonObject.put("user-id", user.toUpperCase());
        }
        return jsonObject;
    }

    /**
     * Map given tier game code to respective Enum
     *
     * @param jsonObject Json consumed
     * @return JSON Object modified to be sent to topic
     */
    private JSONObject getGameTier(JSONObject jsonObject) {
        if (jsonObject.get("game-tier") != null) {
            int tierCode = ((Long) jsonObject.get("game-tier")).intValue();
            EnumGameTier gameTier = EnumGameTier.getById(tierCode);
            jsonObject.put("game-tier", gameTier.toString());
        }
        return jsonObject;
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
     * Create Kafka Producer with required properties.
     *
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
