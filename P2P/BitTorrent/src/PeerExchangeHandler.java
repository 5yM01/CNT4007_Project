import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
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

	// Current Peer Information
	private Peer myPeer;
	private Boolean listener;
	private Boolean hasInitiated = false;
	private Boolean isInterested = false;
	private Boolean isReceiving = false;
	private Boolean isUnchoked = false;

	// Neighbor Peer Information
	private Peer neighborPeer = null;
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
		this.neighborPeer = _peer;
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
			Client_Utils.waitUntilAllPeersHaveFile();
			close_connection();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		setNeighborHandshake();

		if (isListener()) {
			String log_msg = PeerLog.log_connected_from(myPeer.peerID, getNeighborHandshake().getPeerID());
			myPeer.writeToLog(log_msg);
		} else if (!getNeighborHandshake().checkHS(neighborPeer.peerID)) {
			// Peer A Checks if it's Peer B that has established connection
			System.out.println("PeerID is not the same or incorrect header!");
		}

		this.neighborID = getNeighborHandshake().getPeerID();
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
			Actual_Msg msg = (Actual_Msg) recvMessage();

			if (msg == null) {
				continue;
			}

			switch (msg.getMsgType()) {
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
					if (peerProcess.getPiecesNeeded().contains(pieceID) && !peerProcess.piecesRequested.contains(pieceID)) {
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
					Client_Utils.pieceSent(neighborID);

					break;
				
				case PIECE:
					// Peer Receives Piece From Neighbor
					extractPiece(msg.getPayload());

					if (isComplete()) {
						log_message = PeerLog.log_Complete(myPeer.peerID);
						myPeer.writeToLog(log_message);
					} else if (!getPiecesToGetFromNeighbor().isEmpty() && getIsUnchoked()) {
						sendRequestMessage();
					} else {
						sendInterestMessage();
					}
					break;

				default:
					break;
			}
		}
	}

	// Helper Functions

	public void sendHandshakeMessage() {
		sendMessage(new Handshake_Msg(myPeer.peerID));
	}

	public void sendActualMessage(Type _msgType) {
		sendMessage(new Actual_Msg(_msgType));
	}

	public void sendActualMessage(Type _msgType, Payload data) {
		sendMessage(new Actual_Msg(_msgType, data));
	}

	public void sendBitFieldMessage() {
		sendActualMessage(Type.BITFIELD, new Payload(Payload.PayloadTypes.BitFieldArray_Type, myPeer.bitfield_bits));
	}

	public void sendInterestMessage() {
		if (!getPiecesToGetFromNeighbor().isEmpty()) {
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

	public Integer pieceToRequest() {
		HashSet<Integer> pieces = getPiecesToGetFromNeighbor();
		pieces.removeAll(peerProcess.piecesRequested);

		if (pieces.isEmpty()) {
			return null;
		}

		Integer pieceID = Client_Utils.randomSetValue(pieces);

		return pieceID;
	}

	public void sendRequestMessage() {
		// Select Random Piece from Neighbor that MyPeer Doesn't Have
		Integer pieceID = pieceToRequest();
		if (pieceID != null) {
			sendActualMessage(Type.REQUEST, new Payload(Payload.PayloadTypes.PieceIndex_Type, pieceID));
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
		Client_Utils.sendHaveToAll(piece.id);

		// Downloads File
		downloadPiece(piece);

		// Add to MyPeer's Bitfield
		peerProcess.removePieceFromNeeded(piece);
		String log_message = PeerLog.log_Downloaded_piece(myPeer, getNeighborID(), piece.id);
		myPeer.writeToLog(log_message);
	}

	private void downloadPiece(BitField piece) {
		String path = "peer_" + myPeer.peerID + File.separator + peerProcess.FileName;

		try {
			RandomAccessFile fos = new RandomAccessFile(path, "rw");
			fos.seek(piece.id * piece.data.length * 8);
			fos.write(piece.data);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHaveMessage(int pieceID) {
		sendActualMessage(Type.HAVE, new Payload(Payload.PayloadTypes.PieceIndex_Type, pieceID));
	}

	public Boolean isComplete() {
		// Checks if MyPeer Has Complete File
		myPeer.peerHasFile = peerProcess.getPiecesNeeded().isEmpty();
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

	public Boolean getIsReceiving() {
		return isReceiving;
	}

	public void setIsReceiving(Boolean isReceiving) {
		this.isReceiving = isReceiving;
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

	public HashSet<Integer> getPiecesToGetFromNeighbor() {
		HashSet<Integer> pieces = new HashSet<>(peerProcess.getPiecesNeeded());
		pieces.retainAll(neighborBitfield.currentPieces());
		return pieces;
	}

	public Boolean isListener() {
		return listener;
	}

	public void setListener(Boolean listener) {
		this.listener = listener;
	}

	// TCP Connection

	public synchronized void sendMessage(Message msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (SocketException e) {
			;
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} 
	}

	public Message recvMessage() {
		Message msg = null;

		try {
			msg = (Message) in.readObject();
		} catch (EOFException | SocketException eof) {
			;
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

		return msg;
	}

	public void close_connection() {
		try {
			this.in.close();
			this.out.close();
			this.connection.close();
		} catch (IOException e) {
			;
		}
	}
}