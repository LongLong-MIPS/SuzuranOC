// See LICENSE for license details.

package mips

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import junctions._

// Cache实现 https://www.bilibili.com/video/BV1364y117ZB
// Cache Require
// 用来表示dataW中那些字节时无效的
class CacheReq(addrWidth: Int, dataWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
  val data = UInt(dataWidth.W)
  val mask = UInt((dataWidth / 8).W)
}
// Cache Response
// 用来返回写入是否成功
class CacheResp(dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
}

// 一些关于Valid() 的API介绍
// *** 注意关于bit用法 , 默认为Output() ***
// https://www.chisel-lang.org/api/3.5.3/chisel3/util/Valid$.html
class CacheIO(addrWidth: Int, dataWidth: Int) extends Bundle {
  val abort = Input(Bool())
  val req = Flipped(Valid(new CacheReq(addrWidth, dataWidth)))
  val resp = Valid(new CacheResp(dataWidth))
}

// CPU <> Cache <> MemAbi
//   CPU-IO   NASTI-IO
class CacheModuleIO(nastiParams: NastiBundleParameters, addrWidth: Int, dataWidth: Int) extends Bundle {
  val cpu = new CacheIO(addrWidth, dataWidth)
  val nasti = new NastiBundle(nastiParams)
}

case class CacheConfig(nWays: Int, nSets: Int, blockBytes: Int)

class MetaData(tagLength: Int) extends Bundle {
  val tag = UInt(tagLength.W)
}

object CacheState extends ChiselEnum {
  val sIdle, sReadCache, sWriteCache, sWriteBack, sWriteAck, sRefillReady, sRefill = Value
}

//  |______Tag______|__Line__|___Offset___|
class Cache(val p: CacheConfig, val nasti: NastiBundleParameters, val xlen: Int) extends Module {
  // local parameters
  val nSets = p.nSets // Cache 组数 --- line
  val bBytes = p.blockBytes // Cache 每一行的字节数 --- offset --- 16Bytes
  val bBits = bBytes << 3 // Cache 每一行的比特位数  --- offset --- 128bits
  val blen = log2Ceil(bBytes) // Cache 每行内编址占用的比特位数 --- offset
  val slen = log2Ceil(nSets) // Cache 组号编址占用的比特位数 --- line
  val tlen = xlen - (slen + blen) // 地址Tag位
  val nWords = bBits / xlen // 4
  val wBytes = xlen / 8 // for mask 掩模 4
  val byteOffsetBits = log2Ceil(wBytes) //
  val dataBeats = bBits / nasti.dataBits // axi len?数据传输的次数

  val io = IO(new CacheModuleIO(nasti, addrWidth = xlen, dataWidth = xlen))

  // cache states
  import CacheState._
  val state = RegInit(sIdle)
  // memory
  val v = RegInit(0.U(nSets.W))
  val d = RegInit(0.U(nSets.W))
  val metaMem = SyncReadMem(nSets, new MetaData(tlen))

  /**
    * dataMem 带有掩模的并行SRAM 交叉存储
    * 256个Cache行，每行4字节
    * 并行nWords = 4  nSets = 256 , wBytes = 4
    *
    * 256 * Vec : 00000000 00000000 00000000 00000000 : 4 mask = 1Words
    */

  val dataMem = Seq.fill(nWords)(SyncReadMem(nSets, Vec(wBytes, UInt(8.W))))

  val addr_reg = Reg(chiselTypeOf(io.cpu.req.bits.addr))
  val cpu_data = Reg(chiselTypeOf(io.cpu.req.bits.data))
  val cpu_mask = Reg(chiselTypeOf(io.cpu.req.bits.mask))

  // Counters
  require(dataBeats > 0)
  val (read_count, read_wrap_out)   = Counter(io.nasti.r.fire, dataBeats)
  val (write_count, write_wrap_out) = Counter(io.nasti.w.fire, dataBeats)

  val is_idle = state === sIdle
  val is_read = state === sReadCache
  val is_write = state === sWriteCache
  val is_alloc = state === sRefill && read_wrap_out
  val is_alloc_reg = RegNext(is_alloc)

  val hit = Wire(Bool())
  val wen = is_write && (hit || is_alloc_reg) && !io.cpu.abort || is_alloc
  val ren = !wen && (is_idle || is_read) && io.cpu.req.valid
  val ren_reg = RegNext(ren)

  val addr = io.cpu.req.bits.addr
  val idx = addr(slen + blen - 1, blen)
  val tag_reg = addr_reg(xlen - 1, slen + blen)
  val idx_reg = addr_reg(slen + blen - 1, blen)
  val off_reg = addr_reg(blen - 1, byteOffsetBits)

  val rmeta = metaMem.read(idx, ren)
  val rdata = Cat((dataMem.map(_.read(idx, ren).asUInt)).reverse)
  val rdata_buf = RegEnable(rdata, ren_reg)
  val refill_buf = Reg(Vec(dataBeats, UInt(nasti.dataBits.W)))
  val read = Mux(is_alloc_reg, refill_buf.asUInt, Mux(ren_reg, rdata, rdata_buf))

  hit := v(idx_reg) && rmeta.tag === tag_reg

  // Read Mux
  io.cpu.resp.bits.data := VecInit.tabulate(nWords)(i => read((i + 1) * xlen - 1, i * xlen))(off_reg)
  io.cpu.resp.valid := is_idle || is_read && hit || is_alloc_reg && !cpu_mask.orR

  when(io.cpu.resp.valid) {
    addr_reg := addr
    cpu_data := io.cpu.req.bits.data
    cpu_mask := io.cpu.req.bits.mask
  }

  val wmeta = Wire(new MetaData(tlen))
  wmeta.tag := tag_reg

  val wmask = Mux(
    !is_alloc,
    (cpu_mask << Cat(off_reg, 0.U(byteOffsetBits.W) ) ).asUInt.zext, // WARNING?
    (-1).S
  )
  val wdata = Mux(
    !is_alloc,
    Fill(nWords, cpu_data),
    if (refill_buf.size == 1) io.nasti.r.bits.data
    else Cat(io.nasti.r.bits.data, Cat(refill_buf.init.reverse))
  )
  when(wen) {
    v := v.bitSet(idx_reg, true.B)
    d := d.bitSet(idx_reg, !is_alloc)
    when(is_alloc) {
      metaMem.write(idx_reg, wmeta)
    }
    dataMem.zipWithIndex.foreach {
      case (mem, i) =>
        val data = VecInit.tabulate(wBytes)(k => wdata(i * xlen + (k + 1) * 8 - 1, i * xlen + k * 8))
        mem.write(idx_reg, data, wmask((i + 1) * wBytes - 1, i * wBytes).asBools)
        mem.suggestName(s"dataMem_${i}")
    }
  }

  io.nasti.ar.bits := NastiAddressBundle(nasti)(
    0.U,
    (Cat(tag_reg, idx_reg) << blen.U).asUInt,
    log2Up(nasti.dataBits / 8).U,
    (dataBeats - 1).U
  )
  io.nasti.ar.valid := false.B
  // read data
  io.nasti.r.ready := state === sRefill
  when(io.nasti.r.fire) {
    refill_buf(read_count) := io.nasti.r.bits.data
  }

  // write addr
  io.nasti.aw.bits := NastiAddressBundle(nasti)(
    0.U,
    (Cat(rmeta.tag, idx_reg) << blen.U).asUInt,
    log2Ceil(nasti.dataBits / 8).U,
    (dataBeats - 1).U
  )
  io.nasti.aw.valid := false.B
  // write data
  io.nasti.w.bits := NastiWriteDataBundle(nasti)(
    VecInit.tabulate(dataBeats)(i => read((i + 1) * nasti.dataBits - 1, i * nasti.dataBits))(write_count),
    None,
    write_wrap_out
  )
  io.nasti.w.valid := false.B
  // write resp
  io.nasti.b.ready := false.B

  // Cache FSM
  val is_dirty = v(idx_reg) && d(idx_reg)
  switch(state) {
    is(sIdle) {
      when(io.cpu.req.valid) {
        state := Mux(io.cpu.req.bits.mask.orR, sWriteCache, sReadCache)
      }
    }
    is(sReadCache) {
      when(hit) {
        when(io.cpu.req.valid) {
          state := Mux(io.cpu.req.bits.mask.orR, sWriteCache, sReadCache)
        }.otherwise {
          state := sIdle
        }
      }.otherwise {
        io.nasti.aw.valid := is_dirty
        io.nasti.ar.valid := !is_dirty
        when(io.nasti.aw.fire) {
          state := sWriteBack
        }.elsewhen(io.nasti.ar.fire) {
          state := sRefill
        }
      }
    }
    is(sWriteCache) {
      when(hit || is_alloc_reg || io.cpu.abort) {
        state := sIdle
      }.otherwise {
        io.nasti.aw.valid := is_dirty
        io.nasti.ar.valid := !is_dirty
        when(io.nasti.aw.fire) {
          state := sWriteBack
        }.elsewhen(io.nasti.ar.fire) {
          state := sRefill
        }
      }
    }
    is(sWriteBack) {
      io.nasti.w.valid := true.B
      when(write_wrap_out) {
        state := sWriteAck
      }
    }
    is(sWriteAck) {
      io.nasti.b.ready := true.B
      when(io.nasti.b.fire) {
        state := sRefillReady
      }
    }
    is(sRefillReady) {
      io.nasti.ar.valid := true.B
      when(io.nasti.ar.fire) {
        state := sRefill
      }
    }
    is(sRefill) {
      when(read_wrap_out) {
        state := Mux(cpu_mask.orR, sWriteCache, sIdle)
      }
    }
  }
}

class ThroughCacheIO(addrWidth: Int, dataWidth: Int)
    extends CacheIO(addrWidth, dataWidth) {
  val direct_en = Input(Bool())
}

class ThroughCacheModuleIO(nastiParams: NastiBundleParameters, addrWidth: Int, dataWidth: Int) extends Bundle {
  val cpu = new ThroughCacheIO(addrWidth, dataWidth)
  val nasti = new NastiBundle(nastiParams)
}

/**
  * @note switch交替访问还没测试
  */
class ThroughCache(
  val p:     CacheConfig,
  val nasti: NastiBundleParameters,
  val xlen:  Int)
    extends Module
{
  val io = IO(new ThroughCacheModuleIO(nasti , xlen, xlen))

  val cache = Module(new Cache(p, nasti, xlen))

  io.nasti.ar.valid := false.B
  io.nasti.aw.valid := false.B
  io.nasti.b.ready := false.B
  io.nasti.w.valid := false.B
  io.nasti.r.ready := false.B

  cache.io.nasti.b.valid := false.B
  cache.io.nasti.r.valid := false.B
  cache.io.nasti.ar.ready := false.B
  cache.io.nasti.aw.ready := false.B
  cache.io.nasti.w.ready := false.B

  cache.io.nasti.b.bits := DontCare
  cache.io.nasti.r.bits := DontCare

  val direct_r_data = Reg(UInt(nasti.dataBits.W))

  import CacheState._
  val state = RegInit(sIdle)
  val switch_reg = RegInit(false.B)

  switch_reg := io.cpu.direct_en
  io.cpu.req <> cache.io.cpu.req
  io.cpu.resp <> cache.io.cpu.resp
  io.cpu.abort <> cache.io.cpu.abort


  io.nasti := DontCare
  val mask_reg = RegInit(0.U(4.W))
  val addr_reg = RegInit(0.U(32.W))
  val valid_reg = RegInit(false.B)
  mask_reg  := io.cpu.req.bits.mask
  addr_reg  := io.cpu.req.bits.addr
  valid_reg := io.cpu.req.valid


//  printf("DEBUG cache status : \n" +
//  "\tDirect : %x; Valid : %x; State : %x \n",
//    switch_reg , valid_reg , state.asUInt
//  )

  when(!switch_reg) {
    io.nasti <> cache.io.nasti

  }.otherwise {
    io.nasti.aw.valid := state === sWriteCache
    io.nasti.w.valid  := state === sWriteCache
    io.nasti.b.ready  := state === sWriteCache
    io.nasti.r.ready  := state === sReadCache
    io.nasti.ar.valid := state === sReadCache

    io.nasti.ar.bits := NastiAddressBundle(nasti)(
      0.U,
      addr_reg,
      log2Up(nasti.dataBits / 8).U
    )
    io.nasti.w.bits := NastiWriteDataBundle(nasti)(
      io.cpu.req.bits.data,
      Some(mask_reg)
    )
    io.nasti.aw.bits := NastiAddressBundle(nasti)(
      0.U, addr_reg, 2.U
    )
    io.cpu.resp.bits.data := direct_r_data
    io.cpu.resp.valid := state === sIdle && !valid_reg

    switch(state) {
      is(sIdle) {
        when(valid_reg && !mask_reg.orR) {
          state := sReadCache
        }
        when(valid_reg &&  mask_reg.orR) {
//          printf("DEBUG Direct write : %x ; size : %x ; mask : %x\n",
//            io.cpu.req.bits.addr , io.nasti.aw.bits.size , mask_reg);

          state := sWriteCache
        }
      }
      is(sReadCache) {
        when(io.nasti.r.fire) {
          direct_r_data := io.nasti.r.bits.data
          state := sIdle
        }
      }
      is(sWriteCache) {
        when(io.nasti.b.fire) {
          state := sIdle
        }
      }
    }
  }
}
