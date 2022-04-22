`timescale 1ns/1ps
module tb;

reg clk;
wire [7 : 0] ans;

Rom test(.clk(clk) , .ans(ans) );

always begin
	#10 clk = ~clk;
end

initial begin
	clk = 0;
	$dumpfile("wave.vcd");
	$dumpvars;
	$monitor("time = %g , ans = %h" , $time , ans);
	#200 $finish;
end

endmodule