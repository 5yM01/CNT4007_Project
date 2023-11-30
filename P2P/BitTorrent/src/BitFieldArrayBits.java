import java.io.Serializable;
import java.util.HashSet;

public class BitFieldArrayBits implements Serializable {
    public int[] fields;
    public int totalLength;

    public BitFieldArrayBits(int size) {
        this.totalLength = size;
        this.fields = new int[this.totalLength];
    }

    // Calculates Current Array Size
    public int currSize() {
        int curr = 0;

        for (int b : this.fields) {
            if (b == 1) {
                curr += 1;
            };
        }

        return curr;
    }

    // Sets Piece in Array
    public void setArrayPiece(int index) {
        this.fields[index] = 1;
    }

    // Determines Pieces Peer Needs
    public HashSet<Integer> piecesNeeded() {
        HashSet<Integer> counter = new HashSet<Integer>();
        for (int i = 0; i < this.totalLength; i++) {
            if (this.fields[i] == 0) {
                counter.add(i);
            }
        }

        return counter;
    }

    // Returns Pieces Currently In Array
    public HashSet<Integer> currentPieces() {
        HashSet<Integer> counter = new HashSet<Integer>();
        for (int i = 0; i < this.totalLength; i++) {
            counter.add(i);
        }
        counter.removeAll(this.piecesNeeded());
        return counter;
    }

	// Returns set of pieces that neighbor peer has that current peer doesn't
	public HashSet<Integer> retainAll(int[] peerArr) {
		HashSet<Integer> hasPieces = new HashSet<Integer>();
        int b, p;
		
        for (int i = 0; i < this.totalLength; i++) {
            b = this.fields[i];
            p = peerArr[i];

            if (b == 0 && p != 0) {
                hasPieces.add(i);
            }
        }

		return hasPieces;
	}
}
