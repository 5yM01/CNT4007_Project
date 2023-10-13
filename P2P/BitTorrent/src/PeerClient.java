import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class PeerClient {
    static Peer myPeer;
    static int NumberOfPreferredNeighbors;
    static int UnchokingInterval;
    static int OptimisticUnchokingInterval;
    static String FileName;
    static int FileSize;
    static int PieceSize;
    static int BitFieldLength;

    // TCP Connection Info
    static int clientNum = 0;
    static ArrayList<PeerExchangeHandler> peerSockets = new ArrayList<PeerExchangeHandler>();

    // peers
    static ArrayList<Peer> peerList = new ArrayList<Peer>();

    public static void main(String[] args) throws Exception {
        // Read cfg files
        config_init();

        // Parse Parameters
        params(args);

        // Establish TCP Connection
        tcp_connect();

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
            peerList.add(new Peer(tokens[0], tokens[1], tokens[2], tokens[3], BitFieldLength));
        }
    }
    
    public static void params(String[] args) {
        int myPeerID = Integer.parseInt(args[0]);

        for (Peer p : peerList) {
            if (p.peerID == myPeerID) {
                myPeer = p;
                break;
            }
            clientNum++;
        }
    }

    public static void tcp_connect() throws IOException {
        for (int i = clientNum, j = 0; i > 0; i--) {
            Peer currPeer = peerList.get(j++);
            PeerExchangeHandler handler = new PeerExchangeHandler(currPeer, myPeer);
            handler.setNeighborTiming(NumberOfPreferredNeighbors, UnchokingInterval, OptimisticUnchokingInterval);
            peerSockets.add(handler);
            handler.start();
        }

        // TODO: Last on list isn't listening?
        ServerSocket listener = new ServerSocket(myPeer.peerPort);
        try {
			while(true) {
				PeerExchangeHandler listeningHandler = new PeerExchangeHandler(listener.accept(), myPeer);
                listeningHandler.setNeighborTiming(NumberOfPreferredNeighbors, UnchokingInterval, OptimisticUnchokingInterval);
                listeningHandler.start();
			}
		} finally {
				listener.close();
		}
    }

    public static void close_connections() throws IOException {
        for (PeerExchangeHandler s : peerSockets) {
            s.close_connection();
        }
    }
}
