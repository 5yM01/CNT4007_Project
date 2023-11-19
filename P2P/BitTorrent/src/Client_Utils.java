import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Client_Utils {
    // Class for any useful functions

    // Returns Piece Size
    public static int getPieceSize() {
        return PeerClient.PieceSize;
    }

    // Returns File Size
    public static int getFileSize() {
        return PeerClient.FileSize;
    }

    // Returns array of peerconnections
    public static ArrayList<PeerExchangeHandler> getPeerConnections() {
        return PeerClient.peerConnections;
    }

    // Reads file and returns list of lines
    public static ArrayList<String> read_file(String path) {
        ArrayList<String> stringArr = new ArrayList<String>();

        try {
            Scanner reader = new Scanner(new File(path));
            while (reader.hasNextLine()) {
                stringArr.add(reader.nextLine());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error Occured While Trying To Read File " + path);
            e.printStackTrace();
        }

        return stringArr;
    }

    // Returns byte array of file
    public static byte[] getFileBytes(String path) {
        File file = new File(path);
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Creates log file for peer
    public static void createLogFile() throws IOException {
        String path = "log_peer_" + PeerClient.myPeer.peerID + ".log";
        File log_file = new File(path);
        // TODO: Check or always create new log?
        if (!log_file.exists()) {
            log_file.createNewFile();
        }
    }

    // Returns the current date and time
    public static String getDateTime() {
        LocalDateTime curr_time = LocalDateTime.now();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return curr_time.format(format);
    }

    // Loops until all peers have file
    public static void waitUntilAllPeersHaveFile() {
        while (!allPeersHaveFile()) {
            waitTime(1000);
        }
    }

    // Loops until all peers have initiated
    public static void waitUntilAllPeersHaveInitiated() {
        while (!allPeersInitiated()) {
            waitTime(1000);
        }
    }

    // Waits specified amount of time (milliseconds)
    public static void waitTime(Integer time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Checks if all peers have the file
    public static Boolean allPeersHaveFile() {
        for (PeerExchangeHandler p : getPeerConnections()) {
            if (!p.getNeighborHasFile()) {
                return false;
            }
        }

        return true;
    }

    // Checks if all peers have initiated
    public static Boolean allPeersInitiated() {
        for (PeerExchangeHandler p : getPeerConnections()) {
            if (!p.getHasInitiated()) {
                return false;
            }
        }

        return true;
    }

    // Set all values of map to val
    public static void setAllValues(HashMap<Integer, Integer> map, Integer val) {
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            entry.setValue(val);
        }
    }

    // Returns a random values from the set
    public static Integer randomSetValue(Set<Integer> set) {
        int randomNeighborNum = randomValue(set.size());
        int i = 0;
        for (Integer num : set) {
            if (Integer.compare(randomNeighborNum, i) == 0) {
                return num;
            }
            i++;
        }
        
        // TODO: Make sure null can't be returned
        return null;
    }

    // Returns a random value between 0 and max (exclusive)
    public static Integer randomValue(Integer max) {
        return ThreadLocalRandom.current().nextInt(0, max);
    }

    // Get PEH by ID
    public static PeerExchangeHandler getPeerExchangeHandlerByID(Integer id) {
        for (PeerExchangeHandler peh : getPeerConnections()) {
            if (Integer.compare(peh.getNeighborID(), id) == 0) {
                return peh;
            }
        }

        return null;

    }

    // Checks if an Optimistic Peer has been selected
    public static Boolean hasOptimisticPeerExchangeHandler() {
        for (PeerExchangeHandler peh : getPeerConnections()) {
            if (peh.getNeighborOptimisticallyChoked()) {
                return true;
            }
        }

        return false;
    }

    // Returns Optimistic Peer
    public static PeerExchangeHandler getOptimisticPeerExchangeHandler() {
        for (PeerExchangeHandler peh : getPeerConnections()) {
            if (peh.getNeighborOptimisticallyChoked()) {
                return peh;
            }
        }

        return null;
    }

    // Sends a HAVE message to all neighbors
    public static void sendHaveToAll(Integer pieceID) {
        for (PeerExchangeHandler peh : getPeerConnections()) {
            peh.sendActualMessage(Type.HAVE, new Payload(Payload.PayloadTypes.PieceIndex_Type, pieceID));
        }
    }

    // Increments download amount when piece sent
    public static void pieceSent(Integer pID) {
        Integer curr = PeerClient.downloadAmountInInterval.get(pID);
		PeerClient.downloadAmountInInterval.put(pID, curr + 1);
    }
}