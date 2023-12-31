import java.io.Serializable;
import java.util.HashSet;

public class BitFieldArray implements Serializable {
    private static final long serialVersionUID = 1004L;

    // Class Representing Peer Bitfield Array
    public BitField[] data_fields;
    public int totalLength;

    public BitFieldArray(){};

    public BitFieldArray(int size) {
        this.data_fields = new BitField[size];
        this.totalLength = size;
        setFields();
    }

    // Initializes Fields of Array
    public void setFields() {
        for (int i = 0; i < this.totalLength; i++) {
            this.data_fields[i] = new BitField(new byte[Client_Utils.getPieceSize()], i);
        }
    }

    // Calculates Current Array Size
    public int currSize() {
        int curr = 0;

        for (BitField  b : this.data_fields) {
            if (!b.empty) {
                curr += 1;
            };
        }

        return curr;
    }

    // Sets Piece in Array
    public void setArrayPiece(BitField bf) {
        this.data_fields[bf.id].setPiece(bf.data, bf.length);
    }

    // Determines Pieces Peer Needs
    public HashSet<Integer> piecesNeeded() {
        HashSet<Integer> counter = new HashSet<Integer>();
        for (BitField  b : this.data_fields) {
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
	public HashSet<Integer> retainAll(int[] peerArr) {
		HashSet<Integer> hasPieces = new HashSet<Integer>();
        BitField b;
        int p;
		
        for (int i = 0; i < this.totalLength; i++) {
            b = this.data_fields[i];
            p = peerArr[i];

            if (b.empty && p != 0) {
                hasPieces.add(i);
            }
        }

		return hasPieces;
	}
}