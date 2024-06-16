import java.util.HashMap;

public class BranchAndBoundParallel {
    public int subResult = Integer.MAX_VALUE;
    public static long nodeCounter = 0;
    public static long startTime = System.currentTimeMillis();
    public static HashMap<Integer, Long> firstPrunes = new HashMap<>();
    public static HashMap<Integer, Long> secondPrunes = new HashMap<>();
    public Umpire[] umpires;

    public Solution currentSolution;

    public BranchAndBoundParallel(Solution currentSolution, Umpire[] umpires) {
        this.currentSolution = currentSolution;
        this.umpires = umpires;
    }

    public BranchAndBoundParallel() {
//        startTime = System.currentTimeMillis();
        currentSolution = new Solution();
        // create a copy of the umpires from main
        umpires = new Umpire[Main.nUmps];
        for (int i=0; i<Main.nUmps; i++) {
            umpires[i] = new Umpire(i);
            umpires[i].q1TeamCounter = Main.umpires[i].q1TeamCounter.clone();
            umpires[i].q2TeamCounter = Main.umpires[i].q2TeamCounter.clone();
            umpires[i].q1TeamCounterLB = Main.umpires[i].q1TeamCounterLB.clone();
            umpires[i].q2TeamCounterLB = Main.umpires[i].q2TeamCounterLB.clone();
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
//        if(System.currentTimeMillis() - startTime > 300000) return;
        nodeCounter++;
        if(Main.DEBUG && nodeCounter % 10000000 == 0) {
            System.out.println("Nodes per second: " + (nodeCounter / ((double)(System.currentTimeMillis() - startTime) / 1000)) + " Nodes: " + nodeCounter + " Best: " + Main.upperBound);
        }
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        int[] feasibleNextGames = getFeasibleAllocations(umpire, round, true); // TODO OPTIMIZE
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
                    if(umpires[umpire].countVisitedLocations(true) + Main.nRounds - round >= Main.nTeams)
                        branchBound(nextUmpire, nextRound);
                }
                else {
                    // check if all team-venues are visited by every umpire
                    boolean feasible = true;
                    for (Umpire u: umpires) if(!u.hasVisitedAllLocations(true)) {
                        feasible = false;
                        break;
                    }
                    if(feasible) {
                        Solution betterSolution = LocalSearch.localSearch(currentSolution);
                        synchronized (this) {
                            if (Main.upperBound > betterSolution.totalDistance) {
                                Main.best = betterSolution.toString();
                                Main.upperBound = betterSolution.totalDistance;
//                            System.out.println("New best solution: " + Main.upperBound);
                            Main.writeSolution("solutions/sol_" + Main.fileName +"_" + Main.q1 + "_" + Main.q2 + ".txt", Main.best);
                            }
                        }
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
                umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            } else {
                secondPrunes.put(round, secondPrunes.getOrDefault(round, 0L) + 1);
            }
        }
    }

    // return all feasible next games
    public int[] getFeasibleAllocations(int umpire, int round, boolean realdeal) {
        if(!Main.SORT_ALLOCATIONS_EN) return getFeasibleSubAllocations(umpire, round, realdeal);
        Umpire ump = umpires[umpire];
        int[] res = new int[Main.nUmps];
        for (int g=0; g<Main.nUmps; g++){
            // TODO ROUND-1 of niet?
            if(round > 0) res[g] = Main.games[round-1][currentSolution.sol[round-1][umpire]].nextGames[g];
            else res[g] = g;
            int home = Main.games[round][res[g]].home - 1;
            int away = Main.games[round][res[g]].away - 1;

            // if the umpire has already visited the venue in the last q1 consecutive rounds or already officiated one of the teams in the last q2 rounds, mark the game as infeasible
            if (realdeal) {
                if (ump.q1TeamCounter[home] + Main.q1 > round
                        || ump.q2TeamCounter[home] + Main.q2 > round
                        || ump.q2TeamCounter[away] + Main.q2 > round){
                    res[g] = -1;
                }
            }
            else {
                if (ump.q1TeamCounterLB[home] + Main.q1 > round
                        || ump.q2TeamCounterLB[home] + Main.q2 > round
                        || ump.q2TeamCounterLB[away] + Main.q2 > round){
                    res[g] = -1;
                }
            }
        }

//        Arrays.sort(res);
        return res;
    }

    public int[] getFeasibleSubAllocations(int umpire, int round, boolean realdeal) {
        Umpire ump = umpires[umpire];
        int[] res = new int[Main.nUmps];
        for (int g=0; g<Main.nUmps; g++){
            res[g] = g;
            int home = Main.games[round][g].home - 1;
            int away = Main.games[round][g].away - 1;

            // if the umpire has already visited the venue in the last q1 consecutive rounds or already officiated one of the teams in the last q2 rounds, mark the game as infeasible
            if (realdeal) {
                if (ump.q1TeamCounter[home] + Main.q1 > round
                        || ump.q2TeamCounter[home] + Main.q2 > round
                        || ump.q2TeamCounter[away] + Main.q2 > round){
                    res[g] = -1;
                }
            }
            else {
                if (ump.q1TeamCounterLB[home] + Main.q1 > round
                        || ump.q2TeamCounterLB[home] + Main.q2 > round
                        || ump.q2TeamCounterLB[away] + Main.q2 > round){
                    res[g] = -1;
                }
            }
        }

//        Arrays.sort(res);
        return res;
    }


    public int subBranchBound(int umpire, int round, int startRound, int endRound) {
//        System.out.println(endRound);
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        int[] feasibleNextGames = getFeasibleSubAllocations(umpire, round, false);
        for(int game : feasibleNextGames) {
            if (game < 0 || currentSolution.roundAlreadyHasGame(round, game)) continue; // Infeasible games get marked with a negative number
            int cost = currentSolution.calculateDistance(round, umpire, game);
            Main.usedBounds[round][endRound]++;
            int extraUnassignedUmpireCost = 0;
            if(round > 0 && Main.nUmps - umpire - 1 > 0) extraUnassignedUmpireCost = Main.partialBounds[round][Main.nUmps - umpire -1];
            if (currentSolution.totalDistance + cost + Main.lowerBounds[round][endRound] + extraUnassignedUmpireCost < subResult) {  // todo: in aparte methode? is de r+1 correct?
                int homeIndex = Main.games[round][game].home-1;
                int awayIndex = Main.games[round][game].away-1;
                currentSolution.addGame(round, umpire, game, cost);
                int previousQ1 = umpires[umpire].q1TeamCounterLB[homeIndex];
                umpires[umpire].q1TeamCounterLB[homeIndex] = round;
                int previousQ2Home = umpires[umpire].q2TeamCounterLB[homeIndex];
                int previousQ2Away = umpires[umpire].q2TeamCounterLB[awayIndex];
                umpires[umpire].q2TeamCounterLB[homeIndex] = round;
                umpires[umpire].q2TeamCounterLB[awayIndex] = round;

                // If there is a next round we recurse else we start the local search algorithm
                if (nextRound <= endRound)
//                    if(Main.umpires[umpire].countVisitedLocations() + Main.nRounds - round + startRound >= Main.nTeams - 1)
                    subBranchBound(nextUmpire, nextRound, startRound, endRound);

                else if (subResult > currentSolution.totalDistance)
                    subResult = currentSolution.totalDistance;

                currentSolution.removeGame(round, umpire, game, cost);
                umpires[umpire].q1TeamCounterLB[homeIndex] = previousQ1;
                umpires[umpire].q2TeamCounterLB[homeIndex] = previousQ2Home;
                umpires[umpire].q2TeamCounterLB[awayIndex] = previousQ2Away;
            }
        }
        return subResult;
    }
}
