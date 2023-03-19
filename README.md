# MHS-MXP: ABox Abduction Solver

MHS-MXP (combination of MHS and MergeXplain) is a complete algorithm for solving abduction problem.

**READ ME is not up to date, currently in progress....**

## How to run MHS-MXP
You can run MHS-MXP through the command line (allocation of more memory for Java is recommended), for example:

**java -Xmx4096m -jar mhs-mxp.jar in/testExtractingModels/pokus9.in**, where **in/testExtractingModels/pokus9.in** is a relative path to the input file

## Input
MHS-MXP receives a structured input file as a parameter. The input file contains one switch per line. Mandatory switches are **-f** and **-o**, other switches are optional.
...

## Output
As an output for a given input, the solver produces several log files. Time in the logs is given in seconds.

MHS-MXP log types:

**Hybrid log**
*\<time\>__\<input file name\>__hybrid.log*

* final log which contains desired explanations of a certain length in each line (except the last)
  * line form: *\<length n\>;\<number of explanations\>;\<level completion time\>; {\<found explanations of the length n\>}*
* the last line contains the total running time

**Explanation times log**
*\<time\>__\<input file name\>__hybrid_explanation_times.log*

* final log which contains desired explanations and time when they were found
  * line form: *\<time t\>;\<explanation found in the time t\>*

**Level log**
*\<time\>__\<input file name\>__hybrid_level.log*

* final log which contains desired explanations founded in a certain level in each line (except the last)
  * line form: *\<level l\>;\<number of explanations\>;\<level l completion time\>; {\<explanations found in the level l\>}*
* the last line contains the total running time

**Partial explanations log**
*\<time\>__\<input file name\>__hybrid_partial_explanations.log*

* partial log with the same structure as **hybrid log**
* may contain also undesired explonations 

**Partial level explanations log**
*\<time\>__\<input file name\>__hybrid_partial_level_explanations.log*

* partial log with the same structure as **level log**
* may contain also undesired explonations 

MHS log types are a subset of MHS-MXP types: **hybrid log**, **explanation times log** and **partial explanations log**. Other types would be redundant because the division of explanations according to the length and the levels is identical for MHS.

Logs of inputs that used MHS have a different location than logs of inputs that used the MHS-MXP algorithm.
* location of MHS-MXP logs: *logs_basic/JFACT/eval_1/lubm-0*
* location of MHS logs: *logs_mhs/JFACT/eval_1/lubm-0*

If we launch the algorithm, we have to create the input file with defined structure. It contains one switch in each line. Valid input file requires two switches -f and -o, other switches are optional. The following switches are allowed:
* -f: - string, path to the file with ontology (complete ontology in any ontology syntax) which is used as the knowledge base
* -o: - ontology, observation in the ontology format (complete ontology in any ontology syntax in one line)
* -t: - positive integer, the algorithm run in the seconds (timeout after which the algorithm terminates; it is not set by default)
* -d: - positive integer, maximal depth of the HS-Tree (it is not set by default) 
* -n: - boolean, negated assertions in the abducibles (default is true)
* -r: - pellet/hermit/jfact, used reasoner, which corresponds to one of the existed reasoners Pellet, HermiT or jFact, respectivelly (default is hermit)
* -mhs: - boolean, using the plain MHS algorithm (default is false)
* -aI: - IRI of the individual (with or without prefix defined in the observation) or list of IRIs of individuals in curly brackets (each individual in one line). These individuals are used in the abducibles (default is including all individuals from observation and knowledge base). We can use any number of these switches, and individuals from all of them will be used.
* -aC: - IRI of the class (with or without prefix defined in the observation) or list of IRIs of classes in curly brackets (each class in one line). These classes are used in the abducibles (default is including all classes from observation and knowledge base). We can use any number of these switches, and classes from all of them will be used.
* -abd: - ontology in the ontology format (complete ontology in any ontology syntax) with list of assertions allowed in the abducibles or list of all acceptable assertions without using complete ontology (declarations are not specify, however only Manchester syntax can be used)
* -abdF: - string, path to the file with ontology (complete ontology in any ontology syntax) in which axiom-based abducibles are specified

Only one form of abducibles is allowed (switches -aI and -aC that are combined to assertions in abducibles or abd or abdF).

