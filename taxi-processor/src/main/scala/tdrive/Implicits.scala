package tdrive

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala.{DataStream, OutputTag}
import tdrive.TaxiJob.emitToOutput

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
object Implicits {

  implicit class CustomDataStream[R](stream: DataStream[R])(implicit ev2: TypeInformation[R]) {
    def processToSink[S](sideOutput: OutputTag[S], func: R => S, sink: SinkFunction[S])(implicit ev1: TypeInformation[S]): DataStream[R] = {
      stream.process(emitToOutput(sideOutput, func)).getSideOutput(sideOutput).addSink(sink)
      stream
    }

    def processToFilteredSink[S](sideOutput: OutputTag[S], func: R => S, filter: S => Boolean, sink: SinkFunction[S])(implicit ev1: TypeInformation[S]): DataStream[R] = {
      stream.process(emitToOutput(sideOutput, func)).getSideOutput(sideOutput).filter(filter).addSink(sink)
      stream
    }
  }

}
