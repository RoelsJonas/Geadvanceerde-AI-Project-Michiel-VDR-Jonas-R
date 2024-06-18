TUP solver based on the Branch-and-bound with decomposition-based lower bounds algorithm by Toffolo et al. (https://github.com/tuliotoffolo/tup).

Improvements:
- Faster partial matching using an a priori approach using the Hungarian algorithm
- Multi threading to divide the search space and improve the amount of nodes processed per second
- Bounds propagation to get stronger bounds before lower bounds calculation has finished
