import java.io.Serializable;

public class Handshake_Msg implements Message, Serializable {
    private static final long serialVersionUID = 1001L;

    // Class Representing Handshake Message
    private String header = "P2PFILESHARINGPROJ";
    private byte[] zeroBits;
    private int peerID;

    private int msg_length;

    // Constructor For Handshake Message
    public Handshake_Msg(int id) {
        this.peerID = id;
        this.zeroBits = new byte[10];
        setLength();
    }

    public int getPeerID() {
        return peerID;
    }

    public Integer getLength() {
        return this.msg_length;
    }

    public void setLength() {
        this.msg_length = header.length() + zeroBits.length + 4;
    }

    public Boolean checkHeader() {
        return this.header.equals("P2PFILESHARINGPROJ");
    }

    public Boolean checkHS(int id) {
        return this.peerID == id && this.checkHeader();
    }
}
