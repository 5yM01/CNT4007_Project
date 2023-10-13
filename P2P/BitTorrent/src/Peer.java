import java.util.ArrayList;

public class Peer {
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
        for (BitField  b : this.bitfield.fields) {
			if (b.data != 0) {
				return true;
            }
        }
        return false;
    }
	
	public ArrayList<Integer> bitfieldArrayDiff(BitField[] peerArr) {
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
