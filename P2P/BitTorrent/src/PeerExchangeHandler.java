import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerExchangeHandler extends Thread {
    // Class Representing P2P Exhange Between Peers
	private Socket connection;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Boolean listener;
	private Peer peer = null;
	private Peer myPeer;
	private Actual_Msg peerBitfield;
	private Boolean peerInterested;
	private ArrayList<Integer> piecesToGet;
	ScheduledExecutorService taskScheduler;
	private int NumberOfPreferredNeighbors;
    private int UnchokingInterval;
    private int OptimisticUnchokingInterval;


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
	
	public void init_streams() throws IOException {
		//Initialize Input and Output Streams
		out = new ObjectOutputStream(this.connection.getOutputStream());
		out.flush();
		in = new ObjectInputStream(this.connection.getInputStream());
	}

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

	public void init_contact() throws ClassNotFoundException, IOException {
        System.out.println("Handshake and Bitfield");

		// Sends Handshake to Peer
		sendMessage(new Handshake_Msg(myPeer.peerID));
		Handshake_Msg response = (Handshake_Msg) recvMessage();

		response.print();

		// Checks if correct Peer has established connection
		if (!getListener() && !response.check(peer.peerID)) {
			System.out.println("PeerID is not the same!");
		}

		// TODO: Sends Bitfield to Peer If Has Piece
		// if (myPeer.hasAnyPiece()) {
		// 	sendMessage(new Actual_Msg(Type.BITFIELD, myPeer.bitfield));
		// }
		sendMessage(new Actual_Msg(Type.BITFIELD, myPeer.bitfield));
		setPeerBitfield((Actual_Msg) recvMessage());

		// Determines if interested in neighbor or not
		setPiecesToGet(myPeer.bitfieldArrayDiff(this.peerBitfield.getPayload().fields));
		if (getPiecesToGet().size() != 0) {
			sendMessage(new Actual_Msg(Type.INTERESTED));
		} else {
			sendMessage(new Actual_Msg(Type.NOT_INTERESTED));
		}

		setInterested((Actual_Msg) recvMessage());
    }

    public void preferred_neighbors() {
        // Calculate downloading rate
        download_rates();
        
    }

    public void download_rates() {
        System.out.println("Calculate Download Rates and Choose top k");
    }
	
	// Variable GET & SET

	public void construct(Peer _myPeer, Boolean isListener) throws IOException {
		this.taskScheduler = Executors.newScheduledThreadPool(NumberOfPreferredNeighbors);
		this.myPeer = _myPeer;
		setListener(isListener);
		init_streams();
	}

	public void setInterested(Actual_Msg msg) {
		this.peerInterested = (msg.getMsgType() == Type.INTERESTED);
	}

	public Actual_Msg getPeerBitfield() {
		return peerBitfield;
	}

	public void setPeerBitfield(Actual_Msg peerBitfield) {
		this.peerBitfield = peerBitfield;
	}

	public ArrayList<Integer> getPiecesToGet() {
		return piecesToGet;
	}

	public void setPiecesToGet(ArrayList<Integer> piecesToGet) {
		this.piecesToGet = piecesToGet;
	}
	
	public Boolean getListener() {
		return listener;
	}

	public void setListener(Boolean listener) {
		this.listener = listener;
	}

    public void setNeighborTiming(int pref, int unchoking, int optimistic) {
        this.NumberOfPreferredNeighbors = pref;
        this.UnchokingInterval = unchoking;
        this.OptimisticUnchokingInterval = optimistic;
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