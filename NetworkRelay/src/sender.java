import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class sender {
    private static final String SENDER_HELLO = "Sender Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";
    private static final String EXIT_CMD = "-1";
    private static final int DROP_PACKET = 2;

    private Socket senderSocket;
    private PrintWriter out;
    private BufferedReader in;

    // Sender: Transmits a message to the receiver through the network relay
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
                packetList.addAll(messageToPackets(line.trim(), packetList.size()+1));
            }
            packetReader.close();
        } catch (IOException e) {
            System.out.println("Sender could not read file: " + e.toString());
            return;
        }

        // Debug print packets to be sent
        System.out.println("Sending message as packets:");
        for (int i = 0; i < packetList.size(); i++) {
            System.out.println("[" + packetList.get(i).getID() + "]: " + packetList.get(i).getContent() + ", ACK" + packetList.get(i).getAck() + ", digest: " + packetList.get(i).getPacket());
        }

        // Send packets
        int packetIndex = 0;
        int packetsSent = 0;
        while (true) {
            Packet sentPacket = packetList.get(packetIndex);
            byte waitingAck = sentPacket.getAck();
            Packet returnPacket = new Packet(sendMessage(sentPacket.getPacket()));
            packetsSent++;

            // Check ACK
            System.out.print("Waiting ACK" + waitingAck + ", " + packetsSent + ", ");
            if (returnPacket.getAck() == DROP_PACKET) { // If dropped packet, resend
                System.out.println("DROP, resend Packet" + sentPacket.getAck());
            } else if (returnPacket.getAck() == waitingAck) { // If received the ACK that was waited for, send next packet
                packetIndex++;
                // Continually send packets until the last packet is sent
                if (packetIndex >= packetList.size()) {
                    System.out.println("no more packets to send");
                    out.println(EXIT_CMD);
                    break;
                }
                System.out.println("PASS, sending next Packet" + packetList.get(packetIndex).getAck());
            } else {
                System.out.println("CORRUPT, resend Packet" + sentPacket.getAck());
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

    // Splits a message into multiple packets by space, starting at given ID
    private static ArrayList<Packet> messageToPackets(String message, int startIndex) {
        String[] parts = message.trim().replaceAll("\\P{InBasic_Latin}", "").split("\\s+");
        ArrayList<Packet> packets = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            packets.add(new Packet((i + startIndex - 1) % 2, i + startIndex, parts[i].trim()));
        }
        return packets;
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

        // If ACK and content are provided, make packet
        public Packet(int _ack, int _id, String _content) {
            this.ack = (byte)(_ack & 0x000000FF);
            this.id = (byte)(_id & 0x000000FF);
            this.content = _content;
            
            this.checksum = calculateChecksum(content);
            this.packet = makePacket(ack, id, checksum, content);
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

        // Calculates the checksum for the given content
        public static int calculateChecksum(String content) {
            int tempSum = 0;
            for (int i = 0; i < content.length(); i++) {
                tempSum += (int)content.charAt(i);
            }
            return tempSum;
        }

        // Format packet in easy to understand format
        public String toString() {
            if (content.equals("")) {
                return "ACK" + (int)ack;
            } else {
                return "PACKET" + (int)ack + " " + (int)id + " " + checksum + " " + content + ", DIGEST: " + packet;
            }
        }

        public String getPacket() { return packet; }
        public byte getAck() { return ack; }
        public byte getID() { return id; }
        public String getContent() { return content; }
    }
}