# Todo-list

有可能方向和未完成的部分

1. RISC-V -> MIPS(指令格式)
    仍未完成的部分：CP0

2. **MUL DIV**（比较重要）

   乘法器和除法器。或者更确切的说是运算器的优化->[B站视频一份](https://www.bilibili.com/video/BV1VE411o7nx?p=20)

3. ~~延迟槽技术~~

4. Pipeline lv3->lv5+
    
    五级流水是基本，更高级的流水线需要进一步搜集资料

5. 分支预测器的实现
6. CSR Inter
7. 89个测试点和记忆游戏

    经过简单测试已经可以通过80%。详细内容参见`Tests`文件夹。目前暂未配置停机指令，所以需要设置上线来限制停机

9. 为Cache配置LRU

10. 多发射

11. **MMU与TLB**（非常重要）

12. 原子操作和内存屏障

13. 针对连续写优化`Store Buffer`

远期规划：
1. 
2. 浮点数运算器FPU
3. 丰富外设支持
4. 针对性能测试设计加速电路
5. 多级Cache