package com.github.raquelcctome.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  This class should be the first to be executed.
 *  It will populate the RawDB with dummy events in the 'events' Collection.
 */
public class PopulateDB {

    Logger logger = LoggerFactory.getLogger(PopulateDB.class);

    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create()) {
            MongoDatabase myTrainingDB = mongoClient.getDatabase("RawDB");
            MongoCollection<Document> eventsCollection = myTrainingDB.getCollection("events");
            insertManyDocuments(eventsCollection);
        }
    }

    /**
     * This method will generate dummy events and insert in the given collection
     * @param eventsCollection
     */
    private static void insertManyDocuments(MongoCollection<Document> eventsCollection) {
        List<Document> events = new ArrayList<>();

        Document event = new Document("_id", new ObjectId());
        for (int i = 0; i < 5; i++) {
            events.add(generateInitEvent(event, i));
            events.add(generatePurchaseEvent(event, i));
            events.add(generateMatchEvent(event, i));
        }
        eventsCollection.insertMany(events);
    }

    private static Document generateMatchEvent(Document event, int i) {
        Map<String, Object> map = new HashMap<>();
        map.put("_id", new ObjectId());
        map.put("event-type", "match");
        map.put("time", 10d);
        map.put("user-a", "user" + i);
        map.put("user-b", "user" + (i + 1));

        Map<String, Object> userA = new HashMap<>();
        userA.put("coin-balance-after-match", 20);
        userA.put("level-after-match", i);
        userA.put("device", "device1");
        userA.put("platform", "platform1");

        Map<String, Object> userB = new HashMap<>();
        userB.put("coin-balance-after-match", 50);
        userB.put("level-after-match", i+1);
        userB.put("device", "device1");
        userB.put("platform", "platform1");

        map.put("user-a-postmatch-info", userA);
        map.put("user-b-postmatch-info", userB);

        map.put("winner", "user" + i);
        map.put("game-tier", i);
        map.put("duration", 0);

        event = new Document(map);
        return event;
    }

    private static Document generatePurchaseEvent(Document event, int i) {
        Map<String, Object> map = new HashMap<>();
        map.put("_id", new ObjectId());
        map.put("event-type", "in-app-purchase");
        map.put("time", 10d);
        map.put("purchase_value", 20.5);
        map.put("user-id", "user" + i);
        map.put("product-id", "product1");
        event = new Document(map);
        return event;
    }

    private static Document generateInitEvent(Document event, int i) {
        Map<String, Object> map = new HashMap<>();
        map.put("_id", new ObjectId());
        map.put("event-type", "init");
        map.put("time", 10d);
        map.put("user-id", "user" + i);
        map.put("country", "PT");
        map.put("platform", "platform1");
        event = new Document(map);
        return event;
    }
}
