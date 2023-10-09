# Sample Sender/Receiver/Network Relay Server
### Compilation
The code works on Windows/Linux/Unix, and can be compiled and ran the same way on each device.
The program is written in Java, so a JDK (preferably 1.8 or higher) or JRE is required to be
installed on the device to compile and run the code.
The code can be compiled into jar files and ran using the commands:
`make network`
`java -jar network.jar [port]`

`make receiver`
`java -jar receiver.jar [serverURL/IP] [port]`

`make sender`
`java -jar sender.jar [serverURL/IP] [port] [fileName]`

Alternatively, the code can be run directly without compiling into a jar by using
`cd src`
to change the directory to the source directory, and running the following lines:
`javac network.java`
`javac receiver.java`
`javac sender.java`
to compile the each component, and then using
`java network [port]`
`java receiver [serverURL/IP] [port]`
`java sender [serverURL/IP] [port] [fileName]`
to run each component.
Note that the files must be run in the order Network -> Receiver -> Sender,
but what device each of the files is run on does not matter, as long as all of them connect
to the IP address of the device that ran Network. All parameters are required for each of the components to function.
Additionally, the fileName given to the Sender must exist and be readable by Sender. This is best accomplished by placing
the file to be read in the same directory as the jar files (ie. the root directory).

### Functions
Only the source files in /src/ are required for the program to function, ie. network.java, receiver.java, and sender.java.
The project is structured to allow the source files to be compiled into jar files, but this is not required for them to function.
None of the components accept user input, all input must be given as command line arguments when running them.
Each component terminates itself once the message has been successfully transmitted, first the sender, then the network, and finally
the receiver.

### Results
When testing, no bugs were found in the system. The system behaves exactly as specified in the specification document.
However, the files being read are not sanitized extensively before being used as input,
so inputting a file with raw binary data and unorthodox whitespace conventions could cause issues, but this could not be replicated.
If the sender does not successfully send the message to the receiver or the network or receiver experience an IO failure, then
the programs will not automatically terminate. Additionally, the network assumes it will get one connection from a receiver
and one connection from a sender, and no more or less. If the network receives 2 different connections from a sender or receiver,
it will replace the old connection with a new one, until it receives connections of both types, after which it exits when both
connections have ended.

### Code Structure
Due to the nature of having separate source files running on different machines, each of the three files have some redundancy.
The Packet class is defined for each class separately, despite having almost identical functionality for all three parts.
The class allows text to be formatted into packets, transmitted as a string over the network, and be interpreted from the 
transmitted hex digest string. The Packet class also calculates the checksum of the packet and allows the checksum to be checked
for correctness. The bytesToHex(), byteToHex(), hexToBytes(), hexToByte(), and toDigit() functions are also implemented for each part to allow
for easy and reliable transmission of hex data between components.

#### Network.java
The Network class also implements a NetworkClientThread class that allows the Network to listen to input from two connections
simultaneously. The Network opens a new server socket and then listens to handshakes from connections to that socket from clients
until it is connected to one receiver and one sender. It then opens up two NetworkClientThreads, one for reading input from the sender
and writing it to the receiver (or back to the sender if the packet is dropped), and one for reading ACK packets from the receiver and
writing it to the sender. One class accomplishes both of this. The network simulates an unreliable connection by choosing a random
action to perform on received packets (pass, corrupt, or drop the packet) before sending it to the target recipient.
When the network receives a -1 message from the sender, it closes the threads and sends the same terminate command to the receiver before closing itself.

#### Receiver.java
The Receiver class opens a connection to the Network relay and listens to input from the network until a complete message is received
followed by a termination command -1. If the Receiver receives a corrupt packet, or the wrong packet, it sends an ACK to the network
in compliance with RDT 3.0 to request the sender to resend that packet. Once all the packets have been received, the receiver
reconstructs the message (without newlines) and prints it out.

#### Sender.java
The Sender class reads a file and breaks it up into packets to send over a network relay and 
opens a connection to the Network and waits for a valid hankshake before starting transmitting packets.
Once the sender receives a valid handshake, it starts sending the packets to the receiver through the network
one at a time, waiting for an ACK before sending the next message. If the sender receives the wrong ACK, it retransmits
the packet until it receives a valid ACK. Once the sender has sent all packets, it sends a -1 terminate command to network
and closes its connection.

#### Sample output (Screenshots formatted into .txt)
##### Network
$make network
$java -jar network.jar 9328
Network started on port 0.0.0.00.0.0.0:9328
get connection from receiver 127.0.0.1
get connection from sender 127.0.0.1
Threads started, waiting on them to finish.
Sender Received: PACKET0 1, CORRUPT
Receiver Received: ACK1, DROP
Receiver Received: ACK1, PASS
Sender Received: PACKET0 1, DROP
Sender Received: PACKET0 1, PASS
Receiver Received: ACK0, DROP
Receiver Received: ACK0, DROP
Receiver Received: ACK0, PASS
Sender Received: PACKET1 2, DROP
Sender Received: PACKET1 2, PASS
Receiver Received: ACK1, CORRUPT
Sender Received: PACKET0 3, PASS
Receiver Received: ACK0, PASS
Sender Received: PACKET1 4, DROP
Sender Received: PACKET1 4, DROP
Sender Received: PACKET1 4, DROP
Sender Received: PACKET1 4, PASS
Receiver Received: ACK1, DROP
Receiver Received: ACK1, PASS
Sender Received: PACKET0 5, PASS
Receiver Received: ACK0, PASS
Sender Received: PACKET1 6, PASS
Receiver Received: ACK1, DROP
Receiver Received: ACK1, PASS
Sender Received: PACKET0 7, PASS
Receiver Received: ACK0, DROP
Receiver Received: ACK0, CORRUPT
Sender Received: PACKET1 8, PASS
Receiver Received: ACK1, DROP
Receiver Received: ACK1, CORRUPT
Sender Received: PACKET0 9, DROP
Sender Received: PACKET0 9, DROP
Sender Received: PACKET0 9, DROP
Sender Received: PACKET0 9, CORRUPT
Receiver Received: ACK1, CORRUPT
Sender Received: PACKET0 9, PASS
Receiver Received: ACK0, DROP
Receiver Received: ACK0, PASS
Sender Received: PACKET1 10, PASS
Receiver Received: ACK1, DROP
Receiver Received: ACK1, CORRUPT
Sender Received: PACKET0 11, PASS
Receiver Received: ACK0, CORRUPT
Sender thread exiting...
Sender thread finished.
Receiver thread finished.
Threads finished.
Network closed successfully
*Network terminated*

##### Receiver
$make receiver
$java -jar receiver.jar 0.0.0.0 9328
receive: Network Hello!
Waiting 0, 1, 1, PACKET0 1 318 You, ACK1
Waiting 0, 2, 1, ACK2, ACK1
Waiting 0, 3, 1, PACKET0 1 317 You, ACK0
Waiting 1, 4, 2, ACK2, ACK0
Waiting 1, 5, 2, ACK2, ACK0
Waiting 1, 6, 2, PACKET1 2 312 are, ACK1
Waiting 0, 7, 3, PACKET0 3 230 my, ACK0
Waiting 1, 8, 4, PACKET1 4 923 sunshine., ACK1
Waiting 0, 9, 5, ACK2, ACK1
Waiting 0, 10, 5, PACKET0 5 198 My, ACK0
Waiting 1, 11, 6, PACKET1 6 450 only, ACK1
Waiting 0, 12, 7, ACK2, ACK1
Waiting 0, 13, 7, PACKET0 7 921 sunshine,, ACK0
Waiting 1, 14, 8, ACK2, ACK0
Waiting 1, 15, 8, PACKET1 8 317 You, ACK1
Waiting 0, 16, 9, ACK2, ACK1
Waiting 0, 17, 9, PACKET0 9 415 make, ACK1
Waiting 0, 18, 9, PACKET0 9 414 make, ACK0
Waiting 1, 19, 10, ACK2, ACK0
Waiting 1, 20, 10, PACKET1 10 210 me, ACK1
Waiting 0, 21, 11, ACK2, ACK1
Waiting 0, 22, 11, PACKET0 11 592 happy., ACK0
Received message: You are my sunshine. My only sunshine, You make me happy.
*receiver terminated*

##### Sender
$make sender
$java -jar sender.jar 0.0.0.0 9328 message.txt
receive: Network Hello!
Sending message as packets:
[1]: You, ACK0, digest: 00010000013d596f75
[2]: are, ACK1, digest: 010200000138617265
[3]: my, ACK0, digest: 0003000000e66d79
[4]: sunshine., ACK1, digest: 01040000039b73756e7368696e652e
[5]: My, ACK0, digest: 0005000000c64d79
[6]: only, ACK1, digest: 0106000001c26f6e6c79
[7]: sunshine,, ACK0, digest: 00070000039973756e7368696e652c
[8]: You, ACK1, digest: 01080000013d596f75
[9]: make, ACK0, digest: 00090000019e6d616b65
[10]: me, ACK1, digest: 010a000000d26d65
[11]: happy., ACK0, digest: 000b0000025068617070792e
Waiting ACK0, 1, CORRUPT, resend Packet0
Waiting ACK0, 2, DROP, resend Packet0
Waiting ACK0, 3, PASS, sending next Packet1
Waiting ACK1, 4, DROP, resend Packet1
Waiting ACK1, 5, PASS, sending next Packet0
Waiting ACK0, 6, PASS, sending next Packet1
Waiting ACK1, 7, DROP, resend Packet1
Waiting ACK1, 8, DROP, resend Packet1
Waiting ACK1, 9, DROP, resend Packet1
Waiting ACK1, 10, PASS, sending next Packet0
Waiting ACK0, 11, PASS, sending next Packet1
Waiting ACK1, 12, PASS, sending next Packet0
Waiting ACK0, 13, PASS, sending next Packet1
Waiting ACK1, 14, PASS, sending next Packet0
Waiting ACK0, 15, DROP, resend Packet0
Waiting ACK0, 16, DROP, resend Packet0
Waiting ACK0, 17, DROP, resend Packet0
Waiting ACK0, 18, CORRUPT, resend Packet0
Waiting ACK0, 19, PASS, sending next Packet1
Waiting ACK1, 20, PASS, sending next Packet0
Waiting ACK0, 21, no more packets to send
*sender terminated*