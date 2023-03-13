# MHS-MXP: ABox Abduction Solver 

MHS-MXP (combination of MHS and MergeXplain) is a complete algorithm for solving abduction problem.

## Folder contents
* executable file of our solver *mhs-mxp.jar*
* scripts used for evaluation execution *eval_1_mhs-mxp.sh*, *eval_1_mhs-mxp_noNeg.sh*, *eval_1_mhs.sh*, *eval_1_mhs_noNeg.sh*
* evaluation inputs in *eval_1*
* examples of solver run outputs in *logs_basic/JFACT/eval_1/lubm-0* and *logs_mhs/JFACT/eval_1/lubm-0*

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
MHS-MXP receives a structured input file as a parameter. The input file contains one switch per line. Mandatory switches are **-f** and **-o**, other switches are optional.

This version of the solver has the following switches (for clarity, we list only the switches relevant to the performed evaluation):
* **-f: - \<string\>**   a relative path to the ontology file (complete ontology in any ontology syntax) which is used as the knowledge base
* **-o: - \<ontology\>** observation in a ontology format (complete ontology in any ontology syntax; has to be in one line)
* *-t: - \<positive integer\>*   the limit of algorithm runtime in the seconds (timeout after which the algorithm terminates; not set by default)
* *-d: - \<positive integer\>*   maximal depth of the HS-Tree (not set by default) 
* *-n: - \<boolean\>*   allowing negated assertions in explanations (default is *true*)
* *-mhs: - \<boolean\>*   using the plain MHS algorithm (default is *false*)

## Output
As an output for a given input, the solver produces several log files. Time in the logs is given in seconds.

MHS-MXP log types:
#### Hybrid log
*\<time\>__\<input file name\>__hybrid.log*

* final log which contains desirable explanations of a certain length in each line (except the last)
  * line form: *\<length n\>;\<number of explanations\>;\<level completion time\>; {\<found explanations of the length n\>}*
* the last line contains the total running time ??? TODO

#### Explanation times log
*\<time\>__\<input file name\>__hybrid_explanation_times.log*

* final log which contains desirable explanations and time when they were found
  * line form: *\<time t\>;\<explanation found in the time t\>*

#### Level log
*\<time\>__\<input file name\>__hybrid_level.log*

* final log which contains desirable explanations founded in a certain level (except the last)
  * line form: *\<level l\>;\<number of explanations\>;\<level l completion time\>; {\<explanations found in the level l\>}*
* the last line contains the total running time ??? TODO

#### Partial explanations log
*\<time\>__\<input file name\>__hybrid_partial_explanations.log*

TODO

#### Partial level explanations log
*\<time\>__\<input file name\>__hybrid_partial_level_explanations.log*

TODO

MHS log types are a subset of MHS-MXP types: **hybrid log**, **explanation times log** and **partial explanations log**.

Logs of inputs that used MHS have a different location than logs of inputs that used the MHS-MXP algorithm.
* location of MHS-MXP logs: *logs_basic/JFACT/eval_1/lubm-0*
* location of MHS logs: *logs_mhs/JFACT/eval_1/lubm-0*

TODO a few examples are located there 
