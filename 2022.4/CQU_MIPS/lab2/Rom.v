/*
 * @Author: LingZichao
 * @Date: 2022-04-22 06:56:45
 * @LastEditors: LingZichao
 * @LastEditTime: 2022-04-22 07:54:59
 * @FilePath: /SuzuranOC/2022.4/CQU_MIPS/lab2/Rom.v
 * @Description: file based-rom
 * 
 * Copyright (c) 2022 by LingZichao@bupt, All Rights Reserved. 
 */
`timescale 1ns / 1ps

module Rom(
    input clk,
    output [7:0] ans
    );
    reg [7 : 0] InsData[16 : 0];
    reg [4 : 0] cnt = 5'b00000;
    integer i;
initial begin
    $readmemb("data.txt" , InsData);
    // for (i = 0; i < 16 ; i++ ) begin
    //     $display("Data : %h" , InsData[i]);
    // end
end

    always @(posedge clk ) begin
        cnt <= cnt + 1;
    end

    assign ans = InsData[cnt];  
endmodule
