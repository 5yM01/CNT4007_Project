import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerClient {
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

    // P2P Tunnels
    static HashSet<Integer> preferredNeighbors = new HashSet<Integer>();
    static HashSet<Integer> interestedNeighbors = new HashSet<Integer>();
    static HashSet<Integer> chokedNeighbors = new HashSet<Integer>();
    static HashMap<Integer, Integer> downloadAmountInInterval = new HashMap<>();

    // Peers
    static ArrayList<Peer> peerList = new ArrayList<Peer>();
    static PeerExchangeHandler optimisticNeighbor = null;
    
    // Schedulers
    static ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) throws Exception {
        // Read cfg files
        config_init();

        // Parse Parameters
        params(args);

        // Establish TCP Connection
        tcp_connect();

        // P2P Piece Exchange
        p2p_exchange();

        // Close Connections
        close_connections();

        System.out.println("FIN!");
    }

    // Initial Read of Configuration Files
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
        BitFieldLength = (int) Math.ceil((float) FileSize / PieceSize);
    }

    public static void peerinfo_config_init() {
        for (String line : Client_Utils.read_file("PeerInfo.cfg")) {
            String[] tokens = line.split("\\s+");
            peerList.add(new Peer(tokens[0], tokens[1], tokens[2], tokens[3], BitFieldLength));
        }
    }

    // Program Parameter Parse
    public static void params(String[] args) throws IOException {
        int myPeerID = Integer.parseInt(args[0]);
        Boolean addToClientNum = true;

        for (Peer p : peerList) {
            if (Integer.compare(myPeerID, p.peerID) == 0) {
                myPeer = p;
                if (myPeer.peerHasFile) {
                    myPeer.importFile(FileName);
                }
                addToClientNum = false;
            } else {
                downloadAmountInInterval.put(p.peerID, 0);
            }

            if (addToClientNum) {
                clientNum++;
            }
        }

        Client_Utils.createLogFile();
    }

    // TCP Connections
    public static void tcp_connect() throws IOException {
        // Loop that Connects Current Peer to all Currently Running Neighbor Peers
        for (int i = clientNum, j = 0; i > 0; i--, j++) {
            Peer currPeer = peerList.get(j);
            PeerExchangeHandler thread = new PeerExchangeHandler(currPeer, myPeer);
            peerConnections.add(thread);
            thread.start();

            String log_message = PeerLog.log_connected_to(myPeer.peerID, currPeer.peerID);
            myPeer.writeToLog(log_message);
        }

        // Opens Socket for Current Peer to Accept Incoming Connections
        ServerSocket listener = new ServerSocket(myPeer.peerPort);
        try {
            // Listens To Connections With Remaining Peers On List
            for (int i = clientNum + 1; i < peerList.size(); i++) {
                PeerExchangeHandler listeningHandler = new PeerExchangeHandler(listener.accept(), myPeer);
                peerConnections.add(listeningHandler);
                listeningHandler.start();
            }
        } finally {
            listener.close();
        }
    }

    // Exchange
    public static void p2p_exchange() {
        // TODO: Remove Wait?
        // Waits until all Peers have Completed Handshake & Bitfield Exchange
        Client_Utils.waitUntilAllPeersHaveInitiated();

        // Continuously Choose Preferred Neighbors
		Runnable prefNeighborTask = new Runnable() {
            public void run () {
                neighborSelection(true);
                unchokeNeighbors();
                chokeNeighbors();
            }
		};
		taskScheduler.scheduleAtFixedRate(prefNeighborTask, 0, UnchokingInterval, TimeUnit.SECONDS);

        // Continuously Choose Optimistically Unchoked Neighbor
        Runnable optimisticNeighborTask = new Runnable() {
			public void run () {
                neighborSelection(false);
            }
		};
		taskScheduler.scheduleAtFixedRate(optimisticNeighborTask, 0, OptimisticUnchokingInterval, TimeUnit.SECONDS);

        // Waits until all Peers Have Obtained the File
        Client_Utils.waitUntilAllPeersHaveFile();
    }

    // Neighbor Selection
    public static void neighborSelection(Boolean pref) {
		if (myPeer.peerHasFile){
            // Determine preferred neighbors randomly
            selectNeighbors(true, pref);
		} else {
            // Determine preferred neighbors via download rates
            selectNeighbors(false, pref);
        }
    }

    public static void selectNeighbors(Boolean rand, Boolean pref) {
        String log_message = "";
        Boolean hasChanged = false;
        updatedInterested();
        
        if (pref) {
            // Select k preferred neighbors
            preferredNeighbors.clear();
            hasChanged = selectPreferredNeighbors(rand);
            log_message = PeerLog.log_Preferred_neighbor(myPeer, new ArrayList<Integer>(preferredNeighbors));
        } else {
            // Select 1 Optimistically Unchoked Neighbor
            hasChanged = selectOptimisticNeighbor();
            log_message = PeerLog.log_Optimistically_unchoked(myPeer.peerID, Client_Utils.getOptimisticPeerExchangeHandler().getNeighborID());
        }

        if (hasChanged) {
            myPeer.writeToLog(log_message);
        }
    }

    public static void updatedInterested() {
        for (PeerExchangeHandler c : peerConnections) {
            if (c.getNeighborInterested()) {
                interestedNeighbors.add(c.getNeighborID());
            } else {
                interestedNeighbors.remove(c.getNeighborID());
            }
        }
    }

    public static Boolean selectPreferredNeighbors(Boolean rand) {
        HashSet<Integer> initial = preferredNeighbors;

        if (rand) {
            if (interestedNeighbors.size() <= NumberOfPreferredNeighbors) {
                preferredNeighbors.addAll(interestedNeighbors);
            } else {
                while (preferredNeighbors.size() != NumberOfPreferredNeighbors) {
                    preferredNeighbors.add(Client_Utils.randomSetValue(interestedNeighbors));
                }
            }
        } else {
            // TODO: Check If Works
            DownloadRatesHandler drh = new DownloadRatesHandler();

            for (PeerExchangeHandler p : peerConnections) {
                Float currRate = ((float) downloadAmountInInterval.get(p.getNeighborID())) / UnchokingInterval;
                drh.add_rate(p.getNeighborID(), currRate);
            }
            Client_Utils.setAllValues(downloadAmountInInterval, 0);

            preferredNeighbors = drh.getPreferredNeighbors(NumberOfPreferredNeighbors);
            preferredNeighbors.retainAll(interestedNeighbors);
        }

        // Get Neighbors to Choke
        for (PeerExchangeHandler peh : peerConnections) {
            if (!preferredNeighbors.contains(peh.getNeighborID())) {
                chokedNeighbors.add(peh.getNeighborID());
            }
        }

        return !initial.equals(preferredNeighbors);
    }

    public static Boolean selectOptimisticNeighbor() {
        // TODO: Test. Start off as choked? initial Delay?
        Set<Integer> interestedAndChoked = new HashSet<Integer>(interestedNeighbors);
        interestedAndChoked.retainAll(chokedNeighbors);
        if (!interestedAndChoked.isEmpty()) {          
            // Sets Current Optimistically Unchoked Neighbor Off
            if (optimisticNeighbor != null) {
                if (!preferredNeighbors.contains(optimisticNeighbor.getNeighborID())) {
                    // Choke unless peer became preferred
                    optimisticNeighbor.chokeExchange();
                }
                optimisticNeighbor.setNeighborOptimisticallyChoked(false);
                optimisticNeighbor = null;
            }
            
            // Randomly Selects New Optimistically Unchoked Neighbor
            Integer pID = Client_Utils.randomSetValue(interestedAndChoked);
            optimisticNeighbor = Client_Utils.getPeerExchangeHandlerByID(pID);
            
            // Sends Unchoke Message
            chokedNeighbors.remove(optimisticNeighbor.getNeighborID());
            optimisticNeighbor.setNeighborOptimisticallyChoked(true);
            optimisticNeighbor.unchokeExchange();
            return true;
        }

        return false;
    }

    public static void unchokeNeighbors() {
        PeerExchangeHandler curr;
        for (Integer pID : preferredNeighbors) {
            curr = Client_Utils.getPeerExchangeHandlerByID(pID);
            if (!curr.getIsUnchoked()) {
                curr.unchokeExchange();
                String log_message = PeerLog.log_Unchoke(myPeer.peerID, pID);
                myPeer.writeToLog(log_message);
            }
        }
    }

    public static void chokeNeighbors() {
        for (PeerExchangeHandler peh : peerConnections) {
            Boolean notOptimistic = Integer.compare(peh.getNeighborID(), optimisticNeighbor.getNeighborID()) != 0;
            if (!preferredNeighbors.contains(peh.getNeighborID()) && notOptimistic) {
                peh.chokeExchange();
                chokedNeighbors.add(peh.getNeighborID());
                String log_message = PeerLog.log_Choke(myPeer.peerID, peh.getNeighborID());
                myPeer.writeToLog(log_message);
            }
        }
    }

    // Pieces Needed Functions

    public static void removePieceFromNeeded(BitField bf) {
        myPeer.bitfield.setArrayPiece(bf);
    }

    public static HashSet<Integer> getPiecesNeeded() {
        return myPeer.bitfield.piecesNeeded();
    }

    // TCP Connections Closed
    public static void close_connections() throws IOException {
        for (PeerExchangeHandler s : peerConnections) {
            s.close_connection();
        }

        taskScheduler.shutdown();
    }
}
