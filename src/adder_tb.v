
module tb_adder;
reg a , b ;
wire c;

adder target (	//声明待测模组并连接信号
	.a(a) ,
	.b(b) , 
	.c(c)
);

initial begin
	$display("Hello World");
	$dumpfile("tb_adder.vcd"); // 波形输出文件
	$dumpvars;	   // 捕获全部信号的层级
	$monitor("time = %g , a = %d , b = %d , c = %d" ,$time , a , b ,c);
end

initial begin
	a = 0 ; b = 0;
	#1 
	a = 0 ; b = 0;
	#1 
	a = 0 ; b = 1;
	#1 
	a = 1 ; b = 0;
	#1 
	a = 1 ; b = 1;
	$finish;
end

endmodule
