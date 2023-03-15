import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class receiver {
    private static final String RECEIVER_HELLO = "Receiver Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";
    private static final String GOODBYE = "Goodbye!";

    private Socket receiverSocket;
    private PrintWriter out;
    private BufferedReader in;

    // Handles sending messages to the network until the receiver is disconnected
    private void handleReceiver() {
        ArrayList<Packet> packetList = new ArrayList<>();

        // Receive packets
        int expectedID = 1;
        byte expectedAck = 0;
        while (true) {
            try {
                // Check if packet is expected and correct
                Packet inputPacket = new Packet(in.readLine());
                if (inputPacket.getAck() == expectedAck && inputPacket.verifyChecksum() && inputPacket.getID() == expectedID) {
                    out.println(new Packet(expectedAck, 0, "").getPacket());
                    packetList.add(inputPacket);
                    expectedAck = getOppositeAck(expectedAck);
                    expectedID++;

                    // If last character of packet is '.', terminate connections
                    if (inputPacket.getContent().charAt(inputPacket.getContent().length()-1) == '.') {
                        out.println(GOODBYE);
                        break;
                    }
                } else {
                    out.println(new Packet(getOppositeAck(expectedAck), 0, "").getPacket());
                }
            } catch (IOException e) {
                System.out.println("Receiver could not read input.");
                return;
            }
        }

        // Reconstruct message from packets
        String message = "";
        for (int i = 0; i < packetList.size(); i++) {
            if (i != 0) {
                message += " ";
            }
            message += packetList.get(i).getContent();
        }
        System.out.println("Received message: " + message);
        stopConnection();
    }

    // Sends a message to the network that the receiver is currently connected to
    private String sendMessage(String message) {
        try {
            out.println(message);
            String response = in.readLine();
            return response;
        } catch (IOException e) {
            System.out.println("Receiver could not send message " + message + ": " + e.toString());
            return "";
        }
    }

    // Starts a connection with the network of the given IP and port
    private void startConnection(String host, int port) {
        try {
            receiverSocket = new Socket(host, port);
            out = new PrintWriter(receiverSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(receiverSocket.getInputStream()));

            // Greet server, and start client if greeting matches
            String response = sendMessage(RECEIVER_HELLO);
            if (response.equals(NETWORK_HELLO)) {
                System.out.println("receive: " + response);
                handleReceiver();
            }
        } catch (IOException e) {
            System.out.println("Receiver could not connect to network " + host + ":" + port + ": " + e.toString());
        }
    }

    // Closes the connection with the server
    private void stopConnection() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (receiverSocket != null) {
                receiverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Receiver closed with error: " + e.toString());
        }
    }

    // Main: Connects to network relay
    public static void main(String[] args) {
        // Only start receiver if port number and ip are explicitly given
        if (args.length < 2) {
            System.out.println("Could not start receiver: No URL/port given.");
            return;
        }
        
        // Gather info needed to start connection
        receiver curReceiver = new receiver();
        String url = args[0];
        int portNum = 0;

        // Get port number
        try {
            portNum = Integer.parseInt(args[1]);
            if (portNum < 0 || portNum > 65535) {
                throw new NumberFormatException("Invalid number.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Could not start receiver: Port must be number in range [0-65535]");
            return;
        }

        // Connect to server
        curReceiver.startConnection(url, portNum);
        curReceiver.stopConnection();
    }

    // Returns the opposite ACK of the given ACK
    private static byte getOppositeAck(byte ack) {
        return (byte)((ack + 1) % 2);
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
            byte[] byteList = packet.getBytes();
            for (int i = 0; i < byteList.length; i++) {
                if (i == 0) {
                    ack = byteList[i];
                } else if (i == 1) {
                    id = byteList[i];
                } else if (i < 6) {
                    checksum = (checksum << 8) + (byteList[i] & 0xFF);
                } else {
                    content += (char)byteList[i];
                }
            }
        }

        public Packet(int _ack, int _id, String _content) {
            this.ack = (byte)(ack & 0x000000FF);
            this.id = (byte)(id & 0x000000FF);
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
            return bytePacket.toString();
        }

        // Calculates the checksum for the given content
        public static int calculateChecksum(String content) {
            int tempSum = 0;
            for (int i = 0; i < content.length(); i++) {
                tempSum += (int)content.charAt(i);
            }
            return tempSum;
        }

        public String toString() {
            return "PACKET ACK" + (int)ack + ", ID#" + (int)id + ", SUM=" + ", CONTENT: " + content;
        }

        public Boolean verifyChecksum() { return calculateChecksum(content) == checksum; }
        public String getPacket() { return packet; }
        public byte getAck() { return ack; }
        public byte getID() { return id; }
        public int getChecksum() { return checksum; }
        public String getContent() { return content; }
    }
}