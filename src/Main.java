import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.*;

public class Main {
    // VALIDATE THE SOLUTION USING  java -jar validator.jar .\\instances\\umps8.txt 2 2 .\\solutions\\sol_umps8_2_2.txt
    // CONSTANTS
    public static final boolean DEBUG = true;
    public static final boolean PARTIAL_MATCH_EN = true;
    public static final boolean GREEDY_EN = true;
    public static final boolean HUNGARIAN_EN = false;
    public static final boolean SORT_ALLOCATIONS_EN = false;
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
    public static int[][] lowerbounds;
    public static int[][] usedBounds;
    public static int[][] partialBounds;
    public static void main(String[] args) throws Exception {
        BranchAndBound.startTime = System.currentTimeMillis();

        // Open the file
//        fileName = "umps14";
        readInput("instances/" + fileName + ".txt");
        processGames();

        Solution currentSolution = new Solution();
        currentSolution.totalDistance = 0;

        calculatePartialBounds();

        calculateLowerBounds();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Future<?> future = executor.submit(() -> calculateLowerBounds());
        try {
            Thread.sleep(500); // Sleep for 10 seconds (10,000 milliseconds)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //calculateLowerBounds();

        // reset counters
//        for(int i = 0; i < nUmps; i++) {
//            for(int j = 0; j < nTeams; j++) {
//                umpires[i].q1TeamCounter[j] = Integer.MIN_VALUE;
//                umpires[i].q2TeamCounter[j] = Integer.MIN_VALUE;
//            }
//        }



        // Fix the first round
        for(int i = 0; i < nUmps; i++) {
            int homeIndex = Main.games[0][i].home-1;
            int awayIndex = Main.games[0][i].away-1;
            currentSolution.addGame(0, i, i, 0);
            Main.umpires[i].q1TeamCounter[homeIndex] = 0;
            Main.umpires[i].q2TeamCounter[homeIndex] = 0;
            Main.umpires[i].q2TeamCounter[awayIndex] = 0;
        }

        BranchAndBound.branchBound(currentSolution, 0, 1);
        try {
            future.get(); // This will block until the calculation is complete
            if (DEBUG){
                 //print the lowerbounds matrix
                for (int i=0; i<nRounds; i++) {
                    for (int j=0; j<nRounds; j++) {
                        System.out.print(lowerbounds[i][j] + " ");
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
        System.out.println("Visited Nodes: " + BranchAndBound.nodeCounter + ", in: " + (System.currentTimeMillis() - BranchAndBound.startTime) + " ms");

//        if (WRITE_LOGS) {
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter("analysis/Prune_Types/U14_7_2.log"))) {
//                for(Integer r : BranchAndBound.firstPrunes.keySet()) {
//                    writer.write("\t round: " + r + ", prunes: " + BranchAndBound.firstPrunes.get(r) + ", secondary prunes: " + BranchAndBound.secondPrunes.getOrDefault(r, 0L) + ", " + ((double) BranchAndBound.secondPrunes.getOrDefault(r, 0L) / (BranchAndBound.firstPrunes.get(r) + BranchAndBound.secondPrunes.getOrDefault(r, 0L)))+"\n");
//                }
//                writer.write("Total: " + HungarianAlgorithm.partialCount.size());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter("analysis/Partial_Counts/U14_7_2.log"))) {
//                for(Integer hashKey: HungarianAlgorithm.partialCount.keySet()) {
//                    writer.write(hashKey + " " + HungarianAlgorithm.partialCount.get(hashKey) + "\n");
//                }
//                writer.write("Total: " + HungarianAlgorithm.partialCount.size());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter("analysis/Hung_Greedy_mem/U14_7_2.log"))) {
//                for(Integer hashKey: HungarianAlgorithm.greedyMemory.keySet()) {
//                    writer.write(hashKey + " Greedy: " + HungarianAlgorithm.greedyMemory.get(hashKey) + " Hungarian: " + HungarianAlgorithm.hungMemory.get(hashKey) +"\n");
//                }
//                writer.write("Total: " + HungarianAlgorithm.greedyMemory.size());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
}

//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("analysis/Prune_Types/U12_5_3.log"))) {
//            for(Integer r : BranchAndBound.firstPrunes.keySet()) {
//                writer.write("\t round: " + r + ", prunes: " + BranchAndBound.firstPrunes.get(r) + ", secondary prunes: " + BranchAndBound.secondPrunes.getOrDefault(r, 0L) + ", " + ((double) BranchAndBound.secondPrunes.getOrDefault(r, 0L) / (BranchAndBound.firstPrunes.get(r) + BranchAndBound.secondPrunes.getOrDefault(r, 0L)))+"\n");
//            }
//            writer.write("Total: " + HungarianAlgorithm.partialCount.size());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("analysis/Partial_Counts/U12_5_3.log"))) {
//            for(Integer hashKey: HungarianAlgorithm.partialCount.keySet()) {
//                writer.write(hashKey + " " + HungarianAlgorithm.partialCount.get(hashKey) + "\n");
//            }
//            writer.write("Total: " + HungarianAlgorithm.partialCount.size());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("analysis/Hung_Greedy_mem/U12_5_3.log"))) {
//            for(Integer hashKey: HungarianAlgorithm.greedyMemory.keySet()) {
//                writer.write(hashKey + " Greedy: " + HungarianAlgorithm.greedyMemory.get(hashKey) + " Hungarian: " + HungarianAlgorithm.hungMemory.get(hashKey) +"\n");
//            }
//            writer.write("Total: " + HungarianAlgorithm.greedyMemory.size());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
}
    private static void calculatePartialBounds() {
        partialBounds = new int[nRounds][nUmps];
        int[] mins = new int[nUmps];
        int min = Integer.MAX_VALUE;
        for(int round = 1; round < nRounds; round++) {
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
        }
        if(DEBUG) {
            System.out.println("============== Partial Bounds ==============");
            for(int i = 0; i < nRounds; i++) {
                for(int j = 0; j < nUmps; j++) {
                    System.out.printf("%d,", partialBounds[i][j]);
                }
                System.out.println();
            }
        }
    }

    private static void calculateLowerBounds() {
        sol_subProblems = new int[nRounds][nRounds];
        lowerbounds = new int[nRounds][nRounds];
        usedBounds = new int[nRounds][nRounds];
        for(int r=nRounds - 2; r>=0; r--) {
            sol_subProblems[r][r+1] = HungarianAlgorithm.hungarianAlgo(r);
            for (int r2=r+1; r2<nRounds; r2++) {
                lowerbounds[r][r2] = sol_subProblems[r][r+1] + lowerbounds[r+1][r2];
            }
        }
        Solution a_solution = new Solution();
        for(int k=2; k<nRounds; k++) {
            int r = nRounds - 1 - k;
//            System.out.println("====== k: " + k +", r: " + r + " ======");
            while (r >= 1) {  // >1
//                System.out.println("rr: " + (r+k-2) + ", r: " + r);
                for (int rr = r+k-2; rr >= r; rr--) // rr=r+k-1 en rr>r+1 rr--
                    if(sol_subProblems[rr][r+k] == 0)
                    {
                        BranchAndBound.subResult = Integer.MAX_VALUE;
                        a_solution = new Solution();
                        // Fix the first round
                        for(int i = 0; i < nUmps; i++) {
                            int homeIndex = Main.games[rr][i].home-1;
                            int awayIndex = Main.games[rr][i].away-1;
                            a_solution.addGame(rr, i, i, 0);
                            Main.umpires[i].q1TeamCounterLB[homeIndex] = rr;
                            Main.umpires[i].q2TeamCounterLB[homeIndex] = rr;
                            Main.umpires[i].q2TeamCounterLB[awayIndex] = rr;
                        }
//                        System.out.println("rr: " + rr + ", r+k:" + (r+k));
                        sol_subProblems[rr][r+k] = BranchAndBound.subBranchBound(a_solution, 0, rr+1, rr+0, r+k);
//                        System.out.println("rr: " + rr + ", r: " + r + ", k: " + k + ", sol:" + sol_subProblems[rr][r+k]);
//                        for (int i=0; i<nRounds; i++) {
//                            for (int j=0; j<nRounds; j++) {
//                                System.out.print(sol_subProblems[i][j] + " ");
//                            }
//                            System.out.println();
//                        }

                        // reset counters
                        for(int i = 0; i < nUmps; i++) {
                            for(int j = 0; j < nTeams; j++) {
                                umpires[i].q1TeamCounterLB[j] = Integer.MIN_VALUE;
                                umpires[i].q2TeamCounterLB[j] = Integer.MIN_VALUE;
                            }
                        }
                        for(int r1 = rr; r1 >= 0; r1--) {
                            for (int r2 = r+k; r2 < nRounds; r2++) {
//                                if(lowerbounds[r1][r2] < lowerbounds[r1][rr]+sol_subProblems[rr][r+k]+lowerbounds[r+k][r2])
//                                    System.out.println("Better bound found");
                                lowerbounds[r1][r2] = Math.max(lowerbounds[r1][r2],
                                        lowerbounds[r1][rr]+sol_subProblems[rr][r+k]+lowerbounds[r+k][r2]);
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
                }

                int finalRound = round;
                int finalCurrentGame = currentGame;
                Arrays.sort(games[round-1][currentGame].nextGames, Comparator.comparingInt((Integer game) -> games[finalRound - 1][finalCurrentGame].distancesToNext[game]));
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