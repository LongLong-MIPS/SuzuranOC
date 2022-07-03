# RISCV-MINI 速览
___

**RISCV-MINI**是使用`chisel3`编写的三级流水线`RISC-V`处理器核，同时包括了对`AXI4`和`Cache`的支持。诚然我们的目标是实现较为陈旧的`MIPS`ISA，
但是他们都属于RISC设计，很多部分都是相通的，可以快速迁移

在根目录下的`readme.md`文件中可以看到原版项目的介绍。本文档主要目的为结合`RISCV`真实指令的执行流程，快速对该项目有大致的了解

需要提醒的是，阅读硬件描述语言(HDL)的代码**一定不能**按照软件代码的方式逐条分析，要结合数字电路考虑到并行

## 主要源码概览
```
├─main
│  ├─cc
│  │      mm.cc
│  │      mm.h
│  │      top.cc
│  │
│  └─scala
│      ├─junctions
│      │      nasti.scala     // AXI4总线
│      │
│      └─mini
│              Alu.scala       // ALU实现
│              BrCond.scala    // 跳转控制器实现
│              Cache.scala     // Cache实现
│              Config.scala    // 相关参数配置
│              Control.scala   // 流水线控制器
│              Core.scala      // CPU核的顶层模块
│              CSR.scala       // 条件状态寄存器的实现
│              Datapath.scala  // 数据通路实现
│              ImmGen.scala    // 立即数重组
│              Instructions.scala // 指令译码辅助
│              Main.scala      // 程序入口
│              RegFile.scala   // 寄存器组实现
│              Tile.scala      // 包含AXI Cache的总片
│
└─test  // 各模块相关的测试集,测试数据参见./tests
    └─scala
        └─mini
                ALUTests.scala
                BrCondTests.scala
                CacheTests.scala
                CoreTests.scala
                CSRTests.scala
                DatapathTests.scala
                ImmGenTests.scala
                IntegrationTest.scala
                Opcode.scala
                TestConfig.scala
                TestUtils.scala
                TileTester.scala
```
## 从一条指令开始

首先我们来看RISC-V的一条基本指令`ADD`
```
add rd, rs1, rs2  
```
表示的含义为`x[rd] = x[rs1] + x[rs2]`
把寄存器 `x[rs2]`加到寄存器 `x[rs1]`上，结果写入 `x[rd]`。忽略算术溢出。

该条指令为`R`型指令，指令共有六个部分，具体的格式如下所示

|  func7  |   rs2   |   rs1   |  func3  |   rd   | opcode  |
|:-------:|:-------:|:-------:|:-------:|:------:|:-------:|
| [31:25] | [24:20] | [19:15] | [14:12] | [11:7] |  [6:0]  |
| 0000000 |    /    |    /    |   000   |   /    | 0110011 |

留空的三个位置为寄存器号。因为RISC-V共有32个通用寄存器,因此每个寄存器表示位有5位

为了更好地理解项目的主要脉络，我们首先不考虑`AXI4`,仅仅对Cpu内核部分进行梳理，
同时我们假设该条指令已经加载到内存中了

### 1. Fetch
`datapath.scala`开始,`Line.86` 给出了PC寄存器的有关实现。`pc`为`0x200-0x4`,注意`icache`是和`next
_pc`信号相连接的
`next_pc`会根据`IndexedSeq`里的条件进行选择，如果`->`前面的信号为高电平(1)，则改变为对应的信号。默认为`PC+4`

接下来将`nextpc`信号送入icache中。将`nextpc`信号与`icache`的`request`通道的`addr`接口连接，把指令地址`0x200`送入`icache`，
那么在下一个时钟信号上升沿 ，可以从`response`通道得到该条指令

### 2. Execute
在这一部分，一条通路将取出的指令通往控制器`controller`,在`control.scala`文件中可以看到相应信号的实现
尤其注意`Line.76`中通过List便捷表示信号的编写方式。由于我们是ADD指令,查表得到如下信号组：   
```
ADD   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N),
```
表示的意义为：

|  Signal  |          De.          |
|:--------:|:---------------------:|
|   PC_4   |      下一条指令位于PC+4      |
|  A_RS1   | ALU A端口的数据来自寄存器组出口RS1 |
|  B_RS2   | ALU A端口的数据来自寄存器组出口RS2 |
|  IMM_X   |         无立即数          |
| ALU_ADD  |       ALU模式为加法        | 
|  BR_XXX  |         不执行跳转         |
|    N     |       不插入NOP指令        |
|  ST_XXX  |        不执行写入内存        |
|  LD_XXX  |        不执行读取内存        |
|  WB_ALU  | 执行写回寄存器组入口的来源为ALU的输出  |
|    Y     |       允许寄存器组写入        |
| CSR.N, N |         CSR相关         |

接下来进行ALU的运算工作

### 3. Write Back
将 ALU 结果写回到 Regfile

### 4. ImmGen
为什么需要立即数生成器呢？请看RISCV中BEQ这条指令

    beq rs1, rs2, offset   

相等时分支跳转 (Branch if Equal). 
若寄存器 x[rs1]和寄存器 x[rs2]的值相等，把 pc 的值设为当前值加上符号位扩展的偏移 offset。

| imm[12/10:5] |   rs2   |   rs1   |  func3  | imm[4:1/11] | opcode  |
|:------------:|:-------:|:-------:|:-------:|:-----------:|:-------:|
|   [31:25]    | [24:20] | [19:15] | [14:12] |   [11:7]    |  [6:0]  |
|      /       |    /    |    /    |   000   |      /      | 1100011 |

注意到该offset偏移量：切分成第12位，第11位，第10-5位，第4-1位四个部分分散在指令中，
因此需要重新组合成原来的offset


## 数据通路是灵光一现吗？

在doc文件夹下还有一份重庆大学的教案pdf，展示了数据通路是如何制作的，非常值得一读


## 测试与仿真

请查看 `test/scala/mini/CoreTests` ， 使用`sbt`的相关指令运行测试