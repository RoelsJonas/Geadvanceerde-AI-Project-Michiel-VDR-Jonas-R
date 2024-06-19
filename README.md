Traveling umpire problem (TUP) solver by **Jonas Roels** and **Michiel Vandromme** based on the Branch-and-bound with decomposition-based lower bounds algorithm by Toffolo et al. (https://github.com/tuliotoffolo/tup).

**Improvements:**
- Faster partial matching using an a priori approach using the Hungarian algorithm
- Multi threading to divide the search space and improve the amount of nodes processed per second
- Bounds propagation to get stronger bounds before lower bounds calculation has finished

**Running the program:**
```
java -jar .\Geadvanceerde-AI-Project-Michiel-VDR-Jonas-R.jar <path to input file> <q1> <q2> <path to solution file> <max runtime>
```
Example: 
```
java -jar .\Geadvanceerde-AI-Project-Michiel-VDR-Jonas-R.jar .\instances\umps16.txt 7 3 .\solutions\sol_umps14_7_3_jar.txt 2880
```
