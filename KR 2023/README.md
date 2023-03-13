# MHS-MXP: ABox Abduction Solver 

MHS-MXP (combination of MHS and MergeXplain) is complete algorithm for solving abduction problem.

## Folder contents
...

## Running the evaluation
We created 4 scripts to execute the evaluation:
* **eval_1_mhs-mxp.sh** runs the evaluation using the MHS-MXP algorithm; atomic and negated atomic concept assertions are allowed in explanations
* **eval_1_mhs-mxp_noNeg.sh** runs the evaluation using the MHS-MXP algorithm; only atomic concept assertions are allowed in explanations
* **eval_1_mhs.sh** runs the evaluation using the MHS algorithm; atomic and negated atomic concept assertions are allowed in explanations
* **eval_1_mhs_noNeg.sh** runs the evaluation using the MHS algorithm; only atomic concept assertions are allowed in explanations

Each script runs every input 5 times, so we can acquire the average result for a given input. 

## How to run MHS-MXP
You can run MHS-MXP through the command line (allocation of more memory for Java is recommended), for example:

**java -Xmx4096m -jar mhs-mxp.jar eval_1/mhs/lubm-0_1_0.in**, where **eval_1/mhs/lubm-0_1_0.in** is a relative path to the input file

## Input
MHS-MXP receives a structured input file as a parameter. The input file contains one switch per line. Mandatory switches are -f and -o, other switches are optional.

This version of the solver has the following switches (for clarity, we list only the switches relevant to the performed evaluation):
* **-f: - \<string\>**   a relative path to the ontology file (complete ontology in any ontology syntax) which is used as the knowledge base
* **-o: - ...**
* *-t: - \<positive integer\>*   the limit of algorithm runtime in the seconds (timeout after which the algorithm terminates; not set by default)
* *-d: - \<positive integer\>*   maximal depth of the HS-Tree (not set by default) 
* *-n: - \<boolean\>*   allowing negated assertions in explanations (default is *true*)
* *-r: - ...* 
* *-mhs: - \<boolean\>*   using the plain MHS algorithm (default is *false*)

## Output
...
