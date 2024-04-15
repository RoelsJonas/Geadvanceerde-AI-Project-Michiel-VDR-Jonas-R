import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class Main {
    // VALIDATE THE SOLUTION USING  java -jar validator.jar .\\instances\\umps8.txt 2 2 .\\solutions\\sol_umps8_2_2.txt
    // CONSTANTS
    public static final boolean DEBUG = true;
    public static final boolean HUNGARIAN_EN = false;


    // VARIABLES
    public static int nTeams;
    public static int nUmps;
    public static int nRounds;
    public static String best = "No solution found";
    public static int upperBound = Integer.MAX_VALUE;
    public static int q1 = 5;  // umpire not in venue for q1 consecutive rounds
    public static int q2 = 2;  // umpire not for same team in q2 consecutive rounds


    public static int[][] dist;
    public static int[][] opponents;
    public static Game[][] games;
    public static Umpire[] umpires;
    public static int[][] sol_subProblems;
    public static int[][] lowerbounds;
    public static String fileName;
    public static void main(String[] args) throws Exception {

        // Open the file
        fileName = "umps10A";
        readInput("instances/" + fileName + ".txt");
        processGames();
//        best = new Solution();

        Solution currentSolution = new Solution();
        currentSolution.totalDistance = 0;

        calculateLowerBounds();

        // print the lowerbounds matrix
        for (int i=0; i<nRounds; i++) {
            for (int j=0; j<nRounds; j++) {
                System.out.print(lowerbounds[i][j] + " ");
            }
            System.out.println();
        }

        // Fix the first round
        for(int i = 0; i < nUmps; i++) {
            int homeIndex = Main.games[0][i].home-1;
            int awayIndex = Main.games[0][i].away-1;
            currentSolution.addGame(0, i, i, 0);
            Main.umpires[i].q1TeamCounter[homeIndex] = 0;
            Main.umpires[i].q2TeamCounter[homeIndex] = 0;
            Main.umpires[i].q2TeamCounter[awayIndex] = 0;
        }


        BranchAndBound.startTime = System.currentTimeMillis();
        BranchAndBound.branchBound(currentSolution, 0, 1);
//        System.out.println(best);
        System.out.println("Visited Nodes: " + BranchAndBound.nodeCounter);
    }



    private static void calculateLowerBounds() {
        sol_subProblems = new int[nRounds][nRounds];
        lowerbounds = new int[nRounds][nRounds];
        for(int r=nRounds - 2; r>=0; r--) {
            sol_subProblems[r][r+1] = HungarianAlgorithm.hungarianAlgo(r);
            for (int r2=r+1; r2<nRounds; r2++) {
                lowerbounds[r][r2] = sol_subProblems[r][r+1] + lowerbounds[r+1][r2];
            }
        }
        Solution a_solution = new Solution();
        for(int k=2; k<nRounds; k++) {
            int r = nRounds - 1 - k;
            while (r >= 1) {
                for (int rr = r+k-2; rr <= r; rr++)
                    if(sol_subProblems[rr][r+k] == 0) {
                        BranchAndBound.subResult = Integer.MAX_VALUE;
                        sol_subProblems[rr][r+k] = BranchAndBound.subBranchBound(a_solution, 0, rr, r+k);
                        for(int r1 = rr; r1 > 0; r1--) {
                            for (int r2 = r+k; r2 < nRounds; r2++) {
                                lowerbounds[r1][r2] = Math.max(lowerbounds[r1][r2],
                                        lowerbounds[r1][rr]+sol_subProblems[rr][r+k]+lowerbounds[r+k][r2]);
                            }
                        }
                }
                r -= k;
            }
        }
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

    public static void writeSolution(String fileName, Solution sol) {
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.print(sol);
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}