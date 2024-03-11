import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class Main {
    // CONSTANTS
    private static final boolean DEBUG = true;


    // VARIABLES
    public static int nTeams;
    public static int nUmps;
    public static int nRounds;
    public static Solution best;
    public static int upperBound = Integer.MAX_VALUE;
    public static int lowerBound = 0;
    public static int q1 = 2;  // umpire not in venue for q1 consecutive rounds
    public static int q2 = 2;  // umpire not for same team in q2 consecutive rounds


    public static int[][] dist;
    public static int[][] opponents;
    public static Game[][] games;

    public static void main(String[] args) throws Exception {

        // Open the file
        String fileName = "instances/umps8.txt";
        readInput(fileName);
        processGames();
        best = new Solution();

        Solution currentSolution = new Solution();
        currentSolution.totalDistance = 0;
    }

    private static void branchBound(Solution currentSolution, int umpire, int round) {
        int nextUmpire = ++umpire % nUmps;
        int nextRound = umpire == nUmps ? ++round : round;
        int[] feasibleNextGames = getFeasibleAllocations();
        for(int game : feasibleNextGames) {
            if (game < 0) continue;
            int cost = currentSolution.calculateDistance(round, umpire, game);
            if (currentSolution.totalDistance + cost + lowerBound < upperBound) {  // todo: in aparte methode?
                currentSolution.addGame(round, umpire, game, cost);
                if (nextRound < nRounds) {
                    branchBound(currentSolution, nextUmpire, nextRound);
                }
                else {
                    Solution betterSolution = localSearch(currentSolution);
                    if (best.totalDistance > betterSolution.totalDistance) {
                        best = betterSolution;
                        upperBound = best.totalDistance;
                    }
                }
                currentSolution.removeGame(round, umpire, game, cost);
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
}