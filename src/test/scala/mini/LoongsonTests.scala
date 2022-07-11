package mini

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class LoongsonTests extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "Loongson"

  val c = LoongConfig()
  it should "test case #1" in {
    test(new TileTester(Tile(c),"rv32ui-p-simple")).runUntilStop()
  }
}
