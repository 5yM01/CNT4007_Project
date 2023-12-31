# <u>CNT4007 P2P Group Project</u>

## Group 27
##### <i>Syed Mahdi (smahdi@ufl.edu)</i> 
##### <i>Endrick Lafosse (elafosse@ufl.edu)</i>
##### <i>Stella Rojas (s.rojas@ufl.edu)</i>

## Team Member Contributions
**Syed Mahdi**
1. Unchoke/Choke Protocol
2. Sending Files/Downloading Files over TCP Connections
3. Supplemented code for Peer Representation
4. Testing and Debugging Project
5. Documentation

**Stella Rojas**
1. Testing and Debugging Project in CSE Linux Environment
2. Documentation
3. Minor Formatting for Start Script for CSE Linux Environment
4. Console Checkpoint Output
5. Standardization
6. File Verification

**Endrick Lafosse**
1. Java Classes
2. Peer Piece Exchange
3. Testing and Debugging Project

## Video Link for Demo 
https://youtu.be/QLVzTvUE520

## Achievements
Successfully implemented each of the protocols outlined in the Project Description. [Some errors may exist but they do not affect the correctness of the program or break it in a significant way].

## Brief Overview
Our project is a version of the P2P file-sharing software BitTorrent. It shares files between a number of peers using the protocols described below, derived from the project documentation. 

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
    - The Peer will unchoke the Neighbor Peer and randomly pick a piece from the Neighbor which it does not have yet. After sending the Request message, the Neighbor Peer will send a Message containing the Piece and this exchange will go on until the the Neighbor Peer is choked or it does not have anymore Pieces of Interest.

## Playbook
### Connect to Remote CISE Machines
- Connect to any of the CISE Linux Machines (eg. storm) using your uf credentials. 

### Unzipping the Project Folder
    tar -xvf projectGroup27.tar

#### Manual Execution
##### Compile
    javac PeerProcess.java
##### Run
    java peerProcess [PeerID]
##### Instructions
- Use the Run Command in different Terminal Windows, one time for each Peer that is listed in [PeerInfo.cfg]. Once all Peers are started, the program will run and they will all connect to each and start the file sharing process. Peer logs for each Peer will be generated after the program has finished running. They will be genereated in the same folder containing all Java files. The file sent to the Peers will be stored in their respective sub-folders, such as folder [peer_1002] for Peer 1002, once all pieces have been recieved by all Peers. These sub-folders for each Peer should be created by the user in the directory that contains the Java files before running the program. The file being sent should be stored in the folder of the Peer the [PeerInfo.cfg] file indicates has the file.

# Additional Details
To test (5) peers, configure PeerInfo.cfg to contain only the following data:

    1001 lin114-00.cise.ufl.edu 6011 1 
    1002 lin114-01.cise.ufl.edu 6011 0 
    1003 lin114-02.cise.ufl.edu 6011 0 
    1004 lin114-03.cise.ufl.edu 6011 0 
    1005 lin114-04.cise.ufl.edu 6011 0 
