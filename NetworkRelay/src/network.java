import java.net.*;
import java.io.*;

public class network {
    // Input constants
    private static final String EXIT_CMD = "exit";
    private static final String SENDER_HELLO = "Sender Hello!";
    private static final String RECEIVER_HELLO = "Receiver Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";

    // Return codes
    private static final int SERVER_OK = 0;

    // Components
    private ServerSocket networkSocket;
    private Socket senderSocket;
    private Socket receiverSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String senderIP;
    private String receiverIP;
    private NetworkClientThread networkSenderThread;
    private NetworkClientThread networkReceiverThread;

    // Handles connecting clients to the server
    private void handleNetwork() {
        try {
            // Keep accepting new connections until a negative return flag (error or terminate) is sent
            while (true) {
                // Connect to a client and start handling server operations until closed by client
                Socket tempSocket = networkSocket.accept();
                out = new PrintWriter(tempSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));

                // Only start server operations on correct handshake
                String handshake = in.readLine();
                if (handshake.equals(SENDER_HELLO)) {
                    senderIP = ((InetSocketAddress)tempSocket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
                    System.out.println("get connection from sender " + senderIP);
                    out.println(NETWORK_HELLO);
                    senderSocket = tempSocket;
                } else if (handshake.equals(RECEIVER_HELLO)) {
                    receiverIP = ((InetSocketAddress)tempSocket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
                    System.out.println("get connection from receiver " + receiverIP);
                    out.println(NETWORK_HELLO);
                    receiverSocket = tempSocket;
                } else {
                    System.out.println("Client attempted to connect with invalid handshake: " + handshake);
                    out.println("Handshake not recognized");
                }

                // Start threads when connections have been established
                if (receiverSocket != null && senderSocket != null) {
                    networkReceiverThread = new NetworkClientThread(receiverSocket, senderSocket);
                    networkSenderThread = new NetworkClientThread(senderSocket, receiverSocket);
                    networkSenderThread.start();
                    networkReceiverThread.start();
                    
                    // Wait on threads to finish to close connection
                    try {
                        System.out.println("Threads started, waiting on them to finish.");
                        synchronized (networkReceiverThread) {
                            networkReceiverThread.wait();
                        }
                        synchronized (networkSenderThread) {
                            networkSenderThread.wait();
                        }
                        System.out.println("Threads finished.");
                    } catch (InterruptedException e) {
                        System.out.println("Thread(s) interrupted: " + e.toString());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Network closed with error: " + e.toString());
            return;
        }
    }

    // Starts the network on the given port
    private void startNetwork(int port) {
        try {
            networkSocket = new ServerSocket(port);
            System.out.println("Network started on port " + port);
            handleNetwork();
        } catch (IOException e) {
            System.out.println("Network closed with error: " + e.toString());
            return;
        }
    }

    // Stops the network, closing all connections
    private void stopNetwork() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (senderSocket != null) {
                senderSocket.close();
            }
            if (receiverSocket != null) {
                receiverSocket.close();
            }
            if (networkSocket != null) {
                networkSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Network closed with error: " + e.toString());
            return;
        }
        System.out.println("Network closed successfully");
    }

    // Main: Parses input params and starts the network
    public static void main(String[] args) {
        network curNetwork = new network();
        int portNum = 0;

        // Only start network if port number is explicitly given
        if (args.length < 1) {
            System.out.println("Could not start network: No port number given.");
            return;
        }
        
        // Get port number
        try {
            portNum = Integer.parseInt(args[0]);
            if(portNum < 0 || portNum > 65535) {
                throw new NumberFormatException("Invalid number.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Could not start network: Port must be number in range [0-65535]");
            return;
        }

        // Start server
        curNetwork.startNetwork(portNum);
        curNetwork.stopNetwork();
    }

    private static class NetworkClientThread extends Thread {
        private Socket inSocket;
        private Socket outSocket;
        private PrintWriter out;
        private BufferedReader in;

        public NetworkClientThread(Socket _inSocket, Socket _outSocket) {
            this.inSocket = _inSocket;
            this.outSocket = _outSocket;
        }

        public void run() {
            try {
                out = new PrintWriter(inSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(inSocket.getInputStream()));
    
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.equals(EXIT_CMD)) {
                        out.println("Thread exiting...");
                        break;
                    }
                    out.println("Received input: " + inputLine);
                }
    
                in.close();
                out.close();
                inSocket.close();
                outSocket.close();
            } catch (IOException e) {
                System.out.println("Network thread closed with error: " + e.toString());
                return;
            }
        }
    }
}