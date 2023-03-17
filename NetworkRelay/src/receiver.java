import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class receiver {
    private static final String RECEIVER_HELLO = "Receiver Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";
    private static final String EXIT_CMD = "-1";

    private Socket receiverSocket;
    private PrintWriter out;
    private BufferedReader in;

    // Receiver: receives messages from sender through network relay
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

    // Handles sending messages to the network until the receiver is disconnected
    private void handleReceiver() {
        ArrayList<Packet> packetList = new ArrayList<>();

        // Receive packets
        int expectedID = 1;
        int packetsReceived = 0;
        byte waitingAck = 0;
        while (true) {
            try {
                // Exit when receiving the exit string
                String input = in.readLine();
                if (input.equals(EXIT_CMD)) {
                    break;
                }

                // Check if packet is expected and correct
                Packet inputPacket = new Packet(input);
                packetsReceived++;
                Boolean isCorrectAck = inputPacket.getAck() == waitingAck;
                Boolean isCorrectChecksum = inputPacket.verifyChecksum();
                Boolean isCorrectPacket = inputPacket.getID() == expectedID;

                System.out.print("Waiting " + waitingAck + ", " + packetsReceived + ", " + expectedID + ", " + inputPacket.toString());
                
                // Receive correct package and acknowledge it
                if (isCorrectAck && isCorrectChecksum && isCorrectPacket) {
                    System.out.println(", ACK" + (int)waitingAck);
                    out.println(new Packet(waitingAck).getPacket());
                    packetList.add(inputPacket);
                    waitingAck = getOppositeAck(waitingAck);
                    expectedID++;
                } else {
                    System.out.println(", ACK" + (int)getOppositeAck(waitingAck));
                    out.println(new Packet(getOppositeAck(waitingAck)).getPacket());
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

    // Returns the opposite ACK of the given ACK
    private static byte getOppositeAck(byte ack) {
        return (byte)((ack + 1) % 2);
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
                return "PACKET" + (int)ack + " " + (int)id + " " + checksum + " " + content;
            }
        }

        public Boolean verifyChecksum() { return calculateChecksum(content) == getChecksum(); }
        public String getPacket() { return packet; }
        public byte getAck() { return ack; }
        public byte getID() { return id; }
        public int getChecksum() { return checksum; }
        public String getContent() { return content; }
    }
}