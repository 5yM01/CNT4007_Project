import java.io.Serializable;

public class Handshake_Msg implements Message, Serializable {
    // Class Representing Handshake Message
    private String header = "P2PFILESHARINGPROJ";
    private byte[] zeroBits = new byte[10];
    private int peerID;

    public Handshake_Msg() {}

    public Handshake_Msg(int id) {
        this.peerID = id;
    }

    public void print() {
        System.out.println("Header: " + header + "\nID: " + peerID);
    }

    public Boolean checkHeader() {
        return this.header.equals("P2PFILESHARINGPROJ");
    }

    public Boolean checkHS(int id) {
        return this.peerID == id || this.checkHeader();
    }

    public int getPeerID() {
        return peerID;
    }
}
