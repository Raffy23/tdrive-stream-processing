package tdrive.util

import org.apache.flink.api.common.functions.AggregateFunction
import tdrive.shared.dto.TaxiData

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
class TaxiDataCounter extends AggregateFunction[TaxiData, Int, Int] {

  override def createAccumulator(): Int = 0

  override def add(value: TaxiData, accumulator: Int): Int = accumulator + 1

  override def getResult(accumulator: Int): Int = accumulator

  override def merge(a: Int, b: Int): Int = a + b

}
