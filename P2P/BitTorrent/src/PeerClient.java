import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.ArrayList;

public class PeerClient {
    // TODO: Rename to peerProcess

    // Class Representing Main Program

    // Peer Running Process
    static Peer myPeer;

    // Common.cfg Variables
    static int NumberOfPreferredNeighbors;
    static int UnchokingInterval;
    static int OptimisticUnchokingInterval;
    static String FileName;
    static int FileSize;
    static int PieceSize;

    // Number of BitFields Based on File & Piece Sizes
    static int BitFieldLength;

    // TCP Connection Info
    static int clientNum = 0;
    static ArrayList<PeerExchangeHandler> peerConnections = new ArrayList<PeerExchangeHandler>();

    // Peers
    static ArrayList<Peer> peerList = new ArrayList<Peer>();

    public static void main(String[] args) throws Exception {
        // Read cfg files
        config_init();

        // Parse Parameters
        params(args);

        // Establish TCP Connection
        tcp_connect();

        // TODO: Move Exchange Functions Here?

        // Close Connections
        close_connections();

        System.out.println("FIN!");
    }

    public static void config_init() {
        // Parsing Common.cfg File
        common_config_init();

        // Parsing PeerInfo.cfg File
        peerinfo_config_init();
    }

    public static void common_config_init() {
        String[] commonVarsArr = new String[6];
        int i = 0;

        for (String line : Client_Utils.read_file("Common.cfg")) {
            commonVarsArr[i++] = line.substring(line.lastIndexOf(" ") + 1, line.length());
        }

        NumberOfPreferredNeighbors = Integer.parseInt(commonVarsArr[0]);
        UnchokingInterval = Integer.parseInt(commonVarsArr[1]);
        OptimisticUnchokingInterval = Integer.parseInt(commonVarsArr[2]);
        FileName = commonVarsArr[3];
        FileSize = Integer.parseInt(commonVarsArr[4]);
        PieceSize = Integer.parseInt(commonVarsArr[5]);
        BitFieldLength = (int) Math.ceil(FileSize / PieceSize);
    }

    public static void peerinfo_config_init() {
        for (String line : Client_Utils.read_file("PeerInfo.cfg")) {
            String[] tokens = line.split("\\s+");
            peerList.add(new Peer(tokens[0], tokens[1], tokens[2], tokens[3],0, BitFieldLength));
        }
    }
    
    public static void params(String[] args) throws IOException {
        int myPeerID = Integer.parseInt(args[0]);

        for (Peer p : peerList) {
            if (p.peerID == myPeerID) {
                myPeer = p;
                break;
            }
            clientNum++;
        }

        Client_Utils.createLogFile(myPeerID);
    }

    public static void tcp_connect() throws IOException {
        // Loop that Connects Current Peer to all Currently Running Neighbor Peers
        for (int i = clientNum, j = 0; i > 0; i--, j++) {
            Peer currPeer = peerList.get(j);
            PeerExchangeHandler thread = new PeerExchangeHandler(currPeer, myPeer);
            thread.setNeighborTiming(NumberOfPreferredNeighbors, UnchokingInterval, OptimisticUnchokingInterval);
            peerConnections.add(thread);
            thread.start();

            String log_message = PeerLog.log_connected_to(myPeer.peerID, currPeer.peerID);
            myPeer.writeToLog(log_message);
        }

        // Opens Socket for Current Peer to Accept Incoming Connections
        // Last Peer Does Not Listen
        // TODO: Check if necessary
        if (clientNum != peerList.size() - 1) {
            ServerSocket listener = new ServerSocket(myPeer.peerPort);
            try {
                // Listens To Connections With Remaining Peers On List
                for (int i = clientNum + 1; i < peerList.size(); i++) {
                    PeerExchangeHandler listeningHandler = new PeerExchangeHandler(listener.accept(), myPeer);
                    listeningHandler.setNeighborTiming(NumberOfPreferredNeighbors, UnchokingInterval, OptimisticUnchokingInterval);
                    listeningHandler.start();
                }
            } finally {
                listener.close();
            }
        }
    }

    public static void send_file() throws IOException {
        //string for subdirectory path that contains file (‘~/project/peer_1002/file_name.dat)
        String subFolderFilePath = "project/peer_" + Integer.toString(myPeer.peerID) + "/" + FileName;

        //read file into buffer of bytes
        FileInputStream fileIn = new FileInputStream(subFolderFilePath);

        //allocate byte array size of file
        byte[] data = new byte[FileSize];

        //use buffer input object on file object
        BufferedInputStream bufIn = new BufferedInputStream(fileIn);

        //TODO: Split into pieces and send?

        //read into byte array? pieces?
        //bufIn.read(data);
    }

    public static void close_connections() throws IOException {
        Client_Utils.waitUntilAllPeersHaveFile(peerList);
        for (PeerExchangeHandler s : peerConnections) {
            s.close_connection();
        }
    }
}
