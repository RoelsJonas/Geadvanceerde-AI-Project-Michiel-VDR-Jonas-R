import java.util.HashMap;

public class BranchAndBound {
    public static int subResult = Integer.MAX_VALUE;
    public static int nodeCounter = 0;
    public static long startTime = 0;
    public static HashMap<Integer, Long> firstPrunes = new HashMap<>();
    public static HashMap<Integer, Long> secondPrunes = new HashMap<>();

    public static void branchBound(Solution currentSolution, int umpire, int round) {
//        if(System.currentTimeMillis() - startTime > 300000) return;
        nodeCounter++;
        if(Main.DEBUG && nodeCounter % 10000000 == 0) {
            System.out.println("Nodes per second: " + (nodeCounter / ((double)(System.currentTimeMillis() - startTime) / 1000)) + " Nodes: " + nodeCounter + " Best: " + Main.upperBound);
        }
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        int[] feasibleNextGames = getFeasibleAllocations(umpire, round, currentSolution, true); // TODO OPTIMIZE
        for(int game : feasibleNextGames) {
            if (game < 0) continue; // Infeasible games get marked with a negative number
            if(currentSolution.roundAlreadyHasGame(round, game)) continue; // Game cannot already be allocated in current round TODO OPTIMIZE
            int cost = currentSolution.calculateDistance(round, umpire, game); // TODO OPTIMIZE?

            // Partial matching problem to predict resting count in this round
            int extraUnassignedUmpireCost = 0;
            // TODO CHECK HERE IF WE CAN ALREADY PRUNE
            if (currentSolution.totalDistance +
                    cost +
                    Main.lowerbounds[round][Main.nRounds-1] +
                    extraUnassignedUmpireCost > Main.upperBound) {
                firstPrunes.put(round, firstPrunes.getOrDefault(round, 0L) + 1);
                continue;
            }
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
                    Main.lowerbounds[round][Main.nRounds-1] +
                    extraUnassignedUmpireCost <= Main.upperBound) {
                int homeIndex = Main.games[round][game].home-1;
                int awayIndex = Main.games[round][game].away-1;
                currentSolution.addGame(round, umpire, game, cost);
                int previousQ1 = Main.umpires[umpire].q1TeamCounter[homeIndex];
                Main.umpires[umpire].q1TeamCounter[homeIndex] = round;
                int previousQ2Home = Main.umpires[umpire].q2TeamCounter[homeIndex];
                int previousQ2Away = Main.umpires[umpire].q2TeamCounter[awayIndex];
                Main.umpires[umpire].q2TeamCounter[homeIndex] = round;
                Main.umpires[umpire].q2TeamCounter[awayIndex] = round;

                // If there is a next round we recurse else we start the local search algorithm
                if (nextRound < Main.nRounds) {
                    if(Main.umpires[umpire].countVisitedLocations(true) + Main.nRounds - round >= Main.nTeams)
                        branchBound(currentSolution, nextUmpire, nextRound);
                }
                else {
                    // check if all team-venues are visited by every umpire
                    boolean feasible = true;
                    for (Umpire u: Main.umpires) if(!u.hasVisitedAllLocations(true)) {
                        feasible = false;
                        break;
                    }
                    if(feasible) {
                        Solution betterSolution = LocalSearch.localSearch(currentSolution);
                        if (Main.upperBound > betterSolution.totalDistance) {
                            Main.best = betterSolution.toString();
                            Main.upperBound = betterSolution.totalDistance;
//                            System.out.println("New best solution: " + Main.upperBound);
//                            Main.writeSolution("solutions/sol_" + Main.fileName +"_" + Main.q1 + "_" + Main.q2 + ".txt", Main.best);
                        }
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
                Main.umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                Main.umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                Main.umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            } else {
                secondPrunes.put(round, secondPrunes.getOrDefault(round, 0L) + 1);
            }
        }
    }

    // return all feasible next games
    public static int[] getFeasibleAllocations(int umpire, int round, Solution currentSolution, boolean realdeal) {
        if(!Main.SORT_ALLOCATIONS_EN) return getFeasibleSubAllocations(umpire, round, currentSolution, realdeal);
        Umpire ump = Main.umpires[umpire];
        int[] res = new int[Main.nUmps];
        for (int g=0; g<Main.nUmps; g++){
            if(round > 0) res[g] = Main.games[round][currentSolution.sol[round-1][umpire]].nextGames[g];
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

    public static int[] getFeasibleSubAllocations(int umpire, int round, Solution currentSolution, boolean realdeal) {
        Umpire ump = Main.umpires[umpire];
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

    public static int subBranchBound(Solution currentSolution, int umpire, int round, int startRound, int endRound) {
//        System.out.println(endRound);
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        int[] feasibleNextGames = getFeasibleSubAllocations(umpire, round, currentSolution, false);
        for(int game : feasibleNextGames) {
            if (game < 0 || currentSolution.roundAlreadyHasGame(round, game)) continue; // Infeasible games get marked with a negative number
            int cost = currentSolution.calculateDistance(round, umpire, game);
            Main.usedBounds[round][endRound]++;
            int extraUnassignedUmpireCost = 0;
            if(round > 0 && Main.nUmps - umpire - 1 > 0) extraUnassignedUmpireCost = Main.partialBounds[round][Main.nUmps - umpire -1];
            if (currentSolution.totalDistance + cost + Main.lowerbounds[round][endRound] + extraUnassignedUmpireCost < subResult) {  // todo: in aparte methode? is de r+1 correct?
                int homeIndex = Main.games[round][game].home-1;
                int awayIndex = Main.games[round][game].away-1;
                currentSolution.addGame(round, umpire, game, cost);
                int previousQ1 = Main.umpires[umpire].q1TeamCounterLB[homeIndex];
                Main.umpires[umpire].q1TeamCounterLB[homeIndex] = round;
                int previousQ2Home = Main.umpires[umpire].q2TeamCounterLB[homeIndex];
                int previousQ2Away = Main.umpires[umpire].q2TeamCounterLB[awayIndex];
                Main.umpires[umpire].q2TeamCounterLB[homeIndex] = round;
                Main.umpires[umpire].q2TeamCounterLB[awayIndex] = round;

                // If there is a next round we recurse else we start the local search algorithm
                if (nextRound <= endRound)
//                    if(Main.umpires[umpire].countVisitedLocations() + Main.nRounds - round + startRound >= Main.nTeams - 1)
                        subBranchBound(currentSolution, nextUmpire, nextRound, startRound, endRound);

                else if (subResult > currentSolution.totalDistance)
                    subResult = currentSolution.totalDistance;

                currentSolution.removeGame(round, umpire, game, cost);
                Main.umpires[umpire].q1TeamCounterLB[homeIndex] = previousQ1;
                Main.umpires[umpire].q2TeamCounterLB[homeIndex] = previousQ2Home;
                Main.umpires[umpire].q2TeamCounterLB[awayIndex] = previousQ2Away;
            }
        }
        return subResult;
    }

}
