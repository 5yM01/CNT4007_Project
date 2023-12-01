step 1: you need to agree 'yes' to connect to each machine (eg. lin114-00, lin114-01). There is probably a way to detect if this is necessary and automate it, but idk how so just run this command for whichever you haven't accessed before:
ssh lin114-00.cise.ufl.edu; yes; exit
ssh lin114-01.cise.ufl.edu; yes; exit
ssh lin114-02.cise.ufl.edu; yes; exit
ssh lin114-03.cise.ufl.edu; yes; exit
ssh lin114-04.cise.ufl.edu; yes; exit

step 2: Upload and unzip the directory with all the java files. dump StartRemotePeers into src. cd into src. 
this is the correct contents of PeerInfo.cfg. update if needed:
1001 lin114-00.cise.ufl.edu 6011 1 
1002 lin114-01.cise.ufl.edu 6011 0 
1003 lin114-02.cise.ufl.edu 6011 0 
1004 lin114-03.cise.ufl.edu 6011 0 
1005 lin114-04.cise.ufl.edu 6011 0 

step 3: run:
./compileJava; java StartRemotePeers
The console should output confirmation of all (5) remote clients being activated. If needed, run chmod +x compileJava to add executable permission to the file. 

step 4: Nano into all the log_peer files to confirm execution of program. The file may take a few minutes to finish sending to all users so be patient. 
