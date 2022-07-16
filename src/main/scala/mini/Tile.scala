package mini

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import junctions._

class MemArbiterIO(params: NastiBundleParameters) extends Bundle {
  val icache = Flipped(new NastiBundle(params))
  val dcache = Flipped(new NastiBundle(params))
  val nasti = new NastiBundle(params)
}

object MemArbiterState extends ChiselEnum {
  val sIdle, sICacheRead, sDCacheRead, sDCacheWrite, sDCacheAck = Value
}

/**
  * 内存仲裁器，负责cache与memory的协调工作
  *
  *          Cache -------------- Memory
  *                     NAS
  *   Address Write   == AW == >
  *   Data Write      == W  == >
  *   Response      < == B  ==
  *
  *   Address Read    == AR == >
  *   Data Read     < == R  ==
  */
class MemArbiter(params: NastiBundleParameters) extends Module {
  val io = IO(new MemArbiterIO(params))

  import MemArbiterState._
  val state = RegInit(sIdle)

  // Write Address
  io.nasti.aw.bits := io.dcache.aw.bits
  io.nasti.aw.valid := io.dcache.aw.valid && state === sIdle
  io.dcache.aw.ready := io.nasti.aw.ready && state === sIdle
  io.icache.aw := DontCare

  // Write Data
  io.nasti.w.bits := io.dcache.w.bits
  io.nasti.w.valid := io.dcache.w.valid && state === sDCacheWrite
  io.dcache.w.ready := io.nasti.w.ready && state === sDCacheWrite
  io.icache.w := DontCare

  // Write Ack
  io.dcache.b.bits := io.nasti.b.bits
  io.dcache.b.valid := io.nasti.b.valid && state === sDCacheAck
  io.nasti.b.ready := io.dcache.b.ready && state === sDCacheAck
  io.icache.b := DontCare

  // Read Address
  io.nasti.ar.bits := NastiAddressBundle(params)(
    Mux(io.dcache.ar.valid, io.dcache.ar.bits.id, io.icache.ar.bits.id),
    Mux(io.dcache.ar.valid, io.dcache.ar.bits.addr, io.icache.ar.bits.addr),
    Mux(io.dcache.ar.valid, io.dcache.ar.bits.size, io.icache.ar.bits.size),
    Mux(io.dcache.ar.valid, io.dcache.ar.bits.len, io.icache.ar.bits.len)
  )
  io.nasti.ar.valid := (io.icache.ar.valid || io.dcache.ar.valid) &&
    !io.nasti.aw.valid && state === sIdle
  io.dcache.ar.ready := io.nasti.ar.ready && !io.nasti.aw.valid && state === sIdle
  io.icache.ar.ready := io.dcache.ar.ready && !io.dcache.ar.valid

  // Read Data
  io.icache.r.bits := io.nasti.r.bits
  io.dcache.r.bits := io.nasti.r.bits
  io.icache.r.valid := io.nasti.r.valid && state === sICacheRead
  io.dcache.r.valid := io.nasti.r.valid && state === sDCacheRead
  io.nasti.r.ready := io.icache.r.ready && state === sICacheRead ||
    io.dcache.r.ready && state === sDCacheRead

  // .fire() target.ready && target.valid 表示握手成功
  switch(state) {
    is(sIdle) {
      printf("INFO sIdle\n")
      when(io.dcache.aw.fire) {
        state := sDCacheWrite
      }.elsewhen(io.dcache.ar.fire) {
        state := sDCacheRead
      }.elsewhen(io.icache.ar.fire) {
        state := sICacheRead
      }
    }
    is(sICacheRead) {
      printf("INFO sICacheRead __ MemValid : %x __ IcacheReady : %x\n" +
             "                 __ MemData  : %x\n",
        io.nasti.r.valid , io.icache.r.ready , io.nasti.r.bits.data)

      when(io.nasti.r.fire && io.nasti.r.bits.last) {
        state := sIdle
      }
    }
    is(sDCacheRead) {
      printf("INFO sDCacheRead __ MemValid : %x __ DcacheReady : %x\n" +
        "                      __ MemData  : %x\n",
        io.nasti.r.valid , io.dcache.r.ready , io.nasti.r.bits.data)

      when(io.nasti.r.fire && io.nasti.r.bits.last) {
        state := sIdle
      }
    }
    is(sDCacheWrite) {
      when(io.dcache.w.fire && io.dcache.w.bits.last) {
        state := sDCacheAck
      }
    }
    is(sDCacheAck) {
      printf("INFO sDCacheAck\n")

      when(io.nasti.b.fire) {
        state := sIdle
      }
    }
  }
}

class TileIO(xlen: Int, nastiParams: NastiBundleParameters) extends Bundle {
  val host = new HostIO(xlen)
  val nasti = new NastiBundle(nastiParams)

  val debug = new DebugBundle(xlen , xlen)
}

object Tile {
  def apply(config: Config): Tile = new Tile(config.core, config.nasti, config.cache)
}

class Tile(val coreParams: CoreConfig, val nastiParams: NastiBundleParameters, val cacheParams: CacheConfig)
    extends Module {
  val io = IO(new TileIO(coreParams.xlen, nastiParams))

  val core = Module(new Core(coreParams))
  val icache = Module(new ThroughCache(cacheParams, nastiParams, coreParams.xlen))
  val dcache = Module(new ThroughCache(cacheParams, nastiParams, coreParams.xlen))
  val arb = Module(new MemArbiter(nastiParams))

  io.host <> core.io.host
  io.debug <> core.io.debug

  icache.io.cpu <> core.io.icache
  dcache.io.cpu <> core.io.dcache
  arb.io.icache <> icache.io.nasti
  arb.io.dcache <> dcache.io.nasti
  io.nasti <> arb.io.nasti
}
