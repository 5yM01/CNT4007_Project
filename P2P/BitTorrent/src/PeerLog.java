import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class PeerLog {
    private static Semaphore semaphore = new Semaphore(1);

    private static String makes_connection = "Peer %d makes a connection to Peer %d.";
    private static String is_connected = "Peer %d is connected from Peer %d.";
    private static String preferred_neighbor = "Peer %d has the preferred neighbors %s.";
    private static String optimistically_unchoked = "Peer %d has the optimistically unchoked neighbor %d.";
    private static String unchoke = "Peer %d is unchoked by %d.";
    private static String choke = "Peer %d is choked by %d.";
    private static String have = "Peer %d received the 'have' message from %d for the piece %d.";
    private static String interested = "Peer %d received the 'interested' message from %d.";
    private static String not_interested = "Peer %d received the 'not interested' message from %d.";
    private static String downloaded_piece = "Peer %d has downloaded the piece %d from %d. Now the number of pieces it has is %d.";
    private static String complete = "Peer %d has downloaded the complete file.";

    public static String log_connected_to(int pID1, int pID2) {
        return String.format(makes_connection, pID1, pID2);
    }

    public static String log_connected_from(int pID1, int pID2) {
        return String.format(is_connected, pID1, pID2);
    }

    public static String log_Preferred_neighbor(Peer p1, ArrayList<Integer> list) {
        String prefList = "";
        for (Integer id : list) {
            prefList += id + ", ";
        }
        prefList = prefList.substring(0, prefList.length() - 2);

        return String.format(preferred_neighbor, p1.peerID, prefList);
    }

    public static String log_Optimistically_unchoked(int pID1, int pID2) {
        return String.format(optimistically_unchoked, pID1, pID2);
    }

    public static String log_Unchoke(int pID1, int pID2) {
        return String.format(unchoke, pID1, pID2);
    }

    public static String log_Choke(int pID1, int pID2) {
        return String.format(choke, pID1, pID2);
    }

    public static String log_Have(int pID1, int pID2, int index) {
        return String.format(have, pID1, pID2, index);
    }

    public static String log_is_interested(int pID1, int pID2) {
        return String.format(interested, pID1, pID2);
    }

    public static String log_not_interested(int pID1, int pID2) {
        return String.format(not_interested, pID1, pID2);
    }

    public static String log_Downloaded_piece(Peer p1, int pID2, int pieceID) {
        int total = p1.bitfield.currSize();
        return String.format(downloaded_piece, p1.peerID, pieceID, pID2, total);
    }

    public static String log_Complete(int pID) {
        return String.format(complete, pID);
    }

    // Semaphore Functions

    public static void set_lock() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void release_lock() {
        semaphore.release();
    }
}