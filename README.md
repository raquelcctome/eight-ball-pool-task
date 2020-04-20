# eight-ball-pool-task
This project aims to build a pipeline to process 8BallPool received events. This pipeline has four distinct components:

 1. PopulateDB - This is where is going to be created the dummy events and inserted into a new Collection _events_ of MongoDB. This Collection is going to be consumed by Kafka in further processing.
 
 2. EventProducer - Here, raw events are going to be extracted from _events_ Collection of RawDB database and then produced to a Kafka topic,
 _event-topic_ , without any modifications.
 
 3. EventConsumerProducer - This component will consume and produce Kafka topics. It will consume raw events from _event-topic_ topic, and
 then make some transformations:
 
	*Uppercase user-id value from Init and InAppPurchase events
	
	*Map game-tier id, from Match event, to respective Enum
  
 After this transformations a Kafka Producer will send each event to one respective topic of the same name as the event-type.

 4.  EventConsumer for each Event - Each consumer will collect offsets from the respective event topic, and then bulk insert them into a new Collection of MongoDB database BallPoolGame. There will be three new Collections, where the transformed events will be stored by event type.
 
Steps through the project
-------------------------
To see this project working we need to initialize zookeper and kafka:

*zookeeper-server-start.bat config/zookeeper.properties 

*kafka-server-start.bat config/server.properties

As well as MongoDB.

Then, execute the following commands to create four Kafka topics, each one with three partitions (If we don't create them now, they will be created automatically with one partition).

* kafka-topics.bat --zookeeper localhost:2181 --topic event-topic --create --partitions 3 --replication-factor 1

* kafka-topics.bat --zookeeper localhost:2181 --topic init --create --partitions 3 --replication-factor 1

* kafka-topics.bat --zookeeper localhost:2181 --topic in-app-purchase --create --partitions 3 --replication-factor 1

* kafka-topics.bat --zookeeper localhost:2181 --topic match --create --partitions 3 --replication-factor 1


After this, we should first run com.github.raquelcctome.mongodb.PopulateDB.main() to populate RawDB with dummy events.
Check RawDB:

 1. _use RawDB_
 2. _db.events.find()_

We will see documents with this format:

{ "__id" : ObjectId("5e9cf964771b3f6dc6167f49"), "country" : "PT", "event-type" : "init", "user-id" : "user0", "time" : 10, "platform" : "platform1" }


Then, to start all the consumers run com.github.raquelcctome.kafka.Consumers.*.main()

And finally, start to produce to topics. Run com.github.raquelcctome.kafka.Producers.EventProducer.main():
 First it will be the _event-topic_ to be populated with the raw data. To see the offsets through the comand line:
	
	* kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic event-topic
	
It should appear entries like this:

{"__id": {"$oid": "5e9cf964771b3f6dc6167f4f"}, "country": "PT", "event-type": "init", "user-id": "user0", "time": 10.0, "platform": "platform1"}
 
 
 Then, the topics _init_, _match_, _in-app-purchase_ will be populated, with the transformed data. To see the offsets in the comand line:
 
	* kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic init
	
	* kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic match
	
	* kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic in-app-purchase
	
	
The entries shown should look like this:

{"country":"PT","event-type":"init","user-id":"USER0","__id":{"$oid":"5e9cf9c45e2e5d51a5143a73"},"time":10.0,"platform":"platform1"},
**in case of an Init event**.

{"duration":0,"winner":"user4","user-a":"user4","event-type":"match","game-tier":"GOLD","__id":{"$oid":"5e9cf9c45e2e5d51a5143a75"},"time":10.0,"user-b":"user5","user-b-postmatch-info":{"level-after-match":5,"coin-balance-after-match":50,"device":"device1","platform":"platform1"},"user-a-postmatch-info":{"level-after-match":4,"coin-balance-after-match":20,"device":"device1","platform":"platform1"}}, **in case of a Match event**.


Consumers will print their records value as well.

note: If there is nothing to consume for a period of time, they will shutdown.

We can now check our collection in the BallPoolGame database, with the transformed data.
	
	1. _use BallPoolGame_
	
	2. _show collections_
	
	3. _db.init.find()_


We will see data like this:

	{ "__id" : ObjectId("5e9cf964771b3f6dc6167f49"), "country" : "PT", "event-type" : "init", "user-id" : "USER0", "time" : 10, "platform" : "platform1" }

