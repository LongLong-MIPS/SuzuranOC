// See LICENSE for license details.

package mips

import chisel3._

// 双端口读出/单端口写入的的RegIO
class RegFileIO(xlen: Int) extends Bundle {
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(xlen.W))
  val rdata2 = Output(UInt(xlen.W))

  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
  val wdata = Input(UInt(xlen.W))
}

class RegFile(xlen: Int) extends Module {
  val io = IO(new RegFileIO(xlen))

  // Mem():  https://www.chisel-lang.org/chisel3/docs/explanations/memories.html
  val regs = Mem(32, UInt(xlen.W))

  // .orR将二进制数的每一位取or压缩到一位
  io.rdata1 := Mux(io.raddr1.orR, regs(io.raddr1), 0.U)
  io.rdata2 := Mux(io.raddr2.orR, regs(io.raddr2), 0.U)

  // 可写信号 & Non-Zero寄存器
  when(io.wen & io.waddr.orR) {
    regs(io.waddr) := io.wdata
  }
}
