package tdrive.shared

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.02.2019
  */
object TaxiState {
  def apply(taxi: Taxi): TaxiState = TaxiState(taxi, 0D, 1)
}
case class TaxiState(var taxi: Taxi, var speed: Double, var count: Long) {
  def update(taxi: Taxi, speed: Double): TaxiState = {
    this.taxi = taxi
    this.speed = speed
    this.count += 1

    this
  }
}
