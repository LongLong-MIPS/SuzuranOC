// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._

object Control {
  val Y = true.B
  val N = false.B

  // pc_sel
  // 决定下一条指令的方式
  val PC_4 = 0.U(2.W)//pc+1
  val PC_ALU = 1.U(2.W)//pc=alu
  val PC_0 = 2.U(2.W)//pc=pc
  val PC_EPC = 3.U(2.W)

  // A_sel
  // ALU A端口的数据来源
  val A_XXX = 0.U(1.W)//没有来源
  val A_PC = 0.U(1.W)//pc
  val A_RS1 = 1.U(1.W)//RS1寄存器

  // B_sel
  // ALU B端口的数据来源
  val B_XXX = 0.U(1.W)
  val B_IMM = 0.U(1.W)//立即数
  val B_RS2 = 1.U(1.W)//RS2寄存器

  // imm_sel
  // val IMM_X = 0.U(3.W)
  // val IMM_I = 1.U(3.W)//符号扩展的12位立即数
  // val IMM_S = 2.U(3.W)//imm[31:25]和 imm[11:7]构成的一个12位的立即数，在执行指令运算时需要把12 位立即数扩展到 32 位
  // val IMM_U = 3.U(3.W)//20位无符号数
  // val IMM_J = 4.U(3.W)//J指令的20位立即数顺序为：{20，10:1，11，19:12}
  // val IMM_B = 5.U(3.W)//12位立即数的顺序：{12，10:5}，{4:1，11}
  // val IMM_Z = 6.U(3.W)
  val IMM_X = 0.U(3.W)//不构造立即数
  val IMM_I = 1.U(3.W)//有符号扩展至 32 位的立即数
  val IMM_U = 2.U(3.W)//零扩展至 32 位的立即数
  val IMM_H = 3.U(3.W)//16位高位为该16位立即数 16位低位填零
  val IMM_S = 4.U(3.W)//用于移位指令中的SA立即数
  val IMM_L = 5.U(3.W)//左移 2 位并进行有符号扩展
  val IMM_J = 6.U(3.W)//26位立即数左移两位 留作转移地址的处理
  // br_type
  val BR_XXX = 0.U(3.W)//不执行跳转
  val BR_LTU = 1.U(3.W)//无符号数小于时发生跳转
  val BR_LT = 2.U(3.W)//有符号数小于时发生跳转
  val BR_EQ = 3.U(3.W)//相等发生跳转
  val BR_GEU = 4.U(3.W)//无符号数大于等于时发生跳转
  val BR_GE = 5.U(3.W)//有符号数大于等于时发生跳转
  val BR_NE = 6.U(3.W)//不相等发生跳转

  // st_type
  val ST_XXX = 0.U(2.W)//不执行写入内存
  val ST_SW = 1.U(2.W)//将32位数值写入内存
  val ST_SH = 2.U(2.W)//将16位数值写入内存
  val ST_SB = 3.U(2.W)//将8位数值写入内存

  // ld_type
  val LD_XXX = 0.U(3.W)//不执行读取内存
  val LD_LW = 1.U(3.W)//读取内存中32位数值保存到rd中
  val LD_LH = 2.U(3.W)//读取内存中16位数值,进行符号扩展到32位,再保存到rd中
  val LD_LB = 3.U(3.W)//读取内存中8位数值,进行符号扩展到32位,再保存到rd中
  val LD_LHU = 4.U(3.W)//读取内存中16位数值,进行零扩展到32位,再保存到rd中
  val LD_LBU = 5.U(3.W)//读取内存中8位数值,进行零扩展到32位,再保存到rd中

  // wb_sel 寄存器组写入信号
  // 选择写入对应寄存器的数据来源
  val WB_ALU = 0.U(2.W)//从ALU进行写回
  val WB_MEM = 1.U(2.W)
  val WB_PC4 = 2.U(2.W)//从pc+4进行写回
  val WB_CSR = 3.U(2.W)

  import Alu._
  import Instructions._

  // format: off
  val default =
  //                                                    kill                        wb_en  illegal?
  //  pc_sel  A_sel   B_sel  imm_sel   alu_op   br_type  |  st_type ld_type wb_sel  | csr_cmd |
  //    |       |       |     |          |          |    |     |       |       |    |  |      |
  List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, Y)
  val map = Array(
  //算术运算指令
  ADD   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs1+rs2
  ADDI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs+imm
  ADDU  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs1+rs2 无溢出异常
  ADDIU -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs+imm 无溢出异常
  SUB   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SUB   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs1-rs2
  SUBU  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SUB   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs1-rs2 无溢出异常
  SLT   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLT   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//如果rs1<rs2,rd=1否则rd=0(有符号数)
  SLTI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLT   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//将数值1放到寄存器rd中，如果寄存器rs1小于符号扩展的立即数,否则将0写入rd
  SLTU  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLTU  , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//如果rs1<rs2,rd=1否则rd=0(无符号数)
  SLTIU -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLTU  , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//与上两条类似，但比较的是无符号数
  DIV//有符号除法
  DIVU//无符号除法
  MULT//有符号乘法
  MULTU//无符号乘法
  //逻辑运算指令
  AND   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_AND   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rs=rs1&rs2
  ANDI  -> List(PC_4  , A_RS1,  B_IMM, IMM_U, ALU_AND   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs&imm
  LUI   -> List(PC_4  , A_PC,   B_IMM, IMM_H, ALU_COPY_B, BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//构建32位常数,立即数放到目标寄存器rd的高16位，将rd的低16位填0。
  NOR   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_NOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rs=rs1 NOR rs2
  OR    -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_OR    , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs1|rs2
  ORI   -> List(PC_4  , A_RS1,  B_IMM, IMM_U, ALU_OR    , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs|imm
  XOR   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_XOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs1^rs2
  XORI  -> List(PC_4  , A_RS1,  B_IMM, IMM_U, ALU_XOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//rd=rs^imm
  //移位指令
  SLLV  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//逻辑左移，操作数是rs1，移位次数时rs2
  SLL   -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_SLL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//通过立即数对rs进行逻辑左移
  SRAV  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SRA   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//算术右移，操作数是rs1，移位次数时rs2
  SRA   -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_SRA   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//通过立即数对rs进行算术右移
  SRLV  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SRL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//逻辑右移，操作数是rs1，移位次数时rs2
  SRL   -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_SRL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),//通过立即数对rs进行逻辑左移
  //分支跳转指令
  BEQ   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_EQ , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N),//rs1=rs2跳转
  BNE   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_NE , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N),//rs1！=rs2跳转
  BGEZ
  BGTZ
  BLEZ
  BLTZ
  BGEZAL
  BLTZAL
  J
  JAL
  JR
  JALR
  //数据移动指令
  MFHI
  MFLO
  MTHI
  MTLO
  //自陷指令
  BREAK -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N),
  SYSCALL -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N),
  //访存指令
  LB -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LB , WB_MEM, Y, CSR.N, N),//从存储器中读取一个8位数值,进行符号扩展到32位,再保存到rd中
  LBU -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LBU, WB_MEM, Y, CSR.N, N),//从存储器中读取一个8位数值,进行零扩展到32位,再保存到rd中
  LH -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LH , WB_MEM, Y, CSR.N, N),//从存储器中读取一个16位数值,进行符号扩展到32位,再保存到rd中
  LHU -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LHU, WB_MEM, Y, CSR.N, N),//从存储器中读取一个16位数值,进行零扩展到32位,再保存到rd中
  LW  -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LW , WB_MEM, Y, CSR.N, N),//从存储器中读取一个32位数值保存到rd中
  SB  -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SB , LD_XXX, WB_ALU, N, CSR.N, N),//从rs2取8位数值，保存到存储器中
  SH  -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SH , LD_XXX, WB_ALU, N, CSR.N, N),//从rs2取16位数值，保存到存储器中
  SW  -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SW , LD_XXX, WB_ALU, N, CSR.N, N),//从rs2取32位数值，保存到存储器中
  //特权指令
  ERET  -> List(PC_EPC, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N),
  MFC0
  MTC0
  )
  // format: on
}

class ControlSignals extends Bundle {
  // 输出IR中的指令
  val inst = Input(UInt(32.W))


  // 硬布线,输出信号
  val pc_sel = Output(UInt(2.W))
  val inst_kill = Output(Bool())
  val A_sel = Output(UInt(1.W))
  val B_sel = Output(UInt(1.W))
  val imm_sel = Output(UInt(3.W))
  val alu_op = Output(UInt(4.W))
  val br_type = Output(UInt(3.W))
  val st_type = Output(UInt(2.W))
  val ld_type = Output(UInt(3.W))
  val wb_sel = Output(UInt(2.W))
  val wb_en = Output(Bool())
  val csr_cmd = Output(UInt(3.W))
  val illegal = Output(Bool())
}

class Control extends Module {
  val io = IO(new ControlSignals)
  val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

  // Control signals for Fetch
  io.pc_sel := ctrlSignals(0)
  io.inst_kill := ctrlSignals(6).asBool

  // Control signals for Execute
  io.A_sel := ctrlSignals(1)
  io.B_sel := ctrlSignals(2)
  io.imm_sel := ctrlSignals(3)
  io.alu_op := ctrlSignals(4)
  io.br_type := ctrlSignals(5)
  io.st_type := ctrlSignals(7)

  // Control signals for Write Back
  io.ld_type := ctrlSignals(8)
  io.wb_sel := ctrlSignals(9)
  io.wb_en := ctrlSignals(10).asBool
  io.csr_cmd := ctrlSignals(11)
  io.illegal := ctrlSignals(12)
}
