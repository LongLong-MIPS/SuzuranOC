<!--
 * @Author: LingZichao
 * @Date: 2022-04-21 02:33:04
 * @LastEditors: LingZichao
 * @LastEditTime: 2022-04-23 08:45:01
 * @FilePath: /SuzuranOC/README.md
 * @Description: 
 * 
 * Copyright (c) 2022 by LingZichao@bupt, All Rights Reserved. 
-->
# SuzuranOC

__铃兰小姐是我们的光!__

这个简单轻松的resp就记录一下,我是如何克服 ~~Synax error~~ 的吧

---
### day2
今天主要是阅读了控制器和数据通路的部分，首先夸一夸CQU的Tutorial,真的是Hands-on!

在以前我不是很能理解使能信号在电路中的作用，在这个系统级别设计中我确实改变了看法。~~要全局的，不要片面的~~

系统中这些特别设计信号量的模型，似乎是某鳄鱼洗澡游戏，需要控制水阀(MUX)的开关，使水流(Data Path)导向正确的方向。

总之，单周期的数据通路和设计思路差不多理清了，但是如何和时序联系起来还得下回分晓。

~~**啊，要多想~~

### day1
OK 稍微搞懂了一些`testbench`常用的函数，一方面感觉不少语法和C真的很像，另一方面为什么`Verilog`没有像`C语言`一样丰富的手册呢(当然感谢知乎上的大佬) ，~~令人感叹~~。基于这些`verilog`的`system call` ， 使用外部文件算是模拟了`DRAM/ROM`存储器了。

指令系统部分大概看了一下MIPS和CQU提供的手册，感觉和RISC-V类似，但是译码阶段和看过的tinyriscv中疯狂IF-ELSE的方式有点区别，这点可能得去看一下课程视频

CSAPP , YYDS!

### day0 
开始搞`testbench`,并且意识到寒假刷的`vlab`已经全部遗忘了（现在被窝里全是C++）.

主要是配环境:

|||
|---|---|
|虚拟机系统|Ubuntu20.04|
|仿真器|Iverilog|
|编辑器|VSCode|
|波形查看|WaveTrace|

(<-这玩意pro要$15...第三世界真的不配了)

然后就是第一次跑出来`wave`——CQU `lab1.1`的ALU,算是填了寒假的一个坑.

总之，今天主要是一些准备和热身工作。
