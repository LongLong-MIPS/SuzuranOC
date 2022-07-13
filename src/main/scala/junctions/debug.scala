package junctions

import chisel3._


class DebugBundle(addrBits : Int , dataBits : Int) extends Bundle {
  val wb_pc = Output(UInt(addrBits.W))
  val wb_rf_wen = Output(UInt(4.W))
  val wb_rf_wnum = Output(UInt(5.W))
  val wb_rf_wdata = Output(UInt(dataBits.W))
}
