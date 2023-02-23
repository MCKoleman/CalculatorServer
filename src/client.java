import java.net.*;
import java.io.*;
import java.util.Scanner;

public class client {
    private static final String CLIENT_HELLO = "Client Hello!";
    private static final String SERVER_HELLO = "Hello!";

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner sc;

    // Handles sending messages to the server until the client is disconnected
    private void handleClient() {
        while (true) {
            // Continually read user input until -5 is received
            String status = sendMessage(sc.nextLine());
            try {
                // Handle results
                int result = Integer.parseInt(status);
                if (result == -1) {
                    System.out.println("-1: incorrect operation command");
                } else if (result == -2) {
                    System.out.println("-2: number of inputs is less than two");
                } else if (result == -3) {
                    System.out.println("-3: number of inputs is more than four");
                } else if (result == -4) {
                    System.out.println("-4: one or more of the inputs contain(s) non-number(s)");
                } else if (result == -5) {
                    System.out.println("-5: exit");
                    break;
                } else { // Regular result
                    System.out.println(result);
                }
            } catch (NumberFormatException e) {}
        }
        stopConnection();
    }

    // Sends a message to the server that the client is currently connected to
    private String sendMessage(String message) {
        try {
            out.println(message);
            String response = in.readLine();
            return response;
        } catch (IOException e) {
            System.out.println("Client could not send message " + message + ": " + e.toString());
            return "";
        }
    }

    // Starts a connection with the server with the given IP and port
    private void startConnection(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
            sc = new Scanner(System.in);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Greet server, and start client if greeting matches
            String response = sendMessage(CLIENT_HELLO);
            if (response.equals(SERVER_HELLO)) {
                System.out.println(response);
                handleClient();
            }
        } catch (IOException e) {
            System.out.println("Client could not connect to server " + host + ":" + port + ": " + e.toString());
        }
    }

    // Closes the connection with the server
    private void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Client closed with error: " + e.toString());
        }
    }

    // Main: Connects to calculator server
    public static void main(String[] args) {
        // Only start client if port number and ip are explicitly given
        if (args.length < 2) {
            System.out.println("Could not start client: No URL/port given.");
            return;
        }
        
        // Gather info needed to start connection
        client curClient = new client();
        String url = args[0];
        int portNum = 0;

        // Get port number
        try {
            portNum = Integer.parseInt(args[1]);
            if (portNum < 0 || portNum > 65535) {
                throw new NumberFormatException("Invalid number.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Could not start client: Port must be number in range [0-65535]");
            return;
        }

        // Connect to server
        curClient.startConnection(url, portNum);
        curClient.stopConnection();
    }
}