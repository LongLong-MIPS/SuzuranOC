module adder_4bits_2steps ( cin_a , cin_b , cin , clk , cout , sum ) ;
input [7:0] cin_a ;
input [7:0] cin_b ;
input cin ;
input clk ;
output cout ;
output [7:0] sum ;

reg cout ;
reg cout_temp ;
reg [7:0] sum ;
reg [3:0] sum_temp ;

always @( posedge clk ) begin
{ cout_temp , sum_temp } <= cin_a [3:0] + cin_b [3:0] + cin ;
end
// 并行计算
always @( posedge clk ) begin
{ cout , sum } <= { { 1'b0 , cin_a [7:4] }  + { 1'b0 , cin_b [7:4] } + cout_temp , sum_temp };
end

endmodule