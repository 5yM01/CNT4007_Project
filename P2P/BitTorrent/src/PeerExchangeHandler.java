import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

public class PeerExchangeHandler extends Thread {
    // Class Representing P2P Exhange Between Peers

	// TCP Connection Variables
	private Socket connection;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	// Current Peer Information
	private Peer myPeer;
	private Boolean listener;
	private ArrayList<Peer> myPeerNeighbors;
	
	// Neighbor Peer Information
	private Peer peer = null;
	private BitFieldArray peerBitfield;
	private Boolean peerInterested;
	private ArrayList<Integer> piecesToGet;

	// Common.cfg Variables
	private int NumberOfPreferredNeighbors;
    private int UnchokingInterval;
    private int OptimisticUnchokingInterval;
	
	// Piece Exchange Variables
	ScheduledExecutorService taskScheduler;

	// Constructors & Initialization

	public PeerExchangeHandler(Socket connection, Peer _myPeer) throws IOException {
		// Constructor for Listener
		this.connection = connection;
		construct(_myPeer, true);
	}
	
	public PeerExchangeHandler(Peer _peer, Peer _myPeer) throws IOException {
		// Constructor for Peer Initiating Connection
		this.connection = new Socket(_peer.peerAddress, _peer.peerPort);
		this.peer = _peer;
		construct(_myPeer, false);
	}

	public void construct(Peer _myPeer, Boolean isListener) throws IOException {
		this.myPeer = _myPeer;
		setListener(isListener);
		init_streams();
	}

	public void init_streams() throws IOException {
		//Initialize Input and Output Streams
		out = new ObjectOutputStream(this.connection.getOutputStream());
		out.flush();
		in = new ObjectInputStream(this.connection.getInputStream());
	}

	// Main P2P Function

	public void run() {
		// Initial Contact
		try {
			init_contact();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Continuously Choose Neighbors
		Runnable task = new Runnable() {
			public void run () {preferred_neighbors();}
		};
		taskScheduler.scheduleAtFixedRate(task, 0, UnchokingInterval, TimeUnit.SECONDS);
	}

	// P2P Initial Handshake & Bitfield Exchange

	public void init_contact() throws ClassNotFoundException, IOException {
        System.out.println("Handshake and Bitfield");

		// Handshake
		Handshake_Msg response = handshake_exg();

		// Bitfield
		bitfield_exg(response);
    }

	public Handshake_Msg handshake_exg() {
		// Handshake Exchange between Peer A and Peer B
		sendMessage(new Handshake_Msg(myPeer.peerID));
		Handshake_Msg response = (Handshake_Msg) recvMessage();

		if (isListener()) {
			String log_msg = PeerLog.log_connected_from(myPeer.peerID, response.getPeerID());
			myPeer.writeToLog(log_msg);
		}

		// Peer A Checks if its Peer B that has established connection
		if (!isListener() && !response.checkHS(peer.peerID)) {
			System.out.println("PeerID is not the same or incorrect header!");
		}
		//add peer B as neighbor to peer A in list variable
		else{
			myPeerNeighbors.add(peer);
		}

		return response;
	}

	public void bitfield_exg(Handshake_Msg response) {
		// Peer A Sends Bitfield to Peer B
		// Peer B Sends If It Has Piece
		if (!isListener() || myPeer.hasAnyPiece()) {
			Payload load = new Payload(Payload.PayloadTypes.BitFieldArray_Type, myPeer.bitfield);
			sendMessage(new Actual_Msg(Type.BITFIELD, load));
		}

		// Peer B Receives Bitfield & Sends Interest Message
		if (isListener()) {
			setPeerBitfield((Actual_Msg) recvMessage());
			setPiecesToGet(myPeer.bitfieldArrayDiff(this.peerBitfield.fields));
			sendInterestedMessage();
		}

		// Peer B Has Piece & Sends Bitfield to Peer A
		Actual_Msg msg = (Actual_Msg) recvMessage();
		if (!isListener() && msg.getMsgType() == Type.BITFIELD) {
			setPeerBitfield(msg);
			setPiecesToGet(myPeer.bitfieldArrayDiff(this.peerBitfield.fields));
			sendInterestedMessage();
			msg = (Actual_Msg) recvMessage();
		} else if (!isListener()) {
			// TODO: Send not interested from Peer A to Peer B if Peer B does not have any pieces
			sendMessage(new Actual_Msg(Type.NOT_INTERESTED));
		}

		// Interest Exchange Between Peers
		if (msg.getMsgType() == Type.INTERESTED || msg.getMsgType() == Type.NOT_INTERESTED) {
			setInterested(msg, response.getPeerID());
		} else {
			System.out.println("Message Type not allowed yet!");
		}
	}

	// P2P Piece Exchange & Neighbor Selection

    public void preferred_neighbors() {
        // Calculate downloading rate
        download_rates();
        
    }

    public void download_rates() {
        System.out.println("Calculate Download Rates and Choose top k");

		//IF Peer A (Peer initiating connection) has a COMPLETE file -> determine preferred neighbors randomly
		if(peer.peerHasFile){
			System.out.println("Determine preferred neighbors randomly");

			int[] selectedNeighborsIndex = new int[NumberOfPreferredNeighbors];

			int counter = 0;
			while(counter < NumberOfPreferredNeighbors){
				int randomNeighborNum = ThreadLocalRandom.current().nextInt(0, NumberOfPreferredNeighbors + 1);
				selectedNeighborsIndex[counter] = randomNeighborNum;
				counter++;
			}
		}
		//IF NOT -> follow 1 and 2 below

		//1. Every P seconds, Peer A selects preferred neighbors
		//1a. Calculate download rate for each neighbor
		ArrayList<Long> rates = new ArrayList<>();

		for(int i = 0; i < myPeerNeighbors.size(); i++){
			long start = System.nanoTime();
			//send these bytes to peer
			byte[] byteTestArr = new byte[1024];
			//TODO: figure out sending bytes to each peer on one connection (also rate calculation might be wrong)
			Peer peerRateTest = myPeerNeighbors.get(i);
			long end = System.nanoTime();
			long totalTime = end - start;
			long downloadRate = 1024 / totalTime;
			rates.add(downloadRate);
		}
		//sort rate list in ascending order
		rates.sort(null);

		//1b. Pick k neighbors with highest rates (that are 'interested')
		ArrayList<Long> selectedNeighbors = new ArrayList<>();
		for(int j = 0; j < NumberOfPreferredNeighbors; j++){
			// Same download rate? -> pick randomly
			//TODO: Associate rates with neighbor peers (Map?)
			System.out.println("Check neighbor rates here");
		}

		//2. Unchoke selected neighbors
		//2a. Send unchoke msgs, expect same number of request msgs back
		sendUnchokeMessage();
		//2b. Don't send unchoke msg if neighbor is already unchoked
		//2c. All previously unchoked neighbors become choked, unless optimistic (send msgs, stop sending pieces to them)

		//3. Peer determines an optimistically unchoked neighbor every m seconds
		//3a. Selects randomly among INTERESTED and CHOKED neighbors
		//3b. Send unchoke msg, expect request msg back
		//3c. A peer can be both preferred and optimistically unchoked
    }

	// Helper Functions

	public void sendInterestedMessage() {
		if (getPiecesToGet().size() != 0) {
			sendMessage(new Actual_Msg(Type.INTERESTED));
		} else {
			sendMessage(new Actual_Msg(Type.NOT_INTERESTED));
		}
	}

	//TODO: Modify this function to work with a list of neighbor peers of size > 1
	public void sendUnchokeMessage() {
		if (peerInterested) {
			sendMessage(new Actual_Msg(Type.UNCHOKE));
		}
	}
	
	// Variable GET & SET

	public void setInterested(Actual_Msg msg, int peerID) {
		String log_msg;
		
		this.peerInterested = (msg.getMsgType() == Type.INTERESTED);

		if (this.peerInterested) {
			log_msg = PeerLog.log_is_interested(myPeer.peerID, peerID);
		} else {
			log_msg = PeerLog.log_not_interested(myPeer.peerID, peerID);
		}
		
		myPeer.writeToLog(log_msg);
	}

	public BitFieldArray getPeerBitfield() {
		return peerBitfield;
	}

	public void setPeerBitfield(Actual_Msg peerBitfield) {
		this.peerBitfield = peerBitfield.getPayload().getPayloadBFA();
	}

	public ArrayList<Integer> getPiecesToGet() {
		return piecesToGet;
	}

	public void setPiecesToGet(ArrayList<Integer> piecesToGet) {
		this.piecesToGet = piecesToGet;
	}
	
	public Boolean isListener() {
		return listener;
	}

	public void setListener(Boolean listener) {
		this.listener = listener;
	}

    public void setNeighborTiming(int pref, int unchoking, int optimistic) {
        this.NumberOfPreferredNeighbors = pref;
        this.UnchokingInterval = unchoking;
        this.OptimisticUnchokingInterval = optimistic;
		this.taskScheduler = Executors.newScheduledThreadPool(pref);
    }

	// TCP Connection

	public void sendMessage(Message msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
	
	public Message recvMessage() {
		Message msg = null;

		try {
			msg = (Message)in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return msg;
	}

	public void close_connection() throws IOException {
		this.in.close();
		this.out.close();
		this.connection.close();
	}
}