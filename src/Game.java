public class Game {
    public int home;
    public int away;
    public int[] distancesToNext;
    public Integer[] nextGames;

    public Game(int home, int away) {
        this.home = home;
        this.away = away;
        this.distancesToNext = new int[Main.nUmps];
        this.nextGames = new Integer[Main.nUmps];
        for(int i = 0; i < Main.nUmps; i++) nextGames[i] = i;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Node{");
        sb.append("Game=").append(home);
        sb.append(", away=").append(away);
        sb.append('}');
        return sb.toString();
    }
}
