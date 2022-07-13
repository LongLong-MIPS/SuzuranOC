package mini

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import junctions.{DebugBundle, NastiBundle, NastiBundleParameters}

// PC_START定义为0x200 定义为cpu上电运行的第一条指令地址
// PC_EVEC 0x100定义为 cpu异常运行的指令起始地址
object Const {
  val PC_START = 0x200
  //val PC_START = 0xBFC00000
  val PC_EVEC  = 0x100

  val ADDR_MASK = 0x1FFFFFFF
  val KSEG0_TAG = 0x4 //b100
  val KSEG1_TAG = 0x5 //b101
  val KSEG2_TAG = 0x6 //b110
  val KSEG3_TAG = 0x7 //b111
}

class VaddrArbiterIO(xlen :  Int) extends Bundle {
  val vaddr = Input(UInt(xlen.W))
  val paddr = Output(UInt(xlen.W))
  val cached_en = Output(Bool())
  val mapped_en = Output(Bool())
}

/**
  * 在功能测试阶段，所有的代码均在kseg1和kseg2运行，所以暂时不用考虑mapping
  */
case class VaddrArbiter(xlen : Int) extends Module {
  val io = IO(new VaddrArbiterIO(xlen))

  val tag = io.vaddr(xlen - 1 , xlen - 4)

  // default ---
  io.paddr := io.vaddr
  io.cached_en := true.B
  io.mapped_en := true.B

  switch(tag) {
    is(Const.KSEG0_TAG.U) {
      io.paddr := io.vaddr & Const.ADDR_MASK.U
      io.cached_en := true.B
      io.mapped_en := false.B
    }
    is(Const.KSEG1_TAG.U) {
      io.paddr := io.vaddr & Const.ADDR_MASK.U
      io.cached_en := false.B
      io.mapped_en := false.B
    }
  }

}


// Flipped() 把原本Output的端口变换为Input , 反之亦然
// 数据通路的IO , ctrl为整个通路的控制信号
class DatapathIO(xlen: Int) extends Bundle {
  val host = new HostIO(xlen)
  val icache = Flipped(new ThroughCacheIO(xlen, xlen))
  val dcache = Flipped(new ThroughCacheIO(xlen, xlen))
  val ctrl = Flipped(new ControlSignals)

  val debug = new DebugBundle(xlen, xlen)
}

// 定义取指和执行流水线级之间的寄存器io,仅仅是为了方便寄存器定义
class FetchExecutePipelineRegister(xlen: Int) extends Bundle {
  val inst = chiselTypeOf(Instructions.NOP)
  val pc = UInt(xlen.W)
}

// 定义执行和写回流水线级之间的寄存器io
class ExecuteWritebackPipelineRegister(xlen: Int) extends Bundle {
  val inst = chiselTypeOf(Instructions.NOP)
  val pc = UInt(xlen.W)
  val alu = UInt(xlen.W)
  val csr_in = UInt(xlen.W)
}

// 定义数据通路的模块 , 状态寄存器 寄存器组 ALU
// 立即数组合器(RISC-V中的立即数需要拼凑)
class Datapath(val conf: CoreConfig) extends Module {
  val io = IO(new DatapathIO(conf.xlen))

  val csr = Module(new CSR(conf.xlen))
  val regFile = Module(new RegFile(conf.xlen))
  val alu = Module(conf.makeAlu(conf.xlen))
  val immGen = Module(conf.makeImmGen(conf.xlen))
  val brCond = Module(conf.makeBrCond(conf.xlen))

  val mmu = Module(VaddrArbiter(conf.xlen))

  import Control._

  /** Pipeline State Registers * */

  /** *** Fetch / Execute Registers ****
    *
    * 把多个寄存器合并为一个寄存器 , lit()为语法糖 , 表示了这个
    * Bundle接口的初始值
    */
  val fe_reg = RegInit(
    (new FetchExecutePipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U
    )
  )

  /** *** Execute / Write Back Registers ****
    */
  val ew_reg = RegInit(
    (new ExecuteWritebackPipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U,
      _.alu -> 0.U,
      _.csr_in -> 0.U
    )
  )

  /** **** Control signals ****
    */
  val st_type  = Reg(io.ctrl.st_type.cloneType)
  val ld_type  = Reg(io.ctrl.ld_type.cloneType)
  val wb_sel   = Reg(io.ctrl.wb_sel.cloneType)
  val wb_en    = Reg(Bool())
  val csr_cmd  = Reg(io.ctrl.csr_cmd.cloneType)
  val illegal  = Reg(Bool())
  val pc_check = Reg(Bool())

  /** **** Fetch ****
    * 包括PC等电路
    */
  val started = RegNext(reset.asBool)
  // stall : stop signal——停机信号 暂停流水线
  // cache冲突，暂停
  val stall = !io.icache.resp.valid || !io.dcache.resp.valid
  val pc = RegInit(Const.PC_START.U(conf.xlen.W) - 4.U(conf.xlen.W)) //注意Line.116 icache连接的是next_pc
  // Next Program Counter
  val next_pc = MuxCase(
    pc + 4.U,
    IndexedSeq(
      stall -> pc,
      csr.io.expt -> csr.io.evec,
      (io.ctrl.pc_sel === PC_EPC) -> csr.io.epc,
      ((io.ctrl.pc_sel === PC_ALU) || (brCond.io.taken)) -> (alu.io.sum >> 1.U << 1.U),
      (io.ctrl.pc_sel === PC_0) -> pc
    )
  )

  /** 从icache中取指部分的电路
    * 为了解决冒险问题 , 在下面四种情况下要进行填充NOP
    * (1) 系统启动或reset时
    * (2) 解决load-use冒险问题 或者 CSR相关
    * (3) 执行跳转指令时:控制冒险
    * (4) CSR
    * (5) cache冲突，暂停
  */
  val inst =
    Mux(started || io.ctrl.inst_kill || brCond.io.taken || csr.io.expt,
      Instructions.NOP,
      io.icache.resp.bits.data) // response 通道
  pc := next_pc

  mmu.io.vaddr := next_pc
  printf("VADDR : %x == %x \n" , next_pc , mmu.io.paddr)

  io.icache.req.bits.addr := mmu.io.paddr
  io.icache.req.bits.data := 0.U
  io.icache.req.bits.mask := 0.U
  io.icache.req.valid := !stall
  io.icache.abort := false.B
  io.icache.direct_en := true.B //!
  // Pipelining
  when(!stall) {
    fe_reg.pc := pc
    fe_reg.inst := inst
  }

  /** **** Execute ****
    */
  io.ctrl.inst := fe_reg.inst

  // regFile read
  val rd_addr = fe_reg.inst(11, 7)
  val rs1_addr = fe_reg.inst(19, 15)
  val rs2_addr = fe_reg.inst(24, 20)
  regFile.io.raddr1 := rs1_addr
  regFile.io.raddr2 := rs2_addr

  // gen immdeates
  immGen.io.inst := fe_reg.inst
  immGen.io.sel := io.ctrl.imm_sel

  // bypass
  /**
    *  *** Data Wizard ***
    *  https://zhuanlan.zhihu.com/p/447682231
    *
    * (1) RAW(Read After Write) <- THIS ONE !
    *     add x5*, x4, x6
    *     add x4, x5*, x2
    *
    * (2) WAW(write after write)  (Out-Of-Order)
    *     add x5*, x4, x6
    *     add x5*, x3, x2
    *
    * (3) WAR(write after read) (Out-Of-Order)
    *     add x5, x4*, x6
    *     add x4*, x3, x2
    */
  val wb_rd_addr = ew_reg.inst(11, 7)
  val rs1hazard = wb_en && rs1_addr.orR && (rs1_addr === wb_rd_addr)
  val rs2hazard = wb_en && rs2_addr.orR && (rs2_addr === wb_rd_addr)
  val rs1 = Mux(wb_sel === WB_ALU && rs1hazard, ew_reg.alu, regFile.io.rdata1)
  val rs2 = Mux(wb_sel === WB_ALU && rs2hazard, ew_reg.alu, regFile.io.rdata2)
  // e.g. load + add https://www.cnblogs.com/houhaibushihai/p/9737442.html

  // ALU operations
  alu.io.A := Mux(io.ctrl.A_sel === A_RS1, rs1, fe_reg.pc)
  alu.io.B := Mux(io.ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  alu.io.alu_op := io.ctrl.alu_op

  // Branch condition calc
  brCond.io.rs1 := rs1
  brCond.io.rs2 := rs2
  brCond.io.br_type := io.ctrl.br_type

  // D$ access
  val daddr = Mux(stall, ew_reg.alu, alu.io.sum) >> 2.U << 2.U
  val woffset = (alu.io.sum(1) << 4.U).asUInt | (alu.io.sum(0) << 3.U).asUInt
  io.dcache.req.valid := !stall && (io.ctrl.st_type.orR || io.ctrl.ld_type.orR)
  io.dcache.req.bits.addr := daddr
  io.dcache.req.bits.data := rs2 << woffset
  io.dcache.req.bits.mask := MuxLookup(
    Mux(stall, st_type, io.ctrl.st_type),
    "b0000".U,
    Seq(ST_SW -> "b1111".U, ST_SH -> ("b11".U << alu.io.sum(1, 0)), ST_SB -> ("b1".U << alu.io.sum(1, 0)))
  )
  io.dcache.direct_en := false.B

  // Pipelining
  when(reset.asBool || !stall && csr.io.expt) {
    st_type := 0.U
    ld_type := 0.U
    wb_en := false.B
    csr_cmd := 0.U
    illegal := false.B
    pc_check := false.B
  }.elsewhen(!stall && !csr.io.expt) {
    ew_reg.pc := fe_reg.pc
    ew_reg.inst := fe_reg.inst
    ew_reg.alu := alu.io.out
    ew_reg.csr_in := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    st_type := io.ctrl.st_type
    ld_type := io.ctrl.ld_type
    wb_sel := io.ctrl.wb_sel
    wb_en := io.ctrl.wb_en
    csr_cmd := io.ctrl.csr_cmd
    illegal := io.ctrl.illegal
    pc_check := io.ctrl.pc_sel === PC_ALU
  }

  // Load
  // 对load取出的数据进行修改(位扩展)
  val loffset = (ew_reg.alu(1) << 4.U).asUInt | (ew_reg.alu(0) << 3.U).asUInt
  val lshift = io.dcache.resp.bits.data >> loffset
  val load = MuxLookup(
    ld_type,
    io.dcache.resp.bits.data.zext,
    Seq(
      LD_LH -> lshift(15, 0).asSInt,
      LD_LB -> lshift(7, 0).asSInt,
      LD_LHU -> lshift(15, 0).zext,
      LD_LBU -> lshift(7, 0).zext
    )
  )

  // CSR access
  csr.io.stall := stall
  csr.io.in := ew_reg.csr_in
  csr.io.cmd := csr_cmd
  csr.io.inst := ew_reg.inst
  csr.io.pc := ew_reg.pc
  csr.io.addr := ew_reg.alu
  csr.io.illegal := illegal
  csr.io.pc_check := pc_check
  csr.io.ld_type := ld_type
  csr.io.st_type := st_type
  io.host <> csr.io.host

  /**
    *  *** Write Back ***
    */
  // Regfile Write
  val regWrite =
    MuxLookup(
      wb_sel,
      ew_reg.alu.zext,
      Seq(WB_MEM -> load, WB_PC4 -> (ew_reg.pc + 4.U).zext, WB_CSR -> csr.io.out.zext)
    ).asUInt

  regFile.io.wen := wb_en && !stall && !csr.io.expt
  regFile.io.waddr := wb_rd_addr
  regFile.io.wdata := regWrite


  io.debug.wb_pc := ew_reg.pc
  io.debug.wb_rf_wen := wb_en.asUInt
  io.debug.wb_rf_wnum := wb_rd_addr
  io.debug.wb_rf_wdata := regWrite

  // Abort store when there's an exception
  io.dcache.abort := csr.io.expt

  // TODO: re-enable through AOP
//  if (p(Trace)) {
//    printf(
//      "PC: %x, INST: %x, REG[%d] <- %x\n",
//      ew_reg.pc,
//      ew_reg.inst,
//      Mux(regFile.io.wen, wb_rd_addr, 0.U),
//      Mux(regFile.io.wen, regFile.io.wdata, 0.U)
//    )
//  }
}
