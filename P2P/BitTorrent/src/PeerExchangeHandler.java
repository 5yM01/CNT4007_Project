import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;


public class PeerExchangeHandler extends Thread {
    // Class Representing P2P Exhange Between Peers
	// Peer A: "Client" Peer that Initiates Connection
	// Peer B: "Server" Peer that has Open Connection

	// TCP Connection Variables
	private Socket connection;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Boolean closed = false;
	public Boolean trying_to_close = false;
	public Boolean neighborDone = false;


	// Current Peer Information
	private Peer myPeer;
	private Boolean listener;
	private Boolean hasInitiated = false;
	private Boolean isInterested = false;
	private Boolean isSending = false;
	private Boolean isReceiving = false;
	private Boolean isUnchoked = false;

	// Neighbor Peer Information
	private Integer neighborID;
	private Handshake_Msg neighborHandshake;
	private BitFieldArrayBits neighborBitfield = null;
	private Boolean neighborHasFile = false;
	private Boolean neighborInterested = false;
	private Boolean neighborOptimisticallyChoked = false;
	private Boolean neighborUnchoked = false;

	// Constructors & Initialization

	public PeerExchangeHandler(Socket connection) throws IOException {
		// Constructor for Listener (Peer B)
		this.connection = connection;
		construct(true);
	}
	
	public PeerExchangeHandler(Peer _peer) throws IOException {
		// Constructor for Peer Initiating Connection (Peer A)
		this.connection = new Socket(_peer.peerAddress, _peer.peerPort);
		this.neighborID = _peer.peerID;
		construct(false);
	}

	private void construct(Boolean isListener) throws IOException {
		this.myPeer = peerProcess.getMyPeer();
		setListener(isListener);
		init_streams();
	}

	private void init_streams() throws IOException {
		// Initialize Input and Output Streams
		out = new ObjectOutputStream(this.connection.getOutputStream());
		out.flush();
		in = new ObjectInputStream(this.connection.getInputStream());
	}

	// Main P2P Function

	public void run() {
		try {
			init_contact();
			setHasInitiated(true);
			pieceExchange();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close_connection();
		}
	}

	// P2P Initial Handshake & Bitfield Exchange

	public void init_contact() throws ClassNotFoundException, IOException {
		// Handshake
		handshake_exg();

		// Bitfield
		bitfield_exg();
    }

	public void handshake_exg() {
		// Handshake Exchange between Peer A and Peer B
		sendHandshakeMessage();
		Handshake_Msg neighborHandshake = (Handshake_Msg) recvMessage();
		
		if (isListener()) {
			this.neighborID = neighborHandshake.getPeerID();
			String log_msg = PeerLog.log_connected_from(myPeer.peerID, this.neighborID);
			myPeer.writeToLog(log_msg);
		} else if (!neighborHandshake.checkHS(this.neighborID)) {
			// Peer A Checks if it's Peer B that has established connection
			System.out.println("PeerID is not the same or incorrect header!");
		}
	}

	public void bitfield_exg() {
		// Peer A Sends Bitfield to Peer B
		// Peer B Sends If It Has Piece
		if (!isListener() || myPeer.hasAtLeastOnePiece()) {
			sendBitFieldMessage();
		}

		// Peer B Receives Bitfield & Sends Interest Message
		if (isListener()) {
			setPeerBitfield((Actual_Msg) recvMessage());
			sendInterestMessage();
		}

		Actual_Msg msg = (Actual_Msg) recvMessage();
		if (!isListener() && msg.getMsgType() == Type.BITFIELD) {
			// Peer B Has Piece & Sends Bitfield to Peer A
			setPeerBitfield(msg);
			sendInterestMessage();
			msg = (Actual_Msg) recvMessage();
		} else if (!isListener()) {
			// Peer A Sends Not Interested if BitField Not Received from Peer B
			neighborBitfield = new BitFieldArrayBits(myPeer.bitfield.totalLength);
			sendInterestMessage();
		}

		// Interest Exchange Between Peers
		if (msg.getMsgType() == Type.INTERESTED || msg.getMsgType() == Type.NOT_INTERESTED) {
			setNeighborInterested(msg);
		}
	}

	// P2P Piece Exchange

	public void pieceExchange() {
		// If Neighbor Unchoked & Neighbor Interested, Send Pieces To Them
		// If Unchoked By Neighbor & Still Interested, Receive Pieces From Them
		String log_message = "";
		while (!bothAreComplete()) {
			if (isComplete()) {
				sendActualMessage(Type.DONE);
			}

			Actual_Msg msg = (Actual_Msg) recvMessage();

			Type msgType;
			try {
				msgType = msg.getMsgType();
			} catch (NullPointerException npe) {
				neighborHasFile = true;
				myPeer.peerHasFile = true;
				break;
				// continue;
			}

			switch (msgType) {
				case CHOKE:
					// Neighbor has Choked Connection
					setIsUnchoked(false);
					log_message = PeerLog.log_Choke(myPeer.peerID, getNeighborID());
					myPeer.writeToLog(log_message);
					break;

				case UNCHOKE:
					// Neighbor has Unchoked Connection
					setIsUnchoked(true);
					sendRequestMessage();
					log_message = PeerLog.log_Unchoke(myPeer.peerID, neighborID);
					myPeer.writeToLog(log_message);
					break;

				case INTERESTED:
				case NOT_INTERESTED:
					// Neighbor Has Shown Interest/Uninterest
					setNeighborInterested(msg);
					break;

				case HAVE:
					// Neighbor Has Obtained Piece
					Integer pieceID = msg.getPayload().getPayloadIndex();

					// Update Neighbor Bitfield
					neighborBitfield.setArrayPiece(pieceID);
					
					log_message = PeerLog.log_Have(myPeer.peerID, getNeighborID(), pieceID);
					myPeer.writeToLog(log_message);

					// Determine Whether to Send Interested
					if (!piecesToGetFromNeighbor().isEmpty()) {
						sendInterestMessage();
					}

					break;

				case REQUEST:
					// Received Request but Neighbor Choked
					if (!getNeighborUnchoked()) {
						continue;
					}

					// Neighbor Wants Piece
					sendPiece(msg.getPayload());

					// Adds to Neighbors Download Count
					incrementDownload(neighborID);

					break;
				
				case PIECE:
					// Choked, Cannot Accept Pieces
					// if (!getIsUnchoked()) {
					// 	continue;
					// }

					// Peer Receives Piece From Neighbor
					extractPiece(msg.getPayload());

					if (isComplete()) {
						log_message = PeerLog.log_Complete(myPeer.peerID);
						myPeer.writeToLog(log_message);
					} else if (!piecesToGetFromNeighbor().isEmpty()) {
						sendRequestMessage();
						continue;
					}

					sendInterestMessage();
					break;
				case DONE:
					this.neighborHasFile = true;
					this.neighborDone = true;
				default:
					break;
			}
		}
		System.out.println("DONE: " + neighborID);
	}

	// Helper Functions

	public synchronized void sendHandshakeMessage() {
		sendMessage(new Handshake_Msg(myPeer.peerID));
	}

	public synchronized void sendActualMessage(Type _msgType) {
		sendMessage(new Actual_Msg(_msgType));
	}

	public synchronized void sendActualMessage(Type _msgType, Payload data) {
		sendMessage(new Actual_Msg(_msgType, data));
	}

	public void sendBitFieldMessage() {
		sendActualMessage(Type.BITFIELD, new Payload(Payload.PayloadTypes.BitFieldArray_Type, myPeer.bitfield_bits));
	}

	public void sendInterestMessage() {
		if (!piecesToGetFromNeighbor().isEmpty()) {
			setIsInterested(true);
			sendActualMessage(Type.INTERESTED);
		} else {
			setIsInterested(false);
			sendActualMessage(Type.NOT_INTERESTED);
		}
	}

	public void unchokeExchange() {
		sendUnchokeMessage();
		setNeighborUnchoked(true);
	}

	public void chokeExchange() {
		sendChokeMessage();
		setNeighborUnchoked(false);
	}

	public void sendUnchokeMessage() {
		if (neighborInterested) {
			sendActualMessage(Type.UNCHOKE);
		}
	}

	public void sendChokeMessage() {
		sendActualMessage(Type.CHOKE);
	}

	public synchronized HashSet<Integer> piecesToGetFromNeighbor() {
		HashSet<Integer> pieces = new HashSet<>(myPeer.getPiecesNeeded());
		pieces.retainAll(neighborBitfield.currentPieces());
		// pieces.removeAll(myPeer.piecesRequested);
		return pieces;
	}

	public synchronized Integer pieceToRequest() {
		HashSet<Integer> pieces = piecesToGetFromNeighbor();

		if (pieces.isEmpty()) {
			return null;
		}

		Integer pieceID = Client_Utils.randomSetValue(pieces);
		myPeer.piecesRequested.add(pieceID);

		return pieceID;
	}

	public void sendRequestMessage() {
		// Select Random Piece from Neighbor that MyPeer Doesn't Have
		if (!isComplete() && getIsUnchoked()) {
			Integer pieceID = pieceToRequest();
			if (pieceID != null) {
				sendActualMessage(Type.REQUEST, new Payload(Payload.PayloadTypes.PieceIndex_Type, pieceID));
			}
		}
	}

	public void sendPiece(Payload pieceInfo) {
		// Determines Piece Requested & Sends to Neighbor
		BitField data = myPeer.bitfield.data_fields[pieceInfo.getPayloadIndex()];
		sendActualMessage(Type.PIECE, new Payload(Payload.PayloadTypes.PieceContent_Type, data));
	}

	public void extractPiece(Payload load) {
		BitField piece = load.getPayloadPiece();
		
		// Send Have To All Neighbors
		if (!myPeer.bitfield.currentPieces().contains(piece.id)) {
			Client_Utils.sendHaveToAll(piece.id);
		}
		
		// Add to MyPeer's Bitfield
		myPeer.addPiece(piece);

		String log_message = PeerLog.log_Downloaded_piece(myPeer, getNeighborID(), piece.id);
		myPeer.writeToLog(log_message);
	}

    public static void incrementDownload(Integer pID) {
		// Increments download amount when piece sent
        Integer curr = peerProcess.downloadAmountInInterval.get(pID);
		peerProcess.downloadAmountInInterval.put(pID, curr + 1);
    }

	public void sendHaveMessage(int pieceID) {
		sendActualMessage(Type.HAVE, new Payload(Payload.PayloadTypes.PieceIndex_Type, pieceID));
	}

	public Boolean isComplete() {
		// Checks if MyPeer Has Complete File
		myPeer.peerHasFile = myPeer.getPiecesNeeded().isEmpty();
		return myPeer.peerHasFile;
	}

	public Boolean checkIfNeighborComplete() {
		// Checks if Neighbor Has Complete File
		setNeighborHasFile(neighborBitfield.piecesNeeded().isEmpty());
		return this.neighborHasFile;
	}

	private boolean bothAreComplete() {
		return isComplete() && checkIfNeighborComplete();
	}

	// Variable GET & SET

	public void setNeighborInterested(Actual_Msg msg) {
		String log_msg;
		
		this.neighborInterested = (msg.getMsgType() == Type.INTERESTED);

		if (this.neighborInterested) {
			log_msg = PeerLog.log_is_interested(myPeer.peerID, getNeighborID());
		} else {
			log_msg = PeerLog.log_not_interested(myPeer.peerID, getNeighborID());
		}

		myPeer.writeToLog(log_msg);
	}

	public Boolean getHasInitiated() {
		return hasInitiated;
	}

	public void setHasInitiated(Boolean hasInitiated) {
		this.hasInitiated = hasInitiated;
	}

	public Boolean getNeighborUnchoked() {
		return neighborUnchoked;
	}

	public void setNeighborUnchoked(Boolean neighborUnchoked) {
		this.neighborUnchoked = neighborUnchoked;
	}

	public Boolean getNeighborHasFile() {
		return neighborHasFile;
	}

	public void setNeighborHasFile(Boolean neighborHasFile) {
		this.neighborHasFile = neighborHasFile;
	}

	public void setNeighborHandshake() {
		this.neighborHandshake = (Handshake_Msg) recvMessage();
	}

	public Boolean getIsInterested() {
		return isInterested;
	}

	public void setIsInterested(Boolean isInterested) {
		this.isInterested = isInterested;
	}

	public Handshake_Msg getNeighborHandshake() {
		return neighborHandshake;
	}

	public Boolean getIsSending() {
		return isSending;
	}

	public Boolean getIsReceiving() {
		return isReceiving;
	}

	public void setIsUnchoked(Boolean isUnchoked) {
		this.isUnchoked = isUnchoked;
	}

	public Boolean getNeighborOptimisticallyChoked() {
		return neighborOptimisticallyChoked;
	}

	public void setNeighborOptimisticallyChoked(Boolean optimisticallyChoked) {
		this.neighborOptimisticallyChoked = optimisticallyChoked;
	}

	public Boolean getIsUnchoked() {
		return isUnchoked;
	}

	public Integer getNeighborID() {
		return neighborID;
	}

	public Boolean getNeighborInterested() {
		return neighborInterested;
	}

	public void setPeerBitfield(Actual_Msg peerBitfield) {
		this.neighborBitfield = peerBitfield.getPayload().getPayloadBFA();
		checkIfNeighborComplete();
	}

	public Boolean isListener() {
		return listener;
	}

	public void setListener(Boolean listener) {
		this.listener = listener;
	}

	public synchronized void sendMessage(Message msg) {
		try {
			if (!this.closed || !bothAreComplete()) {
				this.isSending = true;
				out.writeObject(msg);
				out.flush();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			this.isSending = false;
		}
	}

	public Message recvMessage() {
		Message msg = null;

		try {
			if (!this.closed) {
				this.isReceiving = true;
				msg = (Message) in.readObject();
			}
		} catch (EOFException eof) {
			;
		} catch (SocketException se) {
			se.printStackTrace();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} finally {
			this.isReceiving = false;
		}

		return msg;
	}

	public void close_connection() {
		try {
			Client_Utils.waitUntilAllPeersHaveFile();
			Client_Utils.waitToCloseConnections();
			this.closed = true;
			this.connection.close();
			System.out.println("Closed Connection for " + neighborID);
		} catch (IOException e) {
			;
		}
	}
}