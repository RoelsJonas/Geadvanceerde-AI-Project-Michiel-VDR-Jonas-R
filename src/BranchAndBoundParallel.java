public class BranchAndBoundParallel {
    public int subResult = Integer.MAX_VALUE;
    public static long nodeCounter = 0;
    public static long startTime = System.currentTimeMillis();
    public Umpire[] umpires;
    public Solution currentSolution;
    public boolean main = false;


    public BranchAndBoundParallel(Solution currentSolution, Umpire[] umpires) {
        this.currentSolution = currentSolution;
        this.umpires = umpires;
    }

    public BranchAndBoundParallel() {
        currentSolution = new Solution();
        // create a copy of the umpires from main
        umpires = new Umpire[Main.nUmps];
        for (int i=0; i<Main.nUmps; i++) {
            umpires[i] = new Umpire(i);
        }
        // fix the first round
        for(int i = 0; i < Main.nUmps; i++) {
            int homeIndex = Main.games[0][i].home-1;
            int awayIndex = Main.games[0][i].away-1;
            currentSolution.addGame(0, i, i, 0);
            umpires[i].q1TeamCounter[homeIndex] = 0;
            umpires[i].q2TeamCounter[homeIndex] = 0;
            umpires[i].q2TeamCounter[awayIndex] = 0;
        }
    }

    public void branchBound(int umpire, int round) {
        nodeCounter++;
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        int[] feasibleNextGames = getFeasibleAllocations(umpire, round); // TODO OPTIMIZE
        for(int game : feasibleNextGames) {
            if (game < 0) continue; // Infeasible games get marked with a negative number
            if(currentSolution.roundAlreadyHasGame(round, game)) continue; // Game cannot already be allocated in current round TODO OPTIMIZE
            int cost = currentSolution.calculateDistance(round, umpire, game); // TODO OPTIMIZE?

            // Partial matching problem to predict resting count in this round
            int extraUnassignedUmpireCost = 0;
            if(Main.PARTIAL_MATCH_EN && round > 0 && Main.nUmps - umpire - 1 > 0) {
                if(Main.GREEDY_EN) {
                    extraUnassignedUmpireCost = Main.partialBounds[round][Main.nUmps - umpire -1];
                }
                else {


                    int[][] matrix = new int[Main.nUmps - umpire - 1][Main.nUmps - umpire - 1];

                    int[] fromGames = new int[Main.nUmps];
                    int[] toGames = new int[Main.nUmps];

                    for (int i = 0; i < Main.nUmps; i++) {
                        if (!currentSolution.roundAlreadyHasGame(round, i) && i != game) toGames[i] = i;
                        else toGames[i] = -1;
                        fromGames[i] = i;
                    }

                    for (int i = 0; i < umpire + 1; i++) {
                        fromGames[currentSolution.sol[round - 1][i]] = -1;
                    }

                    int index1 = 0;
                    int index2 = 0;
                    for (int i = 0; i < Main.nUmps; i++) {
                        if (fromGames[i] != -1) {
                            index2 = 0;
                            for (int j = 0; j < Main.nUmps; j++) {
                                if (toGames[j] != -1) {
                                    int startLocation = Main.games[round - 1][fromGames[i]].home - 1;
                                    int startAway = Main.games[round - 1][fromGames[i]].away - 1;
                                    int endLocation = Main.games[round][toGames[j]].home - 1;
                                    int endAway = Main.games[round][toGames[j]].away - 1;
                                    boolean notPossible = (endLocation == startLocation && Main.q1 > 0) || (Main.q2 > 0 && (endLocation == startAway || endAway == startLocation || endAway == startAway));
                                    if (notPossible) matrix[index1][index2] = 999999999;
                                    else matrix[index1][index2] = Main.dist[startLocation][endLocation];
                                    index2++;
                                }
                            }
                            index1++;
                        }
                    }
                    extraUnassignedUmpireCost = HungarianAlgorithm.hungarianAlgo(matrix);
                }
            }

            // check if we can prune this branch
            if (currentSolution.totalDistance +
                    cost +
                    Main.lowerBounds[round][Main.nRounds-1] +
                    extraUnassignedUmpireCost <= Main.upperBound) {
                int homeIndex = Main.games[round][game].home-1;
                int awayIndex = Main.games[round][game].away-1;
                currentSolution.addGame(round, umpire, game, cost);
                int previousQ1 = umpires[umpire].q1TeamCounter[homeIndex];
                umpires[umpire].q1TeamCounter[homeIndex] = round;
                int previousQ2Home = umpires[umpire].q2TeamCounter[homeIndex];
                int previousQ2Away = umpires[umpire].q2TeamCounter[awayIndex];
                umpires[umpire].q2TeamCounter[homeIndex] = round;
                umpires[umpire].q2TeamCounter[awayIndex] = round;

                // If there is a next round we recurse else we start the local search algorithm
                if (nextRound < Main.nRounds) {
                    if(umpires[umpire].countVisitedLocations() + Main.nRounds - round >= Main.nTeams)
                        branchBound(nextUmpire, nextRound);
                }
                else {
                    // check if all team-venues are visited by every umpire
                    boolean feasible = true;
                    for (Umpire u: umpires) if(!u.hasVisitedAllLocations()) {
                        feasible = false;
                        break;
                    }
                    String sol = currentSolution.toString();
                    if(feasible) {
                        Main.lock.lock();
                            if (Main.upperBound > currentSolution.totalDistance) {
                                Main.best = sol;
                                Main.upperBound = currentSolution.totalDistance;
                                if(Main.validate()) {
                                    System.out.println("NEW BEST SOLUTION: " + Main.upperBound + " (" + (System.currentTimeMillis() - startTime) + "ms)");
                                    Main.writeSolution("solutions/sol_" + Main.fileName + "_" + Main.q1 + "_" + Main.q2 + ".txt", Main.best);
                                }
                                else
                                    System.out.printf("INVALID SOLUTION (%d)!!!\n%s\n", Main.upperBound, Main.best);
                            }
                        Main.lock.unlock();
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
                umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            }
        }
    }

    // return all feasible next games
    public int[] getFeasibleAllocations(int umpire, int round) {
        if(!Main.SORT_ALLOCATIONS_EN) return getFeasibleSubAllocations(umpire, round);
        Umpire ump = umpires[umpire];
        int[] res = new int[Main.nUmps];
        for (int g=0; g<Main.nUmps; g++){
            // TODO ROUND-1 of niet?
            if(round > 0) res[g] = Main.games[round-1][currentSolution.sol[round-1][umpire]].nextGames[g];
            else res[g] = g;
            int home = Main.games[round][res[g]].home - 1;
            int away = Main.games[round][res[g]].away - 1;

            // if the umpire has already visited the venue in the last q1 consecutive rounds or already officiated one of the teams in the last q2 rounds, mark the game as infeasible
            if (ump.q1TeamCounter[home] + Main.q1 > round
                    || ump.q2TeamCounter[home] + Main.q2 > round
                    || ump.q2TeamCounter[away] + Main.q2 > round){
                res[g] = -1;
            }
        }

        return res;
    }

    public int[] getFeasibleSubAllocations(int umpire, int round) {
        Umpire ump = umpires[umpire];
        int[] res = new int[Main.nUmps];
        for (int g=0; g<Main.nUmps; g++){
            res[g] = g;
            int home = Main.games[round][g].home - 1;
            int away = Main.games[round][g].away - 1;

            // if the umpire has already visited the venue in the last q1 consecutive rounds or already officiated one of the teams in the last q2 rounds, mark the game as infeasible
            if (ump.q1TeamCounter[home] + Main.q1 > round
                    || ump.q2TeamCounter[home] + Main.q2 > round
                    || ump.q2TeamCounter[away] + Main.q2 > round){
                res[g] = -1;
            }
        }

        return res;
    }


    public int subBranchBound(int umpire, int round, int startRound, int endRound) {
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        int[] feasibleNextGames = getFeasibleSubAllocations(umpire, round);
        for(int game : feasibleNextGames) {
            if (game < 0 || currentSolution.roundAlreadyHasGame(round, game)) continue; // Infeasible games get marked with a negative number
            int cost = currentSolution.calculateDistance(round, umpire, game);
            int extraUnassignedUmpireCost = 0;
            if(round > 0 && Main.nUmps - umpire - 1 > 0) extraUnassignedUmpireCost = Main.partialBounds[round][Main.nUmps - umpire -1];
            if (currentSolution.totalDistance + cost + Main.lowerBounds[round][endRound] + extraUnassignedUmpireCost < subResult) {  // todo: in aparte methode? is de r+1 correct?
                int homeIndex = Main.games[round][game].home-1;
                int awayIndex = Main.games[round][game].away-1;
                currentSolution.addGame(round, umpire, game, cost);
                int previousQ1 = umpires[umpire].q1TeamCounter[homeIndex];
                umpires[umpire].q1TeamCounter[homeIndex] = round;
                int previousQ2Home = umpires[umpire].q2TeamCounter[homeIndex];
                int previousQ2Away = umpires[umpire].q2TeamCounter[awayIndex];
                umpires[umpire].q2TeamCounter[homeIndex] = round;
                umpires[umpire].q2TeamCounter[awayIndex] = round;

                // If there is a next round we recurse else we start the local search algorithm
                if (nextRound <= endRound)
                        subBranchBound(nextUmpire, nextRound, startRound, endRound);

                else if (subResult > currentSolution.totalDistance)
                    subResult = currentSolution.totalDistance;

                currentSolution.removeGame(round, umpire, game, cost);
                umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            }
        }
        return subResult;
    }

    public static boolean validate(String sol) {
        boolean valid = true;
        // convert string to matrix of home locations
        String[] lines = sol.split("\n");
        int[][] homeLocations = new int[Main.nUmps][Main.nRounds];
        for (int i=0; i<Main.nUmps; i++) {
            String[] locations = lines[i].split(" ");
            for (int j=0; j<Main.nRounds; j++) {
                homeLocations[i][j] = Integer.parseInt(locations[j]);
            }
        }
        // check if every umpire visits every location
        for (int i=0; i<Main.nUmps; i++) {
            for (int j=0; j<Main.nTeams; j++) {
                boolean visited = false;
                for (int k=0; k<Main.nRounds; k++) {
                    if (homeLocations[i][k] == j+1) {
                        visited = true;
                        break;
                    }
                }
                if (!visited) {
                    return false;
                }
            }
        }

        // check q1 constraint
        for (int i=0; i<Main.nUmps; i++) {
            for (int j=0; j<Main.nRounds; j++) {
                int home = homeLocations[i][j];
                boolean visited = false;
                for(int k = j+1; k < j+Main.q1; k++) {
                    if(k >= Main.nRounds) break;
                    if(homeLocations[i][k] == home) {
                        visited = true;
                        break;
                    }
                }
                if (visited) {
                    return false;
                }
            }
        }

        // create a awayTeams matrix using games
        int[][] awayTeams = new int[Main.nUmps][Main.nRounds];
        for (int i=0; i<Main.nRounds; i++) {
            for (int j=0; j<Main.nUmps; j++) {
                int home = homeLocations[j][i];
                for(int g=0; g<Main.nUmps; g++) {
                    if(Main.games[i][g].home == home) {
                        awayTeams[j][i] = Main.games[i][g].away;
                        break;
                    }
                }
            }
        }

        // check q2 constraint
        for (int i=0; i<Main.nUmps; i++) {
            for (int j=0; j<Main.nRounds; j++) {
                int team = homeLocations[i][j];
                int homeTeam = awayTeams[i][j];
                boolean visited = false;
                for(int k = j+1; k < j+Main.q2; k++) {
                    if(k >= Main.nRounds) break;
                    if(homeLocations[i][k] == homeTeam || awayTeams[i][k] == homeTeam || homeLocations[i][k] == team || awayTeams[i][k] == team) {
                        visited = true;
                        break;
                    }
                }
                if (visited) {
                    return false;
                }
            }
        }
        return valid;
    }
}
