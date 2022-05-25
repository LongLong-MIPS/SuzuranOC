
# SuzuranOC
![avatar](https://badgen.net/badge/Language/Verilog/orange)
![avatar](https://badgen.net/badge/State/Preparing/blue)

__铃兰小姐是我们的光!__


---

这个`README.md`主要是进行一些项目的介绍和记录富有价值的学习资料。嘛，众人拾柴火焰高，围绕“体系结构设计”这一主题多方面的搜寻有关资料,并共享到这份分档就完事了（

---

## 零. 写在前面

1. 主要是三个重要的方法吧，我觉得对我影响蛮大的
`STFW`/`RTFSC`/`RTFM` : [链接，点我](https://nju-projectn.github.io/ics-pa-gitbook/ics2021/#%E5%A6%82%E4%BD%95%E8%8E%B7%E5%BE%97%E5%B8%AE%E5%8A%A9) -> 既是学习也是提高，懂得如何获取有效知识真的蛮重要

2. 另外，`龙芯杯`官方给的说法是“鼓励参考开源项目”，因此我们是可以大量学习借鉴前辈们的作品的，但是落实到代码和设计阶段一定要**保证原创**。

3. 多多关注龙芯杯的[官方消息及线上培训](https://space.bilibili.com/1339327684)。

4. 关于团队分工啥的还是得进一步商榷、

## 一.关于语言方面的准备

首先需要学习的是基本的硬件描述语言（HDL）verilog:
1.  https://verilogoj.ustc.edu.cn/oj/ -> 中科大的VerilogOJ，基本上跟着走下来能掌握大部分语法

2. 另外这个项目中的`src`文件夹中有一个`Hello world`程序，具体食用的方式如下所示
（不过事先说明一下，这一套也不是很有必要......因为最终还是要使用`Vivado`或者`Quartus`这种EDA进行仿真与开发，各种体验完爆这种烂怂脚手架的，因此有条件的话直接上`Vivado`跳过这些=。=）：

	首先是仿真环境的配置:

	|||
	|---|---|
	|虚拟机系统|Ubuntu20.04|
	|仿真器|Iverilog|
	|编辑器|VSCode|
	|波形查看|WaveTrace|


	**Note 1:** `WaveTrace`是Vscode的一个插件，在插件商店直接搜索即可<-这玩意`pro`要$15...第三世界真的不配了)

	**Note 2:** `iverilog`也可以在`Windows`下食用，具体的安装方法~~Google~~`Baidu`一下即可。但是运行

	**Note 3:** `iverilog`的错误提示不怎么友好，而且年久失修，也推荐尝试一下`Verilator`

	配置好运行环境后，在`src`目录下通过下面命令可以食用`Hello World`
	* i. 生成二进制文件（姑且称之为编译）
	```verilog
	iverilog adder_tb.v adder.v
	```
	* ii. 运行二进制文件(Linux环境下的运行，Windows下应该有所出入)
	```verilog
	./a.out
	```
	这时已经可以看到控制台中输出了有关的监视信息

	* iii. 在`vscode`中打开`tb_adder.vcd`即可查看波形（也可以使用`GTKWave`）
	
	有关更多信息可以参阅 https://zhuanlan.zhihu.com/p/148795858 ，以及别忘记`STFW`. : )


3. 在学习`verilog`的基础上，我们也可以尝试一下其他高级语言如`SpitalHDL`或者`Chisel`，他们相较于~~腐朽的~~`verilog`更加现代，~~据说~~使用起来更加便捷，但是仍然存在学习成本，因此最终要不要采用该方案需要咱们进一步探讨。

## 二、体系结构的有关资料
1. 这里的`体系结构`更多指的是宏观上总体设计，涉及计算机的方方面面。首先需要提及的就是`计算机组成原理`这门课程中的所有基础知识。
   
2. ~~考研408也算一个~~。
   
3. 电子书籍 ： https://foxsen.github.io/archbase/ ->龙芯官方上马的体系结构数据，版本比较新，基本能覆盖我们项目大很多方面

4. 书籍 `《See MIPS Run Liunx》`: (暂无地址) -> 助教`@Name1e5s`友情推荐，大黑书，可读性未知

5. PA实验：[南京大学ICS实验PA](https://nju-projectn.github.io/ics-pa-gitbook/ics2021/) -> 南京大学面向大一同学的一系列计算机Lab，如果有空可以看一下，绝对的干货，如果一步一步跟着做的话耗费精力蛮大的。

6. 施工中......

## 三、CPU核心的设计

1. 这里的CPU核心设计主要指的是流水线设计、指令系统（RISC/MIPS）、分支预测器、ALU（乘法器除法器浮点运算）等等在内。另外，虽然叫做“核心”，但是这只是我们项目中的一部分，甚至不能
   
2. 书籍`《超标量流水线设计》` : (暂无地址) -> 小有名气的一本书，基本上能覆盖我们项目的很大一部分
   
3. `CQU`流水线设计：https://www.bilibili.com/video/BV1pK4y1C7es ->这个项目实现了一个~~小学二年级水平~~的多周期五级流水线CPU，并处理了三种冒险问题。这个难度就是我们暑期课设的要求。可`RTFSC`
   
4. `TINY-RISCV` ：https://gitee.com/liangkangnan/tinyriscv -> `RISC-V`的一个开源项目，能运行完整可执行程序。`RISC-V`与`MIPS`同属于`RISC`体系，从指令集的角度来看差异真不大，所以这方面的知识是可以通用的。

5. `蜂鸟e203` ：https://github.com/riscv-mcu/e203_hbirdv2 -> 商用级别的开源核，可以选择性查看
   
6. `BOOM` ：https://github.com/riscv-boom/riscv-boom -> 天花板级别的开源核，水平不够难以评价。

## 四、Cache的设计
1. Cache部分可以说是拉动性能分的主力了。
   
2. 施工中......

## 五、AXI总线及外设的设计
1. 施工中......

## 六、TLB虚拟内存方面的设计
1. 施工中......

## 七、启动Linux操作系统
1. 关于Linux内核简短介绍 ： https://github.com/sunym1993/flash-linux0.11-talk 

2. 施工中......