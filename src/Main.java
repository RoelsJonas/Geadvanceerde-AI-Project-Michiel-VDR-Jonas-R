import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class Main {
    // VALIDATE THE SOLUTION USING  java -jar validator.jar .\\instances\\umps8.txt 2 2 .\\solutions\\sol_umps8_2_2.txt
    // CONSTANTS
    private static final boolean DEBUG = true;


    // VARIABLES
    public static int nTeams;
    public static int nUmps;
    public static int nRounds;
    public static String best = "No solution found";
    public static int upperBound = Integer.MAX_VALUE;
    public static int lowerBound = 0;
    public static int q1 = 2;  // umpire not in venue for q1 consecutive rounds
    public static int q2 = 2;  // umpire not for same team in q2 consecutive rounds


    public static int[][] dist;
    public static int[][] opponents;
    public static Game[][] games;

    public static void main(String[] args) throws Exception {

        // Open the file
        fileName = "umps8";
        readInput("instances/" + fileName + ".txt");
        processGames();
//        best = new Solution();

        Solution currentSolution = new Solution();
        currentSolution.totalDistance = 0;
        branchBound(currentSolution, 0, 0);
//        System.out.println(best);
    }

    private static void branchBound(Solution currentSolution, int umpire, int round) {
        // Determine the next umpire and round
        int nextUmpire = (umpire+1) % nUmps;
        int nextRound = (nextUmpire == 0) ? round+1 : round;

        // Get an array (sorted by shorted distance) of all feasible next games the current umpire can be assigned to in this round
        Integer[] feasibleNextGames = getFeasibleAllocations(umpire, round);
        for(Integer game : feasibleNextGames) {
            if (game < 0 || currentSolution.roundAlreadyHasGame(round, game)) continue; // Infeasible games get marked with a negative number
            int cost = currentSolution.calculateDistance(round, umpire, game);
            if (currentSolution.totalDistance + cost + lowerBound < upperBound) {  // todo: in aparte methode?
                int homeIndex = games[round][game].home-1;
                int awayIndex = games[round][game].away-1;
                currentSolution.addGame(round, umpire, game, cost);
                int previousQ1 = umpires[umpire].q1TeamCounter[homeIndex];
                umpires[umpire].q1TeamCounter[homeIndex] = round;
                int previousQ2Home = umpires[umpire].q2TeamCounter[homeIndex];
                int previousQ2Away = umpires[umpire].q2TeamCounter[awayIndex];
                umpires[umpire].q2TeamCounter[homeIndex] = round;
                umpires[umpire].q2TeamCounter[awayIndex] = round;

                // If there is a next round we recurse else we start the local search algorithm
                if (nextRound < nRounds) {
                    branchBound(currentSolution, nextUmpire, nextRound);
                }
                else {
                    // check if all team-venues are visited by every umpire
                    boolean feasible = true;
                    for (Umpire u: umpires) if(!u.hasVisitedAllLocations()) {
                        feasible = false;
                        break;
                    }
                    if(feasible) {
                        Solution betterSolution = localSearch(currentSolution);
                        if (upperBound > betterSolution.totalDistance) {
                            best = betterSolution.toString();
                            upperBound = betterSolution.totalDistance;
                            System.out.println("New best solution: " + upperBound);
                            writeSolution("solutions/sol_" + fileName +"_" + q1 + "_" + q2 + ".txt", betterSolution);
//                            System.exit(0);
                        }
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
                umpires[umpire].q1TeamCounter[homeIndex] = previousQ1;
                umpires[umpire].q2TeamCounter[homeIndex] = previousQ2Home;
                umpires[umpire].q2TeamCounter[awayIndex] = previousQ2Away;
            }
        }
    }

    private static int[] getFeasibleAllocations() {
        int[] res = new int[nUmps];
        return res;
    }

    private static Solution localSearch(Solution solution) {
        return solution;
    }

    private static void readInput(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));

        // Read the number of teams
        String line = reader.readLine();
        nTeams = Integer.parseInt(line.split("=")[1].split(";")[0]);
        nUmps = nTeams / 2;
        nRounds = 4 * nUmps - 2;
        if(DEBUG) {
            System.out.println("nTeams: " + nTeams);
            System.out.println("nUmps: " + nUmps);
            System.out.println("nRounds: " + nRounds);
        }

        for (int i=0; i<2; i++) reader.readLine();

        // Read the distance matrix
        dist = new int[nTeams][nTeams];
        for(int i = 0; i < nTeams; i++) {
            line = reader.readLine();
            line = line.split("\\[\\s{1,}")[1];
            line = line.split("]")[0];
            String[] distances = line.split("\\s{1,}");
            for(int j = 0; j < nTeams; j++)
                dist[i][j] = Integer.parseInt(distances[j]);
        }

        // Print the distances
        if(DEBUG) {
            System.out.println("===== DIST =====");
            for(int i = 0; i < nTeams; i++) {
                for(int j = 0; j < nTeams; j++) System.out.print(dist[i][j] + "; ");
                System.out.println();
            }
        }

        for (int i=0; i<3; i++) reader.readLine();

        // Read the opponents matrix
        opponents = new int[nRounds][nTeams];
        games = new Game[nRounds][nUmps];
        for(int i = 0; i < nRounds; i++) {
            int gameCount = 0;
            line = reader.readLine();
            line = line.split("\\[\\s{1,}")[1];
            line = line.split("]")[0];
            String[] splitString = line.split("\\s{1,}");
            for(int j = 0; j < nTeams; j++) {
                int value = Integer.parseInt(splitString[j]);
                opponents[i][j] = value;
                if (value > 0) {
                    games[i][gameCount++] = new Game(j + 1, value);
                }
            }
        }

        // Print the opponents
        if(DEBUG) {
            System.out.println("===== Opponents =====");
            for(int i = 0; i < nRounds; i++) {
                for(int j = 0; j < nTeams; j++) System.out.print(opponents[i][j] + "; ");
                System.out.println();
            }

            System.out.println("===== Games =====");
            for(int i = 0; i < nRounds; i++) {
                for(int j = 0; j < nUmps; j++) System.out.print(games[i][j] + "; ");
                System.out.println();
            }
        }
    }
    private static void processGames() {
        for(int round = 1; round < nRounds; round++) {
            for(int currentGame = 0; currentGame < nUmps; currentGame++) {
                for(int nextGame = 0; nextGame < nUmps; nextGame++) {
                    games[round - 1][currentGame].distancesToNext[nextGame] = dist[games[round - 1] [currentGame].home-1][games[round][nextGame].home-1];
                }

                int finalRound = round;
                int finalCurrentGame = currentGame;
                Arrays.sort(games[round-1][currentGame].nextGames, Comparator.comparingInt((Integer game) -> games[finalRound - 1][finalCurrentGame].distancesToNext[game]));
            }
        }
    }

    private static void writeSolution(String fileName, Solution sol) {
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.print(sol);
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}