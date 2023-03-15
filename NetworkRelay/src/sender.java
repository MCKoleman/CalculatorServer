import java.net.*;
import java.io.*;
import java.util.Scanner;

public class sender {
    private static final String SENDER_HELLO = "Sender Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";

    private Socket senderSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner sc;

    // Handles sending messages to the network until the sender is disconnected
    private void handleSender() {
        while (true) {
            // Continually read user input until -5 is received
            String status = sendMessage(sc.nextLine());
            try {
                System.out.println(status);
                break;
            } catch (NumberFormatException e) {}
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
            sc = new Scanner(System.in);
            out = new PrintWriter(senderSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));

            // Greet server, and start client if greeting matches
            String response = sendMessage(SENDER_HELLO);
            if (response.equals(NETWORK_HELLO)) {
                System.out.println("receive: " + response);
                handleSender();
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
}