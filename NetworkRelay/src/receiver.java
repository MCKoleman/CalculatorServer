import java.net.*;
import java.io.*;
import java.util.Scanner;

public class receiver {
    private static final String RECEIVER_HELLO = "Receiver Hello!";
    private static final String NETWORK_HELLO = "Network Hello!";

    private Socket receiverSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner sc;

    // Handles sending messages to the network until the receiver is disconnected
    private void handleReceiver() {
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
            System.out.println("Receiver could not send message " + message + ": " + e.toString());
            return "";
        }
    }

    // Starts a connection with the network of the given IP and port
    private void startConnection(String host, int port) {
        try {
            receiverSocket = new Socket(host, port);
            sc = new Scanner(System.in);
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
}