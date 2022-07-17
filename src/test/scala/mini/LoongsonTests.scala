package mini

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec



class LoongsonTests extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "Loongson"


  val c = LoongConfig()
  it should "inst_ram" in {
    test(new TileTester(Tile(c),"inst_ram")).runUntilStop(10000)
  }

  behavior of "self test-cases"

  val cases = List(
    "sw"
  )
  cases.foreach(name => {
    it should s"${name}" in {
      test(new TileTester(Tile(c) , name ,latency = 2) ).runUntilStop(50)
    }
  })
}
