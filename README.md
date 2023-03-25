# MHS-MXP: ABox Abduction Solver

MHS-MXP (combination of MHS and MergeXplain) is a complete algorithm for solving abduction problem.

## Folder contents
* **KR2023** is a repository for *International Conference on Principles of Knowledge Representation and Reasoning*
* **files** contains input ontologies
* **in** contains input examples
* **old_versions** contains JAR files of the previous version
* **src/main** contains the source code of the abduction solver

## How to run MHS-MXP
You can run MHS-MXP through the command line (allocation of more memory for Java is recommended), for example:

**java -Xmx4096m -jar mhs-mxp.jar in/testExtractingModels/pokus9.in**, where **in/testExtractingModels/pokus9.in** is a relative path to the input file

Another way to run the solver is directly through the **src/main/java/Main.java** class in a Java IDE.

## Input
MHS-MXP receives a structured input file as a parameter. The input file contains one switch per line. Mandatory switches are **-f** and **-o**, other switches are optional.

* **-f: \<string\>**  a relative path to the ontology file, which represents the knowledge base $K$.
* **-o: \<ontology\>** observation $O$ in the form of an ontology (in any ontology syntax), which has to be written in one line.
* *-t: \<positive integer\>* the time after which the search for explanations terminates. By default, it is not set.
* *-d: \<positive integer\>* the depth of the HS-tree, when the search terminates. For example, for *-d: 2* search terminates after completing level 1 of HS-tree. By default, it is not set.
* *-mhs: \<boolean\>*  using the MHS algorithm for explanations search. Set to *false*, by default.
* *-r: \<boolean\>* allowing role assertions in explanations. Set to *false*, by default.
* *-n: \<boolean\>*  allowing negated assertions in explanations. Set to *true*, by default.
* *-l: \<boolean\>* allows assertions of form $i, i: R$ in explanations, i.e. individual $i$ can be in role $R$ with itself (it is also called *looping*). 
* *-output: \<string\>* custom relative path to output log files.

#### Relevance for multiple observation
In the case where observation consists of multiple assertions (also called multiple observation), there are two ways how to define relevant explanation.
1. *Strictly relevant explanation:* an explanation is relevant with respect to **each** assertion from the observation $O$. 
2. *Partially relevant explanation:* an explanation is relevant with respect to **at least one** assertion from the observation $O$.

* *-sR: \<boolean\>* what type of relevance should be used. Set to *true* (scrict relevance), by default.  

### Abducibles
#### Defining abducibles using <ins>entities</ins> they can contain:

* *-aI: \<IRI of an individual\>* defines individual that is used in abducibles. We can define more abducible individuals by writing multiple *-aI* switches. By default, all invididuals from $K$ and $O$ are used.
* *-aC: \<IRI of a class\>* defines class that is used in abducibles. We can define more classes by writing multiple *-aC* switches. By default, all classes from $K$ and $O$ are used.
* *-aR: \<IRI of an object property\>* defines object property that is used in abducibles. We can define more object properties by writing multiple *-aR* switches. By default, all object properties from $K$ and $O$ are used.

There is also another way by which it is possible to define a list of individuals in one *-aI* switch. The list is wrapped in curly braces and each individual is in one line.

*-aI: {*

*\<IRI of an individual 1\>*

*\<IRI of an individual 2\>*

*}*

Same can be done with *-aC* (defining a list of classes) and *-aR* (defining a list of object properties).

#### Defining abducibles directly by enumerating <ins>assertions</ins>:
* *-abd: \<ontology\>* a complete ontology (in any ontology syntax), which has to be written in one line.

Another way is to list only assertions. In this case, *Manchester Syntax* needs to be used. 

*-abd: {*

*\<assertion 1\>*

*\<assertion 2\>*

*}* 
    
* *-abdF: \<string\>* a relative path to the ontology file, assertions from the ontology will be used as abducibles.

Abducibles cannot be defined by combining different definition types. Either they are defined using entities (*-aI*, *-aC*, *-aR*), using an ontology or list of assertions (*-abd*), or from the ontology file (*-abdF*).
    

## Output
As an output for a given input, the solver produces several log files. Time in the logs is given in seconds.

### Final logs 
Final logs are created only after the search for explanations is complete (it either ended normally and all explanations have been found or was interrupted by the depth limit or timeout). The found explanations are filtered and only the desired ones are written in the logs.

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

**Info log**
*\<time\>__\<input file name\>__info.log*
* contains basic information about how the input was set in a given run, e.g. timeout, maximum depth or enabling negation in explanations (switches)

**Error log**
*\<time\>__\<input file name\>__error.log*
* created only if an error occurred during the construction of the HS-tree
* records an error that occurred

### Partial logs
Partial logs are created while the solving of the abduction problem is running. They help us to have an overview of the progress along the run. They record, for example, possible explanations which were found after passing one level. However, these explanations are only possible explanations and therefore may not be desired.

**Partial explanations log**
*\<time\>__\<input file name\>__hybrid_partial_explanations.log*

* partial log with the same structure as **hybrid log**
* may contain also undesired explonations 

**Partial level explanations log**
*\<time\>__\<input file name\>__hybrid_partial_level_explanations.log*

* partial log with the same structure as **level log**
* may contain also undesired explonations 

When solving the abduction problem using MHS-MXP, all the mentioned logs are produced.
When using the MHS algorithm, however, only some are produced: **hybrid log**, **explanation times log**, **info log**, **error log** and **partial explanations log**. The reason for this is that other logs would be redundant. For the MHS algorithm, the grouping of explanations according to the length is identical to grouping them according to the levels.

Logs of inputs that used MHS have a different location than logs of inputs that used the MHS-MXP algorithm.
* location of MHS-MXP logs: *logs/...*
* location of MHS logs: *logs_mhs/...*
