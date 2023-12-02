# <u>CNT4007 P2P Group Project</u>

## Group 27
##### <i>Syed Mahdi (smahdi@ufl.edu)</i> 
##### <i>Endrick Lafosse (@ufl.edu)</i>
##### <i>Stella Rojas (s.rojas@ufl.edu)</i>

## Team Member Contributions
**Syed Mahdi**
1. Unchoke/Choke Protocol
2. Sending Files/Downloading Files over TCP Connections
3. Supplemented code for Peer Representation
4. Testing and Debugging Project
5. Documentation

**Stella Rojas**
1. [List Contributions here]
2. 

**Endrick Lafosse**
1. [List Contributions here]
2. 

## Video Link for Demo 
[Put YouTube or DropBox link here]

## Achievements
Successfully implemented each of the protocols outlined in the Project Description. [Some errors may exist but they do not affect the correctness of the program or break it in a significant way].

## Brief Overview
Our project is a version of the P2P file-sharing software BitTorrent. It shares files between a number of peers using the protocols described below.

- Handshake Message
    - After connecting to each other, the peers will exchange messages that contain the header, zero bits, and peer ID. The stream of messages between the peers will then start after this.
      
- Actual Message 
    - This message consists of the message length, type, and payload which affect the other protocols in the program. It can be the following types: choke, unchoke, interested, not interested, have, bitfield, request, and piece.
      
- Bitfield and Handshake
    - Peers in a connection will check their connected Peers to see if they are right by using their Peer ID and Handshake Header. Bitfield messages are then exchanged, and depending on them, the peer will either send a message saying it is interested or not interested.
      
- Choke and Unchoke
    - The Peer picks k Preferred Neighbors and 1 Optimistically Unchoked neighbor based on their respective specified time intervals. The Peer unchokes Interested Preferred Neighbor Peers based on their Download Rates, and does so with the Optimistically Unchoked Neighbor randomly among the Interested and Choked Neighbor Peers.
      
- Interested and Not Interested
    - Based on the Bitfield, the Peer will send Interested, Not Interested, or Have messages.
      
- Request and Piece
    - The Peer will unchoke the Neighbor Peer and randomly pick a piece from the Neighbor which it does not have yet. After sending the Requet message, the Neighbor Peer will send a Message containing the Piece and this exchange will go on until the the Neighbor Peer is choked or it does not have anymore Pieces of Interest.
## Playbook

### Connect to Remote CISE Machines


### Unzipping the Project Folder
    Put cmd code here for unzipping tar file

#### Compile
    javac PeerClient.java   
#### Run
    java PeerClient [PeerID]

### Instructions
Use the Run Command in different Terminal Windows, one time for each Peer that is listed in [PeerInfo.cfg]. Once all Peers are started, the program will run and they will all connect to each and start the file sharing process. Peer logs for each Peer will be generated after the program has finished running. They will be genereated in the same folder containing all Java files. The file sent to the Peers will be stored in their respective sub-folders, such as folder [peer1002] for Peer 1002, once all pieces have been recieved by all Peers.
