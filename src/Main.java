import java.io.*;

public class Main {
    // CONSTANTS
    private static final boolean DEBUG = true;


    // VARIABLES
    private static int nTeams;
    private static int[][] dist;
    private static int[][] opponents;

    public static void main(String[] args) throws Exception {

        // Open the file
        String fileName = "instances/umps8.txt";
        BufferedReader reader = new BufferedReader(new FileReader(fileName));

        // Read the number of teams
        String line = reader.readLine();
        nTeams = Integer.parseInt(line.split("=")[1].split(";")[0]);
        if(DEBUG) System.out.println("nTeams: " + nTeams);

        for (int i=0; i<2; i++) reader.readLine();

        // Read the distance matrix
        dist = new int[nTeams][nTeams];
        for(int i = 0; i < nTeams; i++) {
            line = reader.readLine();
            line = line.split("\\[\\s{1,}")[1];
            line = line.split("]")[0];
            String[] distances = line.split("\\s{1,}");
            for(int j = 0; j < nTeams; j++)
                System.out.println(distances[j]);
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
        opponents = new int[nTeams][nTeams];
        for(int i = 0; i < nTeams; i++) {
            line = reader.readLine();
            line = line.split("\\[\\s{1,}")[1];
            line = line.split("]")[0];
            String[] games = line.split("\\s{1,}");
            for(int j = 0; j < nTeams; j++)
                opponents[i][j] = Integer.parseInt(games[j]);
        }

        // Print the opponents
        if(DEBUG) {
            System.out.println("===== GAMES =====");
            for(int i = 0; i < nTeams; i++) {
                for(int j = 0; j < nTeams; j++) System.out.print(opponents[i][j] + "; ");
                System.out.println();
            }
        }

    }
}