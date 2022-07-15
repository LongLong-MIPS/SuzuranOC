package mini

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec



class LoongsonTests extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "Loongson"


  val c = LoongConfig()
//  it should "inst_ram" in {
//    test(new TileTester(Tile(c),"inst_ram")).runUntilStop(1000)
//  }

  behavior of "self test-cases"

  val cases = List(
    "jal"
  )
  cases.foreach(name => {
    it should s"${name}" in {
      test(new TileTester(Tile(c),name)).runUntilStop(350)
    }
  })
}
