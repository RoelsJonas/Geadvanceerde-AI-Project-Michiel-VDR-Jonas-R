public class Game {
    public int home;
    public int away;
    public int ump; // TODO UMPIRE TOEWIJZEN
    public int[] distancesToNext;
    public int[] distancesToNextHard;
    public Integer[] nextGames;

    public Game(int home, int away) {
        this.home = home;
        this.away = away;
        this.distancesToNext = new int[Main.nUmps];
        this.distancesToNextHard = new int[Main.nUmps];
        this.nextGames = new Integer[Main.nUmps];
        for(int i = 0; i < Main.nUmps; i++) nextGames[i] = i;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Game{");
        sb.append("home=").append(home);
        sb.append(", away=").append(away);
        sb.append('}');
        return sb.toString();
    }
}
