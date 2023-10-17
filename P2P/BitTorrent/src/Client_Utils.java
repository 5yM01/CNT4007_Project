import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class Client_Utils {
    // Class for any useful functions
    public static ArrayList<String> read_file(String path) {
        ArrayList<String> stringArr = new ArrayList<String>();

        try {
            Scanner reader = new Scanner(new File(path));
            while (reader.hasNextLine()) {
                stringArr.add(reader.nextLine());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return stringArr;
    }

    // Loops until all peers have file
    public static void waitUntilAllPeersHaveFile(ArrayList<Peer> peers) {
        while (!allPeersHaveFile(peers)) {
            assert true;
        }
    }

    // Checks if all peers have the file
    public static Boolean allPeersHaveFile(ArrayList<Peer> peers) {
        for (Peer p : peers) {
            if (!p.peerHasFile) {
                return false;
            }
        }

        return true;
    }

    public static void createLogFile(int peerID) throws IOException {
        String path = "log_peer_" + peerID + ".log";
        File log_file = new File(path);
        // TODO: Check or always create new log?
        if (!log_file.exists()) {
            log_file.createNewFile();
        }
    }

    public static String getDateTime() {
        LocalDateTime curr_time = LocalDateTime.now();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return curr_time.format(format);
    }

}