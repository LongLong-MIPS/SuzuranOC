# CPU AXI Table 

---

该文档用于测试环境中AXI有关接口的对照

|  BitWidth   |     Interface      |
|:-----------:|:------------------:|
| wire [3 :0] |      cpu_arid      |
| wire [31:0] |     cpu_araddr     |
| wire [3 :0] |     cpu_arlen      |
| wire [2 :0] |     cpu_arsize     |
| wire [1 :0] |    cpu_arburst     |
| wire [1 :0] |     cpu_arlock     |
| wire [3 :0] |    cpu_arcache     |
| wire [2 :0] |     cpu_arprot     |
|    wire     |    cpu_arvalid     |
|    wire     |    cpu_arready     |
|  ---------  |     ---------      |
| wire [3 :0] |      cpu_rid       |
| wire [31:0] |     cpu_rdata      |
| wire [1 :0] |     cpu_rresp      |
|    wire     |     cpu_rlast      |
|    wire     |     cpu_rvalid     |
|    wire     |     cpu_rready     |
|  ---------  |     ---------      |
| wire [3 :0] |      cpu_awid      |
| wire [31:0] |     cpu_awaddr     |
| wire [3 :0] |     cpu_awlen      |
| wire [2 :0] |     cpu_awsize     |
| wire [1 :0] |    cpu_awburst     |
| wire [1 :0] |     cpu_awlock     |
| wire [3 :0] |    cpu_awcache     |
| wire [2 :0] |     cpu_awprot     |
|    wire     |    cpu_awvalid     |
|    wire     |    cpu_awready     |
|  ---------  |     ---------      |
| wire [3 :0] |      cpu_wid       |
| wire [31:0] |     cpu_wdata      |
| wire [3 :0] |     cpu_wstrb      |
|    wire     |     cpu_wlast      |
|    wire     |     cpu_wvalid     |
|    wire     |     cpu_wready     |
|  ---------  |     ---------      |
| wire [3 :0] |      cpu_bid       |
| wire [1 :0] |     cpu_bresp      |
|    wire     |     cpu_bvalid     |
|    wire     |     cpu_bready     |
|    -----    |       -----        |
| wire [31:0] |    debug_wb_pc;    |
| wire [3 :0] |  debug_wb_rf_wen;  |
| wire [4 :0] | debug_wb_rf_wnum;  |
| wire [31:0] | debug_wb_rf_wdata; |