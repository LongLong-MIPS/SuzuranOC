<!--
 * @Author: LingZichao
 * @Date: 2022-04-21 08:37:17
 * @LastEditors: LingZichao
 * @LastEditTime: 2022-04-23 08:51:53
 * @FilePath: /SuzuranOC/2022.4/CQU_MIPS/log.md
 * @Description: 
 * 
 * Copyright (c) 2022 by LingZichao@bupt, All Rights Reserved. 
-->
一些注意点如下:
lab1 : 
* 在8bit pipeline全加器中可以看出pipline与concurrenc的一些联系
* always与组合逻辑时序逻辑的运用.
* 在FPGA真实仿真中,reg的消耗的资源会略大

lab2 :
* `$readmemb / $readmemh`是以字符的形式读入数据的，这一点实在是有点异类
* 另外,区分一个数的方式是使用空字符，~~这实在是humanreadable过分了~~

	经过试验，M大于数据位宽，数据可以正常读取，高位补0；小于数据位宽，数据无法正常读取。(https://zhuanlan.zhihu.com/p/158474602)
	
	The content read only includes: blank spaces (spaces, line feeds, tabs and form-feeds), comment lines, binary or hexadecimal numbers.

	http://www.referencedesigner.com/tutorials/verilog

* 遇到了一个`Ports cannot be unpacked arrays` , verilog 中压缩数组和解压缩数组的区别参考下面
	http://www.asic-world.com/systemverilog/data_types10.html
* 善于使用{}运算符简化程序,避免大量IF-ELSE