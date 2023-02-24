import java.net.*;
import java.io.*;

public class server {
    // Input constants
    private static final String ADD_CMD = "add";
    private static final String SUBTRACT_CMD = "subtract";
    private static final String MULTIPLY_CMD = "multiply";
    private static final String EXIT_CMD = "bye";
    private static final String TERMINATE_CMD = "terminate";
    private static final String CLIENT_HELLO = "Client Hello!";
    private static final String SERVER_HELLO = "Hello!";

    // Calculator params
    private static final int MIN_INPUTS = 2;
    private static final int MAX_INPUTS = 4;

    // Return codes
    private static final int SERVER_OK = 0;
    private static final int INCORRECT_OPERATION = -1;
    private static final int TOO_FEW_INPUTS = -2;
    private static final int TOO_MANY_INPUTS = -3;
    private static final int INPUT_NOT_A_NUMBER = -4;
    private static final int EXIT = -5;
    private static final int SERVER_ERROR = -6;

    // Components
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientIP;

    // Handles the calculation command given, returning the result or an error for invalid input
    private int handleCalculation(String input) {
        String[] params = input.split(" ");
        int result = 0;

        // Parse possible invalid inputs first
        if (!(params[0].equals(ADD_CMD) 
            || params[0].equals(SUBTRACT_CMD) 
            || params[0].equals(MULTIPLY_CMD))) {
            return INCORRECT_OPERATION;
        } else if (params.length < MIN_INPUTS + 1) {
            return TOO_FEW_INPUTS;
        } else if (params.length > MAX_INPUTS + 1) {
            return TOO_MANY_INPUTS;
        }

        // Handle calculation
        try {
            switch(params[0]) {
                // Handle addition
                case ADD_CMD:
                    for (int i = 1; i < params.length; i++) {
                        result += Integer.parseInt(params[i]);
                    }
                    return result;
                // Handle subtraction
                case SUBTRACT_CMD:
                    result = Integer.parseInt(params[1]);
                    for (int i = 2; i < params.length; i++) {
                        result -= Integer.parseInt(params[i]);
                    }
                    return result;
                // Handle multiplication
                case MULTIPLY_CMD:
                    result = 1;
                    for (int i = 1; i < params.length; i++) {
                        result *= Integer.parseInt(params[i]);
                    }
                    return result;
                // Invalid input, should have already been parsed
                default:
                    return INCORRECT_OPERATION;
            }
        } catch (NumberFormatException e) {
            return INPUT_NOT_A_NUMBER;
        }
    }

    // Handles server IO operations and communication with the client
    private int handleClient() {
        String inputLine;
        try {
            // Keep reading requests from client until an exit command is sent
            while ((inputLine = in.readLine()) != null) {
                switch (inputLine) {
                    case EXIT_CMD:
                        System.out.println("Closing connection to client...");

                        // Close client
                        out.println(EXIT);

                        // Keep server up, waiting for new clients
                        return SERVER_OK;
                    case TERMINATE_CMD:
                        System.out.println("Closing connection to client " + clientIP);

                        // Close client
                        out.println(EXIT);
                        clientIP = "";

                        // Close server
                        return EXIT;
                    default:
                        // Handle calculation and request next input
                        int result = handleCalculation(inputLine);
                        out.println(result);
                        System.out.println("get: " + inputLine + ", return: " + result);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Server encountered error: " + e.toString());
            return SERVER_ERROR;
        }
        return SERVER_OK;
    }

    // Handles connecting clients to the server
    private void handleServer() {
        try {
            // Keep accepting new connections until a negative return flag (error or terminate) is sent
            int returnFlag = 0;
            while (returnFlag >= 0) {
                // Connect to a client and start handling server operations until closed by client
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Only start server operations on correct handshake
                String handshake = in.readLine();
                if (!handshake.equals(CLIENT_HELLO)) {
                    System.out.println("Client attempted to connect with invalid handshake: " + handshake);
                    out.println("Handshake not recognized");
                } else {
                    clientIP = ((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
                    System.out.println("get connection from " + clientIP);
                    out.println(SERVER_HELLO);
                    returnFlag = handleClient();
                }
            }
        } catch (IOException e) {
            System.out.println("Server closed with error: " + e.toString());
            return;
        }
    }

    // Starts the server on the given port
    private void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            handleServer();
        } catch (IOException e) {
            System.out.println("Server closed with error: " + e.toString());
            return;
        }
    }

    // Stops the server, closing all connections
    private void stopServer() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Server closed with error: " + e.toString());
            return;
        }
        System.out.println("Server closed successfully");
    }

    // Main: Parses user input and starts the server
    public static void main(String[] args) {
        server curServer = new server();
        int portNum = 0;

        // Only start server if port number is explicitly given
        if (args.length < 1) {
            System.out.println("Could not start server: No port number given.");
            return;
        }
        
        // Get port number
        try {
            portNum = Integer.parseInt(args[0]);
            if(portNum < 0 || portNum > 65535) {
                throw new NumberFormatException("Invalid number.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Could not start server: Port must be number in range [0-65535]");
            return;
        }

        // Start server
        curServer.startServer(portNum);
        curServer.stopServer();
    }
}