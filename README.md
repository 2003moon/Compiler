## Compiler for PL241
The project is from a CS241 of UC Irvine. The compiler is used to compile programs written by PL241.

## Implementation
### First step
Build a recursive-descent parser that parses the token and generates SSA form, CFG and dominator tree. The algorithm to build the SSA form is described in [1]. The CFG and Dominator tree are visualized using a VCG(Visualization of Compiler Graph) tool.
### Second step
Do common subexpression elimination and copy propagation.
### Third Step
Build interference graph and implement global register allocation via colring the graph. The liveness analysis uses the algorithm describes in p429 of [2]. This step assumes that the target machine has 8 general-purpose data registers. The values that cannot be accommodated onto the data registers would be marked "Spilled". 
### Fourth step
Implement a code generator for the DLX processor. 

## Problems needed to be solved
1. The algorithm for removing phi functions needs to be modified.
2. There should be a mechinism to de-allocate the registers.
3. This compiler does not support function call.
4. The operation on arrays is incorrect.

## Reference
[1] M. M. Brandis and H. Mössenböck; "Single-Pass Generation of Static Single-Assignment Form for Structured Languages"; ACM Transactions on Programming Languages and Systems, 16:6, 1684-1698; 1994.
[2] Andrew w. Appel and Jens Palsberg; Moder Compiler Implementation In Java, 2nd edition; Cambridge University Press.





