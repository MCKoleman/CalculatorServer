import java.net.*;
import java.io.*;
import java.util.Random;

public class network {
    // Input constants
    private static final String EXIT_CMD = "-1";
    private static final String SENDER_HELLO = "Sender Hello!";
    private static final String RECEIVER_HELLO = "Receiver Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";
    private static final String RECEIVER = "Receiver";
    private static final String SENDER = "Sender";

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

    // Network: Connects sender to receiver and transmits messages between them
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
                return new Packet(DROP_PACKET);
        }
    }

    // Converts byte array to hex
    private static String bytesToHex(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    // Converts the byte to hex
    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    // Converts hex to byte array
    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = hexToByte(hex.substring(i, i + 2));
        }
        return bytes;
    }

    // Converst the hex character to byte
    private static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    // Converts the given character into hex
    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
              "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    /* ====================================== PACKET CLASS ========================= */
    private static class Packet {
        private byte ack;
        private byte id;
        private int checksum;
        private String content;
        private String packet;

        // Make packet from another packet's digest
        public Packet(String _packet) {
            this.packet = _packet;

            // If the packet length is negative, 0, or 1, it is invalid
            if (packet.length() <= 1) {
                return;
            }

            // Unpack packet
            byte[] byteList = hexToBytes(packet);
            ack = byteList[0];
            // If the packet length is 2, the format is ACK
            if(packet.length() <= 2) {
                this.checksum = byteList[1];
                byte[] bytePacket = new byte[2];
                bytePacket[0] = ack;
                bytePacket[1] = (byte)checksum;
                this.packet = bytesToHex(bytePacket);
                return;
            }

            // If the packet length is normal, create regular packet
            this.checksum = 0;
            this.content = "";
            id = byteList[1];
            for (int i = 2; i < byteList.length; i++) {
                if (i < 6) {
                    checksum = (checksum << 8) + (byteList[i] & 0xFF);
                } else {
                    content += (char)((int)byteList[i]);
                }
            }
        }

        // If only an ACK is provided, the packet format is ACK
        public Packet(int _ack) {
            this.ack = (byte)(_ack & 0x000000FF);
            this.id = 0;
            this.content = "";
            this.checksum = 0;
            byte[] bytePacket = new byte[2];
            bytePacket[0] = ack;
            bytePacket[1] = (byte)checksum;
            this.packet = bytesToHex(bytePacket);
        }

        // Makes a packet out of the given content
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

        // Returns the packet as a string
        public String toString() {
            if (content.equals("")) {
                return "ACK" + (int)ack;
            } else {
                return "PACKET" + (int)ack + " " + (int)id;
            }
        }

        public String getPacket() { return packet; }
        public byte getAck() { return ack; }
        public byte getID() { return id; }
        public int getChecksum() { return checksum; }
        public String getContent() { return content; }
    }

    /* ====================================== NETWORK CLIENT THREAD CLASS ========================= */
    private static class NetworkClientThread extends Thread {
        private Socket inSocket;
        private Socket outSocket;
        private PrintWriter out;
        private PrintWriter backToSender;
        private BufferedReader in;
        private String name;

        // Make new network client thread
        public NetworkClientThread(Socket _inSocket, Socket _outSocket, String _name) {
            this.inSocket = _inSocket;
            this.outSocket = _outSocket;
            this.name = _name;
        }

        // Runs the thread
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