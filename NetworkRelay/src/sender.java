import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class sender {
    private static final String SENDER_HELLO = "Sender Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";

    private Socket senderSocket;
    private PrintWriter out;
    private BufferedReader in;

    // Handles sending messages to the network until the sender is disconnected
    private void handleSender(String fileName) {
        // Read file
        ArrayList<Packet> packetList = new ArrayList<>();
        File file = new File(fileName);
        BufferedReader packetReader;
        String line;
        try {
            packetReader = new BufferedReader(new FileReader(file));
            while((line = packetReader.readLine()) != null) {
                packetList.addAll(messageToPackets(line));
            }
            packetReader.close();
        } catch (IOException e) {
            System.out.println("Sender could not read file: " + e.toString());
            return;
        }

        // Debug print packets to be sent
        System.out.println("Debug printing packets:");
        for (int i = 0; i < packetList.size(); i++) {
            System.out.println("[" + i + "]: " + packetList.get(i).getContent() + ", digest: " + packetList.get(i).getPacket());
        }

        // Send packets
        int packetIndex = 0;
        while (true) {
            // Continually send packets until the last packet is sent
            if (packetIndex >= packetList.size()) {
                break;
            }
            byte expectedAck = getOppositeAck(packetList.get(packetIndex).getAck());
            Packet returnPacket = new Packet(sendMessage(packetList.get(packetIndex).getPacket()));

            // Check ACK
            System.out.println(returnPacket.toString());
            if (returnPacket.getAck() == expectedAck) {
                packetIndex++;
            } else {
                System.out.println("Invalid ACK, requesting same packet again.");
            }
        }
        stopConnection();
    }

    // Sends a message to the network that the receiver is currently connected to
    private String sendMessage(String message) {
        try {
            out.println(message);
            String response = in.readLine();
            return response;
        } catch (IOException e) {
            System.out.println("Sender could not send message " + message + ": " + e.toString());
            return "";
        }
    }

    // Starts a connection with the network of the given IP and port, sending the given filename to it
    private void startConnection(String host, int port, String fileName) {
        try {
            senderSocket = new Socket(host, port);
            out = new PrintWriter(senderSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));

            // Greet server, and start client if greeting matches
            String response = sendMessage(SENDER_HELLO);
            if (response.equals(NETWORK_HELLO)) {
                System.out.println("receive: " + response);
                handleSender(fileName);
            }
        } catch (IOException e) {
            System.out.println("Sender could not connect to network " + host + ":" + port + ": " + e.toString());
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
            if (senderSocket != null) {
                senderSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Sender closed with error: " + e.toString());
        }
    }

    // Main: Connects to network relay
    public static void main(String[] args) {
        // Only start sender if port number and ip are explicitly given
        if (args.length < 2) {
            System.out.println("Could not start sender: No URL/port given.");
            return;
        }
        
        // Gather info needed to start connection
        sender curSender = new sender();
        String url = args[0];
        int portNum = 0;

        // Get port number
        try {
            portNum = Integer.parseInt(args[1]);
            if (portNum < 0 || portNum > 65535) {
                throw new NumberFormatException("Invalid number.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Could not start sender: Port must be number in range [0-65535]");
            return;
        }

        // Read file
        if (args.length < 3) {
            System.out.println("Could not start sender: No file given.");
            return;
        }
        String fileName = args[2];
        File file = new File(fileName);
        if (!file.isFile()) {
            System.out.println("Could not start sender: Could not find file: " + fileName);
            return;
        }

        // Connect to server
        curSender.startConnection(url, portNum, fileName);
        curSender.stopConnection();
    }

    // Returns the opposite ACK of the given ACK
    private static byte getOppositeAck(byte ack) {
        return (byte)((ack + 1) % 2);
    }

    //
    private static ArrayList<Packet> messageToPackets(String message) {
        String[] parts = message.trim().split("\\s+");
        ArrayList<Packet> packets = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            packets.add(new Packet(i % 2, i + 1, parts[i]));
        }
        return packets;
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
            return "PACKET" + (int)id + " ACK" + (int)ack + ", " + checksum + ", CONTENT: " + content;
        }

        public Boolean verifyChecksum() { return calculateChecksum(content) == checksum; }
        public String getPacket() { return packet; }
        public byte getAck() { return ack; }
        public byte getID() { return id; }
        public int getChecksum() { return checksum; }
        public String getContent() { return content; }
    }
}