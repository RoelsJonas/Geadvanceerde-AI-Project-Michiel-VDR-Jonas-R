import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.*;

public class Main {
    // VALIDATE THE SOLUTION USING  java -jar validator.jar .\\instances\\umps8.txt 2 2 .\\solutions\\sol_umps8_2_2.txt
    // CONSTANTS
    public static final boolean DEBUG = true;
    public static final boolean PARTIAL_MATCH_EN = true;
    public static final boolean GREEDY_EN = true;
    public static final boolean HUNGARIAN_EN = false;
    public static final boolean SORT_ALLOCATIONS_EN = true;
    public static final boolean INTERMEDIARY_BOUNDS_EN = true;
    public static final boolean FULL_EXPLORATION_EN = false;
    public static final boolean WRITE_LOGS = true;
    // VARIABLES
    public static int nTeams;
    public static int nUmps;
    public static int nRounds;
    public static String best = "No solution found";
    public static int upperBound = Integer.MAX_VALUE;
    public static String fileName = "umps14";
    public static int q1 = 7;  // umpire not in venue for q1 consecutive rounds
    public static int q2 = 3;  // umpire not for same team in q2 consecutive rounds
    public static int[][] dist;
    public static int[][] opponents;
    public static Game[][] games;
    public static Umpire[] umpires;
    public static int[][] sol_subProblems;
    public static int[][] lowerBounds;
    public static int[][] usedBounds;
    public static int[][] partialBounds;
    public static void main(String[] args) throws Exception {
        BranchAndBound.startTime = System.currentTimeMillis();

        // Open the file
        readInput("instances/" + fileName + ".txt");
        processGames();

        Solution currentSolution = new Solution();
        currentSolution.totalDistance = 0;

        calculatePartialBounds();
        sol_subProblems = new int[nRounds][nRounds];
        lowerBounds = new int[nRounds][nRounds];
        usedBounds = new int[nRounds][nRounds];

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Future<?> future = executor.submit(() -> calculateLowerBounds());

        ExecutorService[] executors = new ExecutorService[nUmps];
        Future<?>[] futures = new Future[nUmps];
        for(int i = 0; i < nUmps; i++) {
            executors[i] = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        // Create nUmps threads and fix the next game for the first umpire
        for(int i = 0; i < nUmps; i++) {
            BranchAndBoundParallel bnbi = new BranchAndBoundParallel();
            // fix the first game of round 1
            int homeIndex = Main.games[1][i].home-1;
            int awayIndex = Main.games[1][i].away-1;
            int cost = bnbi.currentSolution.calculateDistance(1, 0, i);
            bnbi.currentSolution.addGame(1, 0, i, cost);
            bnbi.umpires[0].q1TeamCounter[homeIndex] = 1;
            bnbi.umpires[0].q2TeamCounter[homeIndex] = 1;
            bnbi.umpires[0].q2TeamCounter[awayIndex] = 1;
            futures[i] = executors[i].submit(() -> bnbi.branchBound(1, 1));
        }

        for(int i = 0; i < nUmps; i++) {
            futures[i].get();
            executors[i].shutdown();
        }
        try {
            future.get(); // This will block until the calculation is complete
            if (DEBUG){
                 //print the lowerbounds matrix
                for (int i=0; i<nRounds; i++) {
                    for (int j=0; j<nRounds; j++) {
                        System.out.print(lowerBounds[i][j] + " ");
                    }
                    System.out.println();
                }
                // print the Subresults matrix
                for (int i=0; i<nRounds; i++) {
                    for (int j=0; j<nRounds; j++) {
                        System.out.print(sol_subProblems[i][j] + " ");
                    }
                    System.out.println();
                }
                // print the Usedbounds matrix
                for (int i=0; i<nRounds; i++) {
                    for (int j=0; j<nRounds; j++) {
                        System.out.print(usedBounds[i][j] + " ");
                    }
                    System.out.println();
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        executor.shutdown();


        writeSolution("solutions/sol_" + Main.fileName +"_" + Main.q1 + "_" + Main.q2 + ".txt", best);
        System.out.println("Best solution: " + upperBound);
        System.out.println(best);
        System.out.println("Visited Nodes: " + BranchAndBoundParallel.nodeCounter + ", in: " + (System.currentTimeMillis() - BranchAndBound.startTime) + " ms");

}
    private static void calculatePartialBounds() {
        System.out.println("============== Partial Bounds ==============");
        partialBounds = new int[nRounds][nUmps];
        int[] mins = new int[nUmps];
        int min = Integer.MAX_VALUE;
        for(int round = 1; round < nRounds-1; round++) {
            for(int i = 0; i < nUmps; i++) {
                int[] distances = games[round][i].distancesToNext;
                int val = Integer.MAX_VALUE;
                for(int j = 0; j < nUmps; j++) {
                    if(distances[j] == 0) continue;
                    val = Math.min(val, distances[j]);
                }
                if(val < min) min = val;
                mins[i] = val;
            }
            Arrays.sort(mins);
            for(int i = 1; i < nUmps; i++) {
                partialBounds[round][i] = partialBounds[round][i-1] + mins[i];
            }
            System.out.printf("Round %d: ", round);
            for(int j = 0; j < nUmps; j++) {
                System.out.printf("%d,", partialBounds[round][j]);
            }
            System.out.println();


            int[][] matrix = new int[nUmps][nUmps];
            for(int i = 0; i < nUmps; i++) {
                for(int j = 0; j < nUmps; j++) {
                    int distance = games[round][i].distancesToNextHard[j];
                    if(distance <= 0)
                        matrix[i][j] = 999999;
                    else matrix[i][j] = distance;
                }
            }

            int[] res = HungarianAlgorithm.hungarianAlgoPartial(matrix);
            Arrays.sort(res);
            for(int i = 1; i < nUmps; i++) {
                partialBounds[round][i] = partialBounds[round][i-1] + res[i];
            }
            System.out.printf("Round %d: ", round);
            for(int j = 0; j < nUmps; j++) {
                System.out.printf("%d,", partialBounds[round][j]);
            }
            System.out.println();

        }
//        if(DEBUG) {
//            System.out.println("============== Partial Bounds ==============");
//            for(int i = 0; i < nRounds; i++) {
//                for(int j = 0; j < nUmps; j++) {
//                    System.out.printf("%d,", partialBounds[i][j]);
//                }
//                System.out.println();
//            }
//        }
    }

    // TODO IMPLEMENT INTERMEDIARY BOUNDS PROPAGATION
    private static void calculateLowerBounds() {
        for(int r=nRounds - 2; r>=0; r--) {
            sol_subProblems[r][r+1] = HungarianAlgorithm.hungarianAlgo(r);
            for (int r2=r+1; r2<nRounds; r2++) {
                lowerBounds[r][r2] = sol_subProblems[r][r+1] + lowerBounds[r+1][r2];
            }
        }
        for(int k=2; k<nRounds; k++) {

            // propagate bounds upwards
            if(INTERMEDIARY_BOUNDS_EN) {
                for(int i = nRounds - 2; i >= 0; i--) for(int j = nRounds - 1; j >= 0; j--) {
                    lowerBounds[i][j] = Math.max(lowerBounds[i][j], lowerBounds[i+1][j]);
                }
            }
            int r = nRounds - 1 - k;
            while (r >= 1) {  // >1
                for (int rr = r+k-2; rr >= r; rr--) // rr=r+k-1 en rr>r+1 rr--
                    if(sol_subProblems[rr][r+k] == 0)
                    {
                        Umpire[] subUmps = new Umpire[nUmps];
                        for(int i = 0; i < nUmps; i++) {
                            subUmps[i] = new Umpire(i);
                            subUmps[i].q1TeamCounterLB = umpires[i].q1TeamCounterLB.clone();
                            subUmps[i].q2TeamCounterLB = umpires[i].q2TeamCounterLB.clone();
                        }
                        Solution a_solution = new Solution();
                        for(int i = 0; i < nUmps; i++) {
                            int homeIndex = Main.games[rr][i].home-1;
                            int awayIndex = Main.games[rr][i].away-1;
                            a_solution.addGame(rr, i, i, 0);
                            subUmps[i].q1TeamCounterLB[homeIndex] = rr;
                            subUmps[i].q2TeamCounterLB[homeIndex] = rr;
                            subUmps[i].q2TeamCounterLB[awayIndex] = rr;
                        }
                        BranchAndBoundParallel subBnB = new BranchAndBoundParallel(a_solution, subUmps);
                        sol_subProblems[rr][r+k] = subBnB.subBranchBound(0, rr+1, rr+0, r+k);
                        for(int r1 = rr; r1 >= 0; r1--) {
                            for (int r2 = r+k; r2 < nRounds; r2++) {
                                lowerBounds[r1][r2] = Math.max(lowerBounds[r1][r2],
                                        lowerBounds[r1][rr]+sol_subProblems[rr][r+k]+ lowerBounds[r+k][r2]);
                            }
                        }
                }
                if(FULL_EXPLORATION_EN) r -= 1;
                else r -= k;
            }
        }
        System.out.println("Lower bounds calculated");
    }

    private static void readInput(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));

        // Read the number of teams
        String line = reader.readLine();
        nTeams = Integer.parseInt(line.split("=")[1].split(";")[0]);
        nUmps = nTeams / 2;
        umpires = new Umpire[nUmps];
        for(int u=0; u<nUmps; u++) umpires[u] = new Umpire(u);
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
            line = line.split("\\[\\s{0,}")[1];
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
                    games[round - 1][currentGame].distancesToNextHard[nextGame] = dist[games[round - 1] [currentGame].home-1][games[round][nextGame].home-1];
                }

                int finalRound = round;
                int finalCurrentGame = currentGame;
                Arrays.sort(games[round-1][currentGame].nextGames, Comparator.comparingInt((Integer game) -> games[finalRound - 1][finalCurrentGame].distancesToNext[game]));
                System.out.print("Distances: ");
                for(int i = 0 ; i < games[round-1][currentGame].nextGames.length; i++) {
                    System.out.printf("%d, ", games[finalRound - 1][finalCurrentGame].distancesToNext[games[round-1][currentGame].nextGames[i]]);
                }
                System.out.println();

            }
        }
    }

    public static void writeSolution(String fileName, String sol) {
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.print(sol);
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}