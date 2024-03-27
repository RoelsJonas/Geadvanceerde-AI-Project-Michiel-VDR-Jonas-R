import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class BranchAndBound {

    public static void branchBound(Solution currentSolution, int umpire, int round) {
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        Integer[] feasibleNextGames = getFeasibleAllocations(umpire, round);
        for(Integer game : feasibleNextGames) {
            if (game < 0 || currentSolution.roundAlreadyHasGame(round, game)) continue; // Infeasible games get marked with a negative number
            int cost = currentSolution.calculateDistance(round, umpire, game);

            int extraUnassignedUmpireCost = 0;

            if(round > 0) {
                int[][] matrix = new int[Main.nUmps - umpire][Main.nUmps - umpire];

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
                                int startAway = Main.games[round - 1][fromGames[i]].away-1;
                                int endLocation = Main.games[round][toGames[j]].home-1;
                                int endAway = Main.games[round][toGames[j]].away-1;
                                boolean notPossible = (endLocation == startLocation && Main.q1 > 0) || (Main.q2 > 0 && (endLocation == startAway || endAway == startLocation || endAway == startAway));
                                if(notPossible) matrix[index1][index2] = 999999999;
                                else matrix[index1][index2] = Main.dist[startLocation][endLocation];
                                index2++;
                            }
                        }
                        index1++;
                    }
                }
                // todo: maak dat dit mogelijk is via boolean aan of af
                extraUnassignedUmpireCost = HungarianAlgorithm.hungarianAlgo(matrix);
            }

            if (currentSolution.totalDistance + cost + ((round < Main.nRounds-1) ?
                    Main.lowerbounds[round+1][Main.nRounds-1] : 0) + extraUnassignedUmpireCost < Main.upperBound) {  // todo: in aparte methode? is de r+1 correct?
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
                    if(Main.umpires[umpire].countVisitedLocations() + Main.nRounds - round >= Main.nTeams)
                        branchBound(currentSolution, nextUmpire, nextRound);
                }
                else {
                    // check if all team-venues are visited by every umpire
                    boolean feasible = true;
                    for (Umpire u: Main.umpires) if(!u.hasVisitedAllLocations()) {
                        feasible = false;
                        break;
                    }
                    if(feasible) {
                        Solution betterSolution = LocalSearch.localSearch(currentSolution);
                        if (Main.upperBound > betterSolution.totalDistance) {
                            Main.best = betterSolution.toString();
                            Main.upperBound = betterSolution.totalDistance;
                            System.out.println("New best solution: " + Main.upperBound);
                            Main.writeSolution("solutions/sol_" + Main.fileName +"_" + Main.q1 + "_" + Main.q2 + ".txt", betterSolution);
//                            System.exit(0);
                        }
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
                Main.umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                Main.umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                Main.umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            }
        }
    }

    // return all feasible next games
    public static Integer[] getFeasibleAllocations(int umpire, int round) {
        Integer[] res = new Integer[Main.nUmps];
        for (int g=0; g<Main.nUmps; g++){
            res[g] = g;
            Umpire ump = Main.umpires[umpire];
            int home = Main.games[round][g].home - 1;
            int away = Main.games[round][g].away - 1;
            // if the umpire has already been in the same venue for q1 consecutive rounds mark the game as infeasible
            boolean condition = ump.q1TeamCounter[home] + Main.q1 > round || ump.q2TeamCounter[home] + Main.q2 > round
                    || ump.q2TeamCounter[away] + Main.q2 > round;
            if (condition){
                res[g] = Integer.MIN_VALUE;
            }
        }

        Arrays.sort(res, Comparator.comparingInt((Integer team) -> {
            if (team < 0) {
                return Integer.MAX_VALUE;
            } else {
                if (round > 0) {
                    return Main.games[round - 1][umpire].distancesToNext[team];
                } else {
                    return 0;
                }
            }
        }));
        return res;
    }

    public static int subBranchBound(Solution currentSolution, int umpire, int round) {
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % Main.nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        Integer[] feasibleNextGames = getFeasibleAllocations(umpire, round);
        for(Integer game : feasibleNextGames) {
            if (game < 0 || currentSolution.roundAlreadyHasGame(round, game)) continue; // Infeasible games get marked with a negative number
            int cost = currentSolution.calculateDistance(round, umpire, game);
            int extraUnassignedUmpireCost = 0; // TODO IMPLEMENT EXTRA MATHCING PROBLEM
            if (currentSolution.totalDistance + cost + Main.lowerbounds[round+1][Main.nRounds-1] < Main.upperBound) {  // todo: in aparte methode? is de r+1 correct?
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
                    if(Main.umpires[umpire].countVisitedLocations() + Main.nRounds - round >= Main.nTeams)
                        branchBound(currentSolution, nextUmpire, nextRound);
                }
                else {
                    // check if all team-venues are visited by every umpire
                    boolean feasible = true;
                    for (Umpire u: Main.umpires) if(!u.hasVisitedAllLocations()) {
                        feasible = false;
                        break;
                    }
                    if(feasible) {
                        Solution betterSolution = LocalSearch.localSearch(currentSolution);
                        if (Main.upperBound > betterSolution.totalDistance) {
                            Main.best = betterSolution.toString();
                            Main.upperBound = betterSolution.totalDistance;
                            System.out.println("New best solution: " + Main.upperBound);
                            Main.writeSolution("solutions/sol_" + Main.fileName +"_" + Main.q1 + "_" + Main.q2 + ".txt", betterSolution);
//                            System.exit(0);
                        }
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
                Main.umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                Main.umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                Main.umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            }
        }
        return 0;
    }

}
