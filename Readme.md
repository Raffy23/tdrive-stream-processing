TDrive - Taxi data processing
=============================
A project that does process the [T-Drive trajectory](https://www.microsoft.com/en-us/research/publication/t-drive-trajectory-data-sample/)
data with different scalable technologies, such as Kafka, Flink, Akka and Cassandra.  

```  
   (Data Source) 
        |
        V                                     (Event processing)
+----------------+        +---------+          +--------------+
|                |        |         +--------->|              |
| Kafka-Ingestor +------->|  Kafka  |          |    Flink     |
|                |        |         |<---------+              |
+----------------+        +----+----+          +--------------+
                               |                       
                               |
                               v
                       +-------+-------+
         +-------------|               |
         |             |  Akka Cluster |
   +-----v-------+     |               |
   |             |     +---------+-----+
   |  Cassandra  |            ^  |
   |             |            |  | (Server Push over Websocket)
   +-------------+            |  | (Fetch requests for simple queries) 
   (Data Storage)             |  |
                              v  v
                        +--------------+
                        |              |
                        |    Web UI    |
                        |              |
                        +--------------+
```
<small>*Inspired by the AIC-Project, which used Apache Strom instead of Apache Flink and Redis as 
key-value store between Storm and the Web-Server*</small>

## Parts
* **Importer:** 
    The importer does transform the data sample into a usable format. Filtering and selecting specific
    taxis is done in the pre-processing step. The importer reads the configuration from the 
    `./config/importer.conf` configuration file, or from the file given as the first argument.

* **Kafka-Ingestor:**
    the Kafka ingestor reads the output file of the Importer and proceeds to push the data into kafka.
    The Ingestor can be configured to either simulate real-time data or speed up or slow down the rate
    at which data is pushed to kafka. The kafka ingestor reads the configuration from the 
    `./config/ingestor.properties` configuration file, or from the file given as the first argument.
    
* **Taxi-Processor:** 
    A Apache Flink job, which is in charge of computing average and current speed and other notifications.
    After processing the data is emitted into different kafka topics, which are read by the akka cluster 
    (web-server). The Job is configured with the `--kafka.server` command line parameter.
     
* **web-server:** 
    A Akka Cluster that does connect to kafka and provides the data to a user interface over websocket.
    Besides handing the events from kafka to the client the events are also stored into cassandra where
    the client can retrieve data not recieved by the current event stream, e.g. old data. The server
    can be configured by the `application.conf` and `kafka.conf` files.
        
* **web-client:** 
    *Planned*, A ScalaJS Client that shows taxi movements on a map as they are ommited by the websocket 

* **taxi-visualizer:** 
    A OpenGL renderer that creates videos of the taxi movement with the help of ffmpeg. The renderer
    uses the output file of the importer as data source for the video. The renderer can be configured
    with the `./config/importer.conf` configuration file. Before it can be used make sure the path
    to ffmpeg is set correctly.

## TODO
* Documentation
* Cassandra integration
* Implement web-client
* Optimize taxi-visualizer
* Docker files & docker-compose

## Configuration
TODO

## Runtime Dependencies
* Kafka
* Cassandra
* Flink Job Manager *(optional)*
  * Taxi-Processor can also be started as standalone from sbt and IntelliJ 

## Docker
TODO

## HowTo
* Build (TODO)
* Run (TODO)
