public class Solution {
    public int[][] sol;
    public int totalDistance;
    public int nAllocatedGames;

    public Solution() {
        sol = new int[Main.nRounds][Main.nUmps];
        for (int i=0; i<Main.nRounds; i++)
            for (int j=0; j<Main.nUmps; j++)
                sol[i][j] = -1;

        totalDistance = Integer.MAX_VALUE;
    }

    public int getDistance(int round, int ump) {
        if(round > 0)
            return Main.dist[Main.games[round - 1][sol[round - 1][ump]].home - 1][Main.games[round][sol[round][ump]].home - 1];
        else return 0;
    }

    public int calculateDistance(int round, int ump, int game) {
        if (round > 0)
            return Main.dist[Main.games[round - 1][sol[round - 1][ump]].home - 1][Main.games[round][game].home - 1];
        else return 0;
    }

    public void addGame(int round, int umpire, int game, int cost) {
        totalDistance += cost;
        sol[round][umpire] = game;
    }

    public void removeGame(int round, int umpire, int game, int cost) {
        sol[round][umpire] = -1;
        totalDistance -= cost;
    }

}