# BBD: BEAST2 Better Dating
Bradley R. Jones

BEAST2 Better Dating (BBD) is a package for the software Bayesian Evolutionary Analysis Sampling Trees 2 (BEAST2). BBD includes new priors and operators to extend BEAST2's ability to perform phylogenetic dating of tips and divergence times.

## Installation

1. Navigate to the BEAST2 package folder:</br>
Linux: ~/.beast/2.6</br>
macOS: ~/Library/Application Support/BEAST/2.6</br>
Windows: C:\Users\USERNAME\Documents\BEAST\2.6
2. Create the directory BBD
3. Unzip dist/BBD.addon.zip from this git repo into BBD
4. In the file beauti.properties, add "\\:" followed by the full path of BBD followed by "lib/BBD.addon.jar"
```
#Automatically-generated by BEAUti.
# 
#Fri Jun 17 12:08:16 PDT 2022
currentDir=/Users/USERNAME/work/bbd/sim/rep_0/aligned
package.update.status=AUTO_CHECK_AND_ASK
package.path=\:/Users/USERNAME/Library/Application Support/BEAST/2.6/BEAST/lib/beast.jar\:/Users/USERNAME/Library/Application Support/BEAST/2.6/BBD/lib/BBD.addon.jar
```


To add prior in BEAUTi 2:

1. Add aligment and tip dates
2. Go to Priors tab and click the "+ Add Prior Button"
3. Select "BBD Prior" and click OK
4. Choose taxa to date, give taxon set a label and click OK
6. Select your prior distribution and starting date probability and you're off.

