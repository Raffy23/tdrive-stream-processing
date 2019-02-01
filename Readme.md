TDrive - Taxi data processing
=============================
TODO

<smaller>*Inspired by the AIC-Project, which used Apache Strom*</smaller>

## Parts
* **kafka-ingestor:** Puts the data into Kafka, in realtime or faster
* **taxi-processor:** apache flink job that processes the data
* **taxi-visualizer:** OpenGL application that can render a dataset into a video with ffmpeg
* **web-server:** akka server that communicates with the web client over websocket
* **web-client:** ScalaJs client that displays the taxis on a map

### Importer 
The importer does transform the [T-Drive trajectory](https://www.microsoft.com/en-us/research/publication/t-drive-trajectory-data-sample/)
data sample into a usable format. Additionally filtering and dataset reducing is also implemented. 
The importer should transform the data as pre-processing step since the T-Drive data sample can't easily
transformed at is current state.

The importer reads the configuration from the `./config/importer.properties` configuration file, or
from the file given as the first argument.

### Kafka Ingestor
The Kafka ingestor does read the output file of the *importer* and does write the data to Kafka. 
A sleep timeout can be configured, ranging from 1 (for realtime) to 0 (as fast as possible).

The kafka ingestor reads the configuration from the `./config/ingestor.properties` configuration file
, or from the file given as the first argument.

### Taxi Processor
A Apache Flink job, which is in charge of computing average and current speed and other notifications.
After processing the data is emitted to redis for storage where the web-server can retrieve it send it
to the web-client.

TODO: run, commit to Manager, ... 

## Configuration
TODO

## Dependencies
* Kafka
* Redis (?)

## HowTo
### Build
### Run
