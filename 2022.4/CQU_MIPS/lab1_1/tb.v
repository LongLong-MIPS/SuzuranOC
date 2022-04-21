`timescale 1ps/1ps
module tb;

reg clk;
reg rst;
reg [2:0] op;
reg [7:0] num1;
wire [7:0] ans;
wire [6:0] seg;

top test(.clk(clk) , .ans(ans) , .num1(num1) ,
			.op(op) , .seg(seg) , .rst(rst) );
initial begin
	$dumpfile("wave.vcd");
	$dumpvars;
	$monitor("simutime = %g , ans = %d" ,  $time , ans);
end

initial begin
	clk = 0; rst = 1;
	num1[7:0] = 8'h02; op[2:0] = 3'b001;
	#1 rst = 0;
	#100 ; 
	$finish;
end

always begin
	#5 clk = ~clk;
end

endmodule