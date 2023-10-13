import java.io.Serializable;

public class Handshake_Msg implements Message, Serializable {
    private String header = "P2PFILESHARINGPROJ";
    private int zeroBits;
    private int peerID;

    public Handshake_Msg() {}

    public Handshake_Msg(int id) {
        this.peerID = id;
    }

    public void print() {
        System.out.println("Header: " + header + "\nID: " + peerID);
    }

    public Boolean check(int id) {
        return this.peerID == id;
    }
}
