// See LICENSE for license details.

package mini

import chisel3._
import mini.Control._

class BrCondIO(xlen: Int) extends Bundle {
  val rs1 = Input(UInt(xlen.W))
  val rs2 = Input(UInt(xlen.W))
  val br_type = Input(UInt(3.W))
  val taken = Output(Bool())
}

trait BrCond extends Module {
  def xlen: Int
  val io: BrCondIO
}

class BrCondSimple(val xlen: Int) extends BrCond {
  val io = IO(new BrCondIO(xlen))
  val eq = io.rs1 === io.rs2
  val neq = !eq
  val lt = io.rs1.asSInt < io.rs2.asSInt
  val ge = !lt
  val ltu = io.rs1 < io.rs2
  val geu = !ltu
//  io.taken :=
//    ((io.br_type === BR_EQ) && eq) ||
//      ((io.br_type === BR_NE) && neq) ||
//      ((io.br_type === BR_LT) && lt) ||
//      ((io.br_type === BR_GE) && ge) ||
//      ((io.br_type === BR_LTU) && ltu) ||
//      ((io.br_type === BR_GEU) && geu)
}

/**
  *
/**
  val BR_LZ = 1.U(3.W)//小于0时发生跳转
  val BR_LE = 2.U(3.W)//小于等于0时发生跳转
  val BR_EQ = 3.U(3.W)//相等发生跳转
  val BR_GZ = 4.U(3.W)//大于0时发生跳转
  val BR_GE = 5.U(3.W)//大于等于0时发生跳转
  val BR_NE = 6.U(3.W)//不相等发生跳转
    */
  * @param xlen
  */

class BrCondArea(val xlen: Int) extends BrCond {
  val io = IO(new BrCondIO(xlen))
  val diff = io.rs1 - io.rs2
  val neq = diff.orR
  val eq = !neq

  val neg = io.rs1(xlen - 1)
  val ez = !io.rs1.orR
  val lz = !ez & neg
  val le =  ez | neg
  val gz = !le
  val ge = !lz

  io.taken :=
    ((io.br_type === BR_EQ) && eq) ||
      ((io.br_type === BR_NE) && neq) ||
      ((io.br_type === BR_LZ) && lz) ||
      ((io.br_type === BR_GZ) && gz) ||
      ((io.br_type === BR_GE) && ge) ||
      ((io.br_type === BR_LE) && le)
}
