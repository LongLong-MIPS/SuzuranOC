// See LICENSE for license details.

package mips

import chisel3._
import chisel3.util._
import mips.Control._

class ImmGenIO(xlen: Int) extends Bundle {
  val inst = Input(UInt(xlen.W))
  val sel = Input(UInt(3.W))
  val pc = Input(UInt(xlen.W))
  val out = Output(UInt(xlen.W))
}
// 立即数生成模块, RISC-V的立即数是分散的,
// 需要按照一定规则组合起来
trait ImmGen extends Module {
  def xlen: Int
  val io: ImmGenIO
}

class ImmGenWire(val xlen: Int) extends ImmGen {
  val io = IO(new ImmGenIO(xlen))

  val Iimm = io.inst(15,0).asSInt // 立即数有符号扩展
  val Uimm = io.inst(15,0).zext   // 立即数零扩展
  val Himm = Cat( io.inst(15, 0), 0.U(16.W)).asSInt
  val Bimm = (io.inst(15 , 0) << 2.U).asSInt
  val Simm = io.inst(10 , 6).asSInt //
  val Jimm = Cat(
    io.pc(xlen - 1 , xlen - 4) ,
    io.inst(25 , 0),
    0.U(2.W)
  ).zext // ????????

  io.out := MuxLookup(
    io.sel,
    Iimm & (-2).S,
    Seq(
      IMM_I -> Iimm,
      IMM_U -> Uimm,
      IMM_H -> Himm,
      IMM_S -> Simm,
      IMM_B -> Bimm,
      IMM_J -> Jimm)
  ).asUInt
}

//class ImmGenMux(val xlen: Int) extends ImmGen {
//  val io = IO(new ImmGenIO(xlen))
//  val sign = Mux(io.sel === IMM_Z, 0.S, io.inst(31).asSInt)
//  val b30_20 = Mux(io.sel === IMM_U, io.inst(30, 20).asSInt, sign)
//  val b19_12 = Mux(io.sel =/= IMM_U && io.sel =/= IMM_J, sign, io.inst(19, 12).asSInt)
//  val b11 = Mux(
//    io.sel === IMM_U || io.sel === IMM_Z,
//    0.S,
//    Mux(io.sel === IMM_J, io.inst(20).asSInt, Mux(io.sel === IMM_B, io.inst(7).asSInt, sign))
//  )
//  val b10_5 = Mux(io.sel === IMM_U || io.sel === IMM_Z, 0.U, io.inst(30, 25))
//  val b4_1 = Mux(
//    io.sel === IMM_U,
//    0.U,
//    Mux(io.sel === IMM_S || io.sel === IMM_B, io.inst(11, 8), Mux(io.sel === IMM_Z, io.inst(19, 16), io.inst(24, 21)))
//  )
//  val b0 =
//    Mux(io.sel === IMM_S, io.inst(7), Mux(io.sel === IMM_I, io.inst(20), Mux(io.sel === IMM_Z, io.inst(15), 0.U)))
//
//  io.out := Cat(sign, b30_20, b19_12, b11, b10_5, b4_1, b0).asSInt.asUInt
//}
