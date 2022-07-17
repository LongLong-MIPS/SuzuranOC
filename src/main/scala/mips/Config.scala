// See LICENSE for license details.

package mips

import junctions.NastiBundleParameters

case class Config(core: CoreConfig, cache: CacheConfig, nasti: NastiBundleParameters)

/**
  * 配置参数MINI-CPU核的参数
  *
  * 该MINI版本的默认参数为：
  *
  * Cache ：直接映射（1路）
  *
  * AXI ： 数据位宽：64位
  *         地址位宽 32位
  *         可连接的设备 1<<5
  */
object MiniConfig {
  def apply(): Config = {
    val xlen = 32
    Config(
      core = CoreConfig(
        xlen = xlen,
        makeAlu = new AluArea(_),
        makeBrCond = new BrCondArea(_),
        makeImmGen = new ImmGenWire(_)
      ),
      cache = CacheConfig(
        nWays = 1,
        nSets = 256,
        blockBytes = 4 * (xlen / 8) // 4 * 32 bits = 16B
      ),
      nasti = NastiBundleParameters(
        addrBits = 32,
        dataBits = 32,
        idBits = 5
      )
    )
  }
}

/**
  * 配置自自有Suzuran-CPU核的参数
  *
  * 该版本的参数为：
  *
  * Cache ：直接映射（1路）
  *
  * AXI ： 数据位宽：32位
  *         地址位宽 32位
  *         可连接的设备 1<<4
  */
object LoongConfig {
  def apply(): Config = {
    val xlen = 32
    Config(
      core = CoreConfig(
        xlen = xlen,
        makeAlu = new AluArea(_),
        makeBrCond = new BrCondArea(_),
        makeImmGen = new ImmGenWire(_)
      ),
      cache = CacheConfig(
        nWays = 1,
        nSets = 256,
        blockBytes = 4 * (xlen / 8) // 4 * 32 bits = 16B
      ),
      nasti = NastiBundleParameters(
        addrBits = xlen,
        dataBits = xlen,
        idBits = 4
      )
    )
  }
}