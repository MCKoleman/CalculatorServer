# Sample Calculator Server
### Compilation
The code works on Windows/Linux/Unix, and can be compiled and ran the same way on each device.
The program is written in java, so a JDK (preferably 1.8 or higher) or JRE is required to be
installed on the device to compile and run the code.
The code can be compiled into jar files and ran using the commands:  
`make client`  
`java -jar client.jar [server_url/ip] [port]`  

`make server`  
`java -jar server.jar [port]`  

Alternatively, the code can be run directly without compiling into a jar by using  
`cd src`  
to change the directory to the source directory, and running  
`javac client.java`  
or  
`javac server.java`  
to compile the client and server respectively, and then using  
`java client [server_url/ip] [port]`  
or  
`java server [port]`  
to run the client and server respectively.  

### Functions
Only src/client.java and src/server.java are required for the program to function. The project is 
structured to allow the source files to be compiled into jar files, but this is not required for them to function.

### Results
The client and server behave exactly as specified in the specification document. No abnormal results or bugs were found.
Whenever the server receives input, it parses the input and returns the correct result, either an error code or
the correct calculation. The program is limited in only accepting the three types of arithmetic operations
(add, subtract, multiply) and in only being able to connect to one client at a time (ie. it does not use parallel
processing or threads). These features could easily be added to the server though, but were not requested for this assignment.  

#### Client.java
When starting the client, both a valid server URL or IP address are required to be given, along with the port
the server is running on. If the client doesn't receive these values or the requested server does not exist,
the client closes itself with an error. If the server exists, the client attempts to start a connection with it
using `startConnection()` to open a socket between the client and server.
The client attempts a handshake with the server, and if it receives the correct response
it starts handling client-side operations with `handleClient()`, which continues until shut down, calling `stopConnection()`
to close the socket.
While connected to the server, the client continually accepts user input, sending each line to server for validation.
The server then returns the result of the input, either as an error code or the result of the calculation, and the client
displays the result. When receiving -5 from the server, the client shuts down.  

#### Server.java
The server must be started before the client. When the server is ran, it opens a socket with
`startServer()` Once the server is active, it can connect to one client at a time,
but does not need to be restarted between client connections. The server handles connecting to clients in
`handleServer()`, and handles input from clients in `handleClient()` while it is connected to a client.
Any valid input from the client is parsed in `handleCalculation()` and the result is returned to the client.  

#### Sample output
##### Server
-make server  
-java -jar server.jar 9328  
-Server started on port 9328  
-get connection from ip.ip.ip.ip  
-get: add 3 2, return: 5  
-get: subtract 15 2 1, return: 12  
-get: multiply 4 3 2 1, return: 24  
-get: ask 12, return -1  
-get: add 1, return -2  
-get: multiply 1 2 3 4 5, return -3  
-get: add 1 two, return -4  
-get: exit, return -1  
-Closing connection to client ip.ip.ip.ip  
-get connection from ip.ip.ip.ip  
-Closing connection to client ip.ip.ip.ip  
-Server closed successfully  
*server terminated*  

##### Client
-make client  
-java -jar client.jar ip.ip.ip.ip 9328  
-receive: Hello!  
-add 3 2  
-receive: 5  
-subtract 15 2 1  
-receive 12  
-multiply 4 3 2 1  
-receive 24  
-ask 12  
-receive: -1: incorrect operation command  
-add 1  
-receive: -2: number of inputs is less than two  
-multiply 1 2 3 4 5  
-receive: -3: number of inputs is less than two  
-add 1 two  
-receive: -4: one or more of the inputs contain(s) non-number(s)  
-exit  
-receive: -1: incorrect operation command  
-bye  
-receive: -5: exit  
*client terminated*  
-java -jar client.jar ip.ip.ip.ip 9328  
-receive: Hello!  
-terminate  
-receive: -5: exit  
*client terminated*  