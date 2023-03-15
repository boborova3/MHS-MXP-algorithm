# MHS-MXP: ABox Abduction Solver

MHS-MXP (combination of MHS and MergeXplain) is a complete algorithm for solving abduction problem.

READ ME is not actual, in progress.... 

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

