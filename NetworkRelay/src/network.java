import java.net.*;
import java.io.*;
import java.util.Random;

public class network {
    // Input constants
    private static final String EXIT_CMD = "-1";
    private static final String SENDER_HELLO = "Sender Hello!";
    private static final String RECEIVER_HELLO = "Receiver Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";
    private static final String RECEIVER = "receiver";
    private static final String SENDER = "sender";

    // Network probabilities
    private static final float PASS_CHANCE = 0.5f;
    private static final float CORRUPT_CHANCE = 0.25f;
    private static final float DROP_CHANCE = 0.25f;

    // Return codes
    private static final int INVALID_OPERATION = -1;
    private static final int PASS_PACKET = 0;
    private static final int CORRUPT_PACKET = 1;
    private static final int DROP_PACKET = 2;

    // Components
    private ServerSocket networkSocket;
    private Socket senderSocket;
    private Socket receiverSocket;
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
                PrintWriter tempOut = new PrintWriter(tempSocket.getOutputStream(), true);
                BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));
                
                // Only start server operations on correct handshake
                String handshake = tempIn.readLine();
                if (handshake.equals(SENDER_HELLO)) {
                    senderIP = ((InetSocketAddress)tempSocket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
                    System.out.println("get connection from sender " + senderIP);
                    tempOut.println(NETWORK_HELLO);
                    senderSocket = tempSocket;
                } else if (handshake.equals(RECEIVER_HELLO)) {
                    receiverIP = ((InetSocketAddress)tempSocket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
                    System.out.println("get connection from receiver " + receiverIP);
                    tempOut.println(NETWORK_HELLO);
                    receiverSocket = tempSocket;
                } else {
                    System.out.println("Client attempted to connect with invalid handshake: " + handshake);
                    tempOut.println("Handshake not recognized");
                }

                // Start threads when connections have been established
                if (receiverSocket != null && senderSocket != null) {
                    networkReceiverThread = new NetworkClientThread(receiverSocket, senderSocket, RECEIVER);
                    networkSenderThread = new NetworkClientThread(senderSocket, receiverSocket, SENDER);
                    networkSenderThread.start();
                    networkReceiverThread.start();
                    
                    // Wait on threads to finish to close connection
                    try {
                        System.out.println("Threads started, waiting on them to finish.");
                        synchronized (networkSenderThread) {
                            networkSenderThread.wait();
                            System.out.println("Sender thread finished.");
                        }
                        synchronized (networkReceiverThread) {
                            networkReceiverThread.wait();
                            System.out.println("Receiver thread finished.");
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
            String selfIP = ((InetSocketAddress)networkSocket.getLocalSocketAddress()).getAddress().toString().replace("/", "");
            System.out.println("Network started on port " + selfIP + ":" + port);
            handleNetwork();
        } catch (IOException e) {
            System.out.println("Network closed with error: " + e.toString());
            return;
        }
    }

    // Stops the network, closing all connections
    private void stopNetwork() {
        try {
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

    // Returns a random packet action
    private static int getRandomPacketAction() {
        Random rand = new Random();
        float chanceTotal = PASS_CHANCE + CORRUPT_CHANCE + DROP_CHANCE;
        float rng = rand.nextFloat() / chanceTotal;
        if (rng <= PASS_CHANCE) {
           return PASS_PACKET; 
        } else if (rng <= PASS_CHANCE + CORRUPT_CHANCE) {
            return CORRUPT_PACKET;
        } else if (rng <= PASS_CHANCE + CORRUPT_CHANCE + DROP_CHANCE) {
            return DROP_PACKET;
        } else {
            return INVALID_OPERATION;
        }
    }

    // Returns the action as a string
    private static String getActionText(int action) {
        switch (action) {
            case PASS_PACKET:
                return "PASS";
            case CORRUPT_PACKET:
                return "CORRUPT";
            case DROP_PACKET:
            default:
                return "DROP";
        }
    }

    // Returns a packet modified with a random packet action
    private static Packet getModifiedPacket(Packet packet, int action) {
        switch (action) {
            case PASS_PACKET:
                return packet;
            case CORRUPT_PACKET:
                return new Packet(Packet.makePacket(packet.getAck(), packet.getID(), packet.getChecksum()+1, packet.getContent()));
            case DROP_PACKET:
            default:
                return new Packet(DROP_PACKET, 0, "");
        }
    }

    // Converts bytes to hex
    private static String bytesToHex(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    // Converts hex to bytes
    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = hexToByte(hex.substring(i, i + 2));
        }
        return bytes;
    }

    private static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
              "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    private static class Packet {
        private byte ack;
        private byte id;
        private int checksum;
        private String content;
        private String packet;

        public Packet(String _packet) {
            this.packet = _packet;
            this.checksum = 0;
            this.content = "";
            byte[] byteList = hexToBytes(packet);
            ack = byteList[0];
            id = byteList[1];
            for (int i = 2; i < byteList.length; i++) {
                if (i < 6) {
                    checksum = (checksum << 8) + (byteList[i] & 0xFF);
                } else {
                    content += (char)((int)byteList[i]);
                }
            }
        }

        public Packet(int _ack, int _id, String _content) {
            this.ack = (byte)(_ack & 0x000000FF);
            this.id = (byte)(_id & 0x000000FF);
            this.content = _content;
            
            checksum = calculateChecksum(content);
            packet = makePacket(ack, id, checksum, content);
        }

        public static String makePacket(int _ack, int _id, int _checksum, String _content) {
            byte[] bytePacket = new byte[_content.length() + 6];
            bytePacket[0] = (byte)(_ack & 0x000000FF);
            bytePacket[1] = (byte)(_id & 0x000000FF);
            bytePacket[2] = (byte)((_checksum & 0xFF000000) >> 24);
            bytePacket[3] = (byte)((_checksum & 0x00FF0000) >> 16);
            bytePacket[4] = (byte)((_checksum & 0x0000FF00) >> 8);
            bytePacket[5] = (byte)((_checksum & 0x000000FF) >> 0);
            for (int i = 0; i < _content.length(); i++) {
                bytePacket[i + 6] = (byte)((int)_content.charAt(i) & 0x000000FF);
            }
            return bytesToHex(bytePacket);
        }

        // Calculates the checksum for the given content
        public static int calculateChecksum(String content) {
            int tempSum = 0;
            for (int i = 0; i < content.length(); i++) {
                tempSum += (int)content.charAt(i);
            }
            return tempSum;
        }

        // Returns the packet as a string
        public String toString() {
            if (content.equals("")) {
                return "ACK" + (int)ack;
            } else {
                return "Packet" + (int)id + ", " + checksum + ", ACK" + (int)ack + ", CONTENT: " + content + ", DIGEST: " + packet;
            }
        }

        public String getPacket() { return packet; }
        public byte getAck() { return ack; }
        public byte getID() { return id; }
        public int getChecksum() { return checksum; }
        public String getContent() { return content; }
    }

    private static class NetworkClientThread extends Thread {
        private Socket inSocket;
        private Socket outSocket;
        private PrintWriter out;
        private PrintWriter backToSender;
        private BufferedReader in;
        private String name;

        public NetworkClientThread(Socket _inSocket, Socket _outSocket, String _name) {
            this.inSocket = _inSocket;
            this.outSocket = _outSocket;
            this.name = _name;
        }

        public void run() {
            try {
                out = new PrintWriter(outSocket.getOutputStream(), true);
                backToSender = new PrintWriter(inSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(inSocket.getInputStream()));
    
                String inputLine;
                while (in != null && (inputLine = in.readLine()) != null) {
                    if (inputLine.equals(EXIT_CMD)) {
                        System.out.println(name + " thread exiting...");

                        // Relay termination command from sender to receiver
                        if (name.equals(SENDER)) {
                            out.println(EXIT_CMD);
                        }
                        break;
                    }
                    Packet inputPacket = new Packet(inputLine);

                    // Perform AT&T Wi-Fi simulation
                    int action = getRandomPacketAction();
                    String outPacket = getModifiedPacket(inputPacket, action).getPacket();
                    if (action == DROP_PACKET) {
                        backToSender.println(outPacket);
                    } else {
                        out.println(outPacket);
                    }
                    System.out.println(name + " Received: " + inputPacket.toString() + ", " + getActionText(action));
                }
            } catch (IOException e) {
                System.out.println(name + " thread closed with error: " + e.toString());
                e.printStackTrace();
                return;
            }
        }
    }
}