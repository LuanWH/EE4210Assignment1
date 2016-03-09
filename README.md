# EE4210Assignment1 - SyncPeer by Luan Wenhao

###Introduction

Import the project to Eclipse and run SyncStarter with parameters. 

Alternatively, you can build and run the program in unix terminal following instructions below.

Navigate to `src` folder:

`cd src`

Compile the program by running:

`javac syncpeer/SyncStarter.java`

or

``javac `find . -name '*.java'```

for a clean build.

After build, run the first peer by 

`java syncpeer.SyncStarter <sync folder>`

An example folder `SyncFiles` and a few examples files are provided:

`java syncpeer.SyncStarter ../SyncFiles`

Start another command window and run the second peer by 

`java syncpeer.SyncStarter <sync folder> <ip address>`

It will automatically connect to the first peer and start the synchronization.

Alternatively, you can start the second peer in the same way as the first peer, and using commands to establish the connection.

###Commands

Once the peers are started, you can input commands into the console window.

Note that all commands are _NOT_ case-sensitive. 

##### Synchronization

Type in 

`CONNECT <ip address>`

and the peer will try to connect to the ip address provided. 
Once the connection is established, the file synchronization will automatically begin.
The connection will be closed after file synchronization is finished.

##### Exit

A peer will not exit without user commands. Type in 

`EXIT`

to terminate the peer. Any ongoing connection session will be interrupted and therefore
file integrity might be compromised. Handle with care!
