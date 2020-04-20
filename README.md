# eight-ball-pool-task
This project aims to build a pipeline to process 8BallPool received events. This pipeline has four distinct components:

 1. PopulateDB - This is where is going to be created the dummy events and inserted into a new Collection _events_ of MongoDB. This Collection is going to be consumed by Kafka in further processing.
 
 2. EventProducer - Here, raw events are going to be extracted from _events_ Collection of RawDB databse and then produced to a Kafka topic,
 _event-topic_ , without any modifications.
 
 3. EventConsumerProducer - This component will consume and produce Kafka topics. It will consume raw events from _event-topic_ topic, and
 then make some transformations:
 
	*Uppercase user-id value from Init and InAppPurchase events
	*Map game-tier id, from Match event, to respective Enum
  
 After this transformations a Kafka Producer will send each event to one respective topic of the same name as the event-type.

 4.  EventConsumer for each Event - Each consumer will collect offsets from the respective event topic, and then bulk them into a new Collection of MongoDB database BallPoolGame. Their will be three new Collections, where the transformed events will be stored by event type.
 
Steps through the project
-------------------------
To see this project working we need to initialize zookeper and kafka:

*zookeeper-server-start.bat config/zookeeper.properties 

*kafka-server-start.bat config/server.properties

As well as MongoDB.

Then, we can create our topics so they can have as many partitions we want. If we let them be create by default, they will be created with one partition.

*kafka-topics.bat --zookeeper localhost:2181 --topic event-topic --create --partitions 3 --replication-factor 1

*kafka-topics.bat --zookeeper localhost:2181 --topic init --create --partitions 3 --replication-factor 1

*kafka-topics.bat --zookeeper localhost:2181 --topic in-app-purchase --create --partitions 3 --replication-factor 1

*kafka-topics.bat --zookeeper localhost:2181 --topic match --create --partitions 3 --replication-factor 1


After this, we should first run com.github.raquelcctome.mongodb.PopulateDB.main() to populate RawDB with dummy events.
Then, start all our consumers com.github.raquelcctome.kafka.Consumers.*.main()
And finally, start to produce to topics. Run com.github.raquelcctome.kafka.Producers.EventProducer.main().

note: As there is a stop criteria to stop the Kafka Consumers, if they don't have anything to consume for a short period of time, they will shutdown.

We can now check our topics and BallPoolGame database. Their will be three Collections with the transformed data.

