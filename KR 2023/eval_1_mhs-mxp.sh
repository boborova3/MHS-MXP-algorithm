#!/bin/bash

for j in {0..4}; do for i in lubm-0_{1..5}_{0..9};do java -Xmx4096m -jar mhs-mxp.jar eval_1/mhs-mxp/"$i".in;done;done