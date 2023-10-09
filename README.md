# Sample Server Applications
### Applications
This repository contains three sample projects that show how network code works in Java:
- A [Calculator](/Calculator/) server that allows a client to send calculation operations to a server which calculates
the results and sends it back to the client;
- A [Link State Routing](/LinkStateRouting/) class that implements Dijkstra's Algorithm to calculate the best path between different networks in the given network graph.
- A [Network Relay](/NetworkRelay/) server where a sender (first client) sends information to the network (server) 
which then sends that information to the receiver (second client), sending appropriate acknowledgements
indicating the status and success of each message. This sample implements the RDT 3.0 protocol.

### Purpose
I created these sample server applications to learn how to write network code for Computer Network Fundamentals.
Please do not violate any academic honesty codes by using this source code for any school assignments.
The code is simply public for educational purposes.

### Compilation 
All programs works on Windows/Linux/Unix, and can be compiled and ran the same way on each device. 
The programs are written in Java, so a JDK (preferably 1.8 or higher) or JRE is required to be installed on the 
device to compile and run the code. The code can be compiled into jar files from their own folders 
and ran using `Make` or `javac`. Readme files in each of the subfolders have more instructions on how to compile
and run the individual applications.

### Sub-Projects
[Calculator](/Calculator/README.md)
[LinkStateRouting](/LinkStateRouting/README.md)
[NetworkRelay](/NetworkRelay/README.md)