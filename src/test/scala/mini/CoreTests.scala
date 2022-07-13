// See LICENSE for license details.

package mini

import chisel3._
import chisel3.testers._
import chisel3.util.experimental.loadMemoryFromFileInline
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// CPU核测试电路
class CoreTester(core: => Core, benchmark: String, trace: Boolean = false) extends BasicTester {
  // 每条指令位4B(32bits) 将测试源文件转化为标准形式 , 每行给一条指令
  val originalHexFile = os.rel / "tests" / f"$benchmark.hex"
  val resizedHexFile = os.rel / "tests" / "32" / f"$benchmark.hex"
  TestUtils.resizeHexFile(os.pwd / originalHexFile, os.pwd / resizedHexFile, 32) // we have 32 bits per memory entry

  // 生成要测试的CPU核
  val dut = Module(core)
  val xlen = dut.conf.xlen // 32
  dut.io.host.fromhost.bits := DontCare // DontCare
  dut.io.host.fromhost.valid := false.B

  // 将测试指令文件加载到Inst mem(外存)中
  val imem = Mem(1 << 20, UInt(xlen.W)) // 2^20 = 1MB
  loadMemoryFromFileInline(imem, resizedHexFile.toString())

  // 将测试数据写入Data mem(外存)中
  val dmem = Mem(1 << 20, UInt(xlen.W)) // 2^20 = 1MB
  loadMemoryFromFileInline(dmem, resizedHexFile.toString())

  val cycle = RegInit(0.U(32.W))

  /** AXI4
    * Cache | >Request>>> | Mem
    *      | Response<<< |
    */
  val iaddr = dut.io.icache.req.bits.addr / (xlen / 8).U
  val daddr = dut.io.dcache.req.bits.addr / (xlen / 8).U

  // .foldLeft https://blog.csdn.net/qq_29677083/article/details/84436462
  // 该信号用于根绝mask有效位拼接数据
  // ??????能综合？
  val write = (0 until(xlen / 8)).foldLeft(0.U(xlen.W)) { (write, i) =>
    write |
      (Mux(
        (dut.io.dcache.req.valid && dut.io.dcache.req.bits.mask(i)).asBool,
        dut.io.dcache.req.bits.data,
        dmem(daddr)
      )(8 * (i + 1) - 1, 8 * i) << (8 * i).U).asUInt
      //( Bit range )
  }
  dut.io.icache.resp.valid := !reset.asBool
  dut.io.dcache.resp.valid := !reset.asBool
  dut.io.icache.resp.bits.data := RegNext(imem(iaddr))
  dut.io.dcache.resp.bits.data := RegNext(dmem(daddr)) // why there need RegNext? Postive Edge?

  // valid高电平表示写入信号
  when(dut.io.icache.req.valid) {
    if (trace) printf("INST[%x] => %x\n", iaddr * (xlen / 8).U, imem(iaddr))
  }

  // 当接收到Cache写入的请求
  when(dut.io.dcache.req.valid) {
    // 如果需要mask,则按照上方的write方法返回
    when(dut.io.dcache.req.bits.mask.orR) {
      dmem(daddr) := write
      if (trace) printf("MEM[%x] <= %x\n", daddr * (xlen / 8).U, write)
    }.otherwise {
      // 如果mask = 0 , 意味着全部无效,不需要操作
      if (trace) printf("MEM[%x] => %x\n", daddr * (xlen / 8).U, dmem(daddr))
    }
  }
  // chisel3中寄存器包含隐式时钟，默认在上升沿执行该赋值
  cycle := cycle + 1.U
  when(dut.io.host.tohost =/= 0.U) {
    printf("cycles: %d\n", cycle)
    assert((dut.io.host.tohost >> 1.U).asUInt === 0.U, "* tohost: %d *\n", dut.io.host.tohost)
    stop()
  }

  if (trace)
    printf("DEBUG_PC : %x\n" ,dut.io.debug.wb_pc)
}

object DefaultCoreConfig {
  def apply() = MiniConfig().core
}

// 定义chisel测试类
class CoreSimpleTests extends AnyFlatSpec with ChiselScalatestTester {
  // 描述测试对象
  behavior.of("Core")
  // 测试样例 #1
  it should "execute a ADD test" in {
    test(new CoreTester(new Core(DefaultCoreConfig()), "rv32ui-p-add" , true)).runUntilStop()
  }
  // 测试样例 #2
  it should "execute a BLT test" in {
    test(new CoreTester(new Core(DefaultCoreConfig()), "rv32ui-p-blt" , true)).runUntilStop()
  }
}

abstract class CoreTests(cfg: TestConfig, useVerilator: Boolean = false)
    extends AnyFlatSpec
    with ChiselScalatestTester {
  behavior.of("Core")
  val opts = if (useVerilator) Seq(VerilatorBackendAnnotation) else Seq()
  cfg.tests.foreach { name =>
    it should s"execute $name" taggedAs IntegrationTest in {
      test(new CoreTester(new Core(DefaultCoreConfig()), name)).withAnnotations(opts).runUntilStop(cfg.maxcycles)
    }
  }
}

class CoreISATests extends CoreTests(ISATests)
class CoreBmarkTests extends CoreTests(BmarkTests, true)
