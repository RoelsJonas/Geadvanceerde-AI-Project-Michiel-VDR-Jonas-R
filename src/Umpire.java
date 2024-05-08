public class Umpire {
    public int index;
    public int[] q1TeamCounter;
    public int[] q2TeamCounter;
    public int[] q1TeamCounterLB;
    public int[] q2TeamCounterLB;

    public Umpire(int ind) {
        index = ind;
        q1TeamCounter = new int[Main.nTeams];
        q2TeamCounter = new int[Main.nTeams];
        q1TeamCounterLB = new int[Main.nTeams];
        q2TeamCounterLB = new int[Main.nTeams];
        for (int i=0; i<Main.nTeams; i++) {
            q1TeamCounter[i] = Integer.MIN_VALUE;
            q2TeamCounter[i] = Integer.MIN_VALUE;
            q1TeamCounterLB[i] = Integer.MIN_VALUE;
            q2TeamCounterLB[i] = Integer.MIN_VALUE;
        }
    }

    public boolean hasVisitedAllLocations(boolean realdeal) {
        if (realdeal) {
            for (int i=0; i<Main.nTeams; i++) {
                if (q1TeamCounter[i] < 0){
                    return false;
                }
            }
        }
        else {
            for (int i=0; i<Main.nTeams; i++) {
                if (q1TeamCounter[i] < 0){
                    return false;
                }
            }
        }
        return true;
    }

    public int countVisitedLocations(boolean realdeal) {
        int count = 0;
        if (realdeal) {

            for (int i=0; i<Main.nTeams; i++) {
                if (q1TeamCounter[i] >= 0){
                    count++;
                }
            }
        }
        else {
            for (int i=0; i<Main.nTeams; i++) {
                if (q1TeamCounterLB[i] >= 0){
                    count++;
                }
            }
        }
        return count;
    }
}
