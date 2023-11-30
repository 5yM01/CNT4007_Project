import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Peer {
    // Class Representing Peer
    public int peerID;
	public String peerAddress;
	public int peerPort;
	public Boolean peerHasFile;
	public BitFieldArray bitfield;
	public BitFieldArrayBits bitfield_bits;
	public String logPath;

	// Peer Constructor
	public Peer(String pId, String pAddress, String pPort, String hasFile, int bitFieldLength) {
		this.peerID = Integer.parseInt(pId);
		this.peerAddress = pAddress;
		this.peerPort = Integer.parseInt(pPort);
        this.peerHasFile = (Integer.parseInt(hasFile) == 1);
		this.logPath = "log_peer_" + pId + ".log";
		initBitField(bitFieldLength);
	}

	// Creates Bitfield for Peer
	public void initBitField(int length) {
		this.bitfield = new BitFieldArray(length);
		this.bitfield_bits = new BitFieldArrayBits(length);
	}

	// Checks if Peer has at least One Piece
    public Boolean hasAtLeastOnePiece() {
        for (BitField  b : this.bitfield.data_fields) {
			if (!b.empty) {
				return true;
            }
        }
        return false;
    }

	// Writes Logging Message To Log File
    public void writeToLog(String log) {
        try {
			PeerLog.set_lock();
            FileWriter writer = new FileWriter(this.logPath, true);
            String message = "[" + Client_Utils.getDateTime() + "]: " + log + "\n";
            writer.write(message);
            writer.close();
        } catch (IOException e) {
			System.out.println("An error occurred.");
            e.printStackTrace();
        } finally {
			PeerLog.release_lock();
		}
    }

	// Imports Data From File
	public void importFile(String fname) {
		String path = fname;
		// String path = peerID + "/" + fname;
		byte[] dataArr = Client_Utils.getFileBytes(path);
		byte[] currPiece;

		for (int i=0, start=0; start < Client_Utils.getFileSize(); i++, start += Client_Utils.getPieceSize()) {
			currPiece = Arrays.copyOfRange(dataArr, start, start + Client_Utils.getPieceSize());
			this.bitfield.data_fields[i].setPiece(currPiece, currPiece.length);
			this.bitfield_bits.setArrayPiece(i);
		}
	}
}
