import java.util.ArrayList;

public class Peer {
    // Class Representing Peer
    public int peerID;
	public String peerAddress;
	public int peerPort;
	public Boolean peerHasFile;
	public BitFieldArray bitfield;

	public Peer(String pId, String pAddress, String pPort, String hasFile, int bitFieldLength) {
		this.peerID = Integer.parseInt(pId);
		this.peerAddress = pAddress;
		this.peerPort = Integer.parseInt(pPort);
        this.peerHasFile = (Integer.parseInt(hasFile) == 1);
		initBitField(bitFieldLength);
	}

	public void initBitField(int length) {
		this.bitfield = new BitFieldArray(length, peerHasFile);
	}

    public Boolean hasAnyPiece() {
		// Checks if Peer has at least one piece
        for (BitField  b : this.bitfield.fields) {
			if (b.data != 0) {
				return true;
            }
        }
        return false;
    }
	
	public ArrayList<Integer> bitfieldArrayDiff(BitField[] peerArr) {
		// Returns array of pieces that neighbor peer has that current peer doesn't
		ArrayList<Integer> hasPieces = new ArrayList<Integer>();
		
		for (BitField  b : this.bitfield.fields) {
			for (BitField  p : peerArr) {
				if (b.data == 0 && p.data != 0) {
					hasPieces.add(b.id);
				}
			}
		}

		return hasPieces;
	}
}
