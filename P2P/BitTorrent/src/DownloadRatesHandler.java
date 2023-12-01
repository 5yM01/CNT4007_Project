import java.util.ArrayList;
import java.util.HashSet;

public class DownloadRatesHandler {
    private ArrayList<RatePair> pairs = new ArrayList<>();

    public class RatePair {
        Integer id;
        Float rate;

        public RatePair() {}
        
        public RatePair(Integer _a, Float _b) {
            this.id = _a;
            this.rate = _b;
        }

        public Integer getId() {
            return id;
        }

        public Float getRate() {
            return rate;
        }
    }

    public void add_rate(Integer pID, Float rate) {
        pairs.add(get_index(rate), new RatePair(pID, rate));
    }

    private int get_index(Float rate) {
        int i = 0;
        for (RatePair rp : pairs) {
            if (Float.compare(rate, rp.getRate()) == 0 && Client_Utils.randomValue(2) == 1) {
                return i;
            } else if (Float.compare(rate, rp.getRate()) > 0) {
                return i;
            }
            i++;
        }
        return i;
    }

    public HashSet<Integer> getPreferredNeighbors(Integer num, HashSet<Integer> interested) {
        HashSet<Integer> pref = new HashSet<>();

        int prefSize = Math.min(pairs.size(), num);

        int i = 0, j = 0, currID = 0;
        while (i < prefSize && j < pairs.size()) {
            currID = pairs.get(j++).id;
            if (interested.contains(currID)) {
                pref.add(currID);
                i++;
            }
        }

        return pref;
    }

    public void clear() {
        pairs.clear();
    }
}
