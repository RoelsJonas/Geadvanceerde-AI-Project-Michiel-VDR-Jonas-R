public class Umpire {
    public int index;
    public int[] q1TeamCounter;
    public int[] q2TeamCounter;

    public Umpire(int ind) {
        index = ind;
        q1TeamCounter = new int[Main.nTeams];
        q2TeamCounter = new int[Main.nTeams];
        for (int i=0; i<Main.nTeams; i++) {
            q1TeamCounter[i] = Integer.MAX_VALUE;
            q2TeamCounter[i] = Integer.MAX_VALUE;
        }

    }
}
