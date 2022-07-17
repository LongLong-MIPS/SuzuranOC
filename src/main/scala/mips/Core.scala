// See LICENSE for license details.

package mips

import chisel3._
import chisel3.util.Valid
import junctions.{DebugBundle}

// 样例类，用于配置参数的示例，并未真正使用
case class CoreConfig(
  xlen:       Int,
  makeAlu:    Int => Alu = new AluSimple(_),
  makeBrCond: Int => BrCond = new BrCondSimple(_),
  makeImmGen: Int => ImmGen = new ImmGenWire(_)
)

class HostIO(xlen: Int) extends Bundle {
  val fromhost = Flipped(Valid(UInt(xlen.W)))
  val tohost = Output(UInt(xlen.W))
}

// host为总线相关接口
// 注意这里又反转了一次
// resp为输入信号 req为输出
class CoreIO(xlen: Int) extends Bundle {
  val host = new HostIO(xlen)
  val icache = Flipped(new ThroughCacheIO(xlen, xlen))
  val dcache = Flipped(new ThroughCacheIO(xlen, xlen))

  val debug = new DebugBundle(xlen , xlen)
}

// Core声明
class Core(val conf: CoreConfig) extends Module {
  val io = IO(new CoreIO(conf.xlen))
  val dpath = Module(new Datapath(conf))
  val ctrl = Module(new Control)

  io.host <> dpath.io.host
  io.debug <> dpath.io.debug

  io.icache <> dpath.io.icache
  io.dcache <> dpath.io.dcache
  dpath.io.ctrl <> ctrl.io
}
