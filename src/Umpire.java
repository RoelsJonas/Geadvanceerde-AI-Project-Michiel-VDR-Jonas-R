public class Umpire {
    public int index;
    public int[] q1TeamCounter;
    public int[] q2TeamCounter;

    public Umpire(int ind) {
        index = ind;
        q1TeamCounter = new int[Main.nTeams];
        q2TeamCounter = new int[Main.nTeams];
        for (int i=0; i<Main.nTeams; i++) {
            q1TeamCounter[i] = Integer.MIN_VALUE;
            q2TeamCounter[i] = Integer.MIN_VALUE;
        }
    }

    public boolean hasVisitedAllLocations() {
        for (int i=0; i<Main.nTeams; i++) {
            if (q1TeamCounter[i] < 0){
                return false;
            }
        }
        return true;
    }
}
