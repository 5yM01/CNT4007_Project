import java.io.Serializable;
import java.util.HashSet;

public class BitFieldArray implements Serializable {
    private static final long serialVersionUID = 1004L;

    // Class Representing Peer Bitfield Array
    public BitField[] fields;
    public int totalLength;

    public BitFieldArray(){};

    public BitFieldArray(int size) {
        this.fields = new BitField[size];
        this.totalLength = size;
        setFields();
    }

    // Initializes Fields of Array
    public void setFields() {
        for (int i = 0; i < this.totalLength; i++) {
            this.fields[i] = new BitField(new byte[Client_Utils.getPieceSize()], i);
        }
    }

    // Calculates Current Array Size
    public int currSize() {
        int curr = 0;

        for (BitField  b : this.fields) {
            if (!b.empty) {
                curr += 1;
            };
        }

        return curr;
    }

    // Sets Piece in Array
    public void setArrayPiece(BitField bf) {
        this.fields[bf.id].setPiece(bf.data, bf.length);
    }

    // Determines Pieces Peer Needs
    public HashSet<Integer> piecesNeeded() {
        HashSet<Integer> counter = new HashSet<Integer>();
        for (BitField  b : this.fields) {
            if (b.empty) {
                counter.add(b.id);
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
	public HashSet<Integer> retainAll(BitField[] peerArr) {
		HashSet<Integer> hasPieces = new HashSet<Integer>();
        BitField b, p;
		
        for (int i = 0; i < this.totalLength; i++) {
            b = this.fields[i];
            p = peerArr[i];

            if (b.empty && !p.empty) {
                hasPieces.add(b.id);
            }
        }

		return hasPieces;
	}
}