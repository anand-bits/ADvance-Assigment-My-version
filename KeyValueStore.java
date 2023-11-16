import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyValueStore {
    private static final int TCP_PORT = 12345;
    private static final int UDP_PORT = 54321;
    private static final Logger LOGGER = Logger.getLogger(KeyValueStore.class.getName());
    private static final Scanner scanner = new Scanner(System.in);
    private static final Map<String, List<String>> data = Collections.synchronizedMap(new HashMap<>());
    private static final Object lock = new Object();

    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0];
            if ("server".equalsIgnoreCase(mode)) {
                runTCPServer();
                runUDPServer();
            } else if ("client".equalsIgnoreCase(mode)) {
                runClient();
            } else {
                System.out.println("Invalid mode. Use 'server' or 'client'.");
            }
        } else {
            System.out.println("No mode specified. Use 'server' or 'client'.");
        }
    }

    private static void runTCPServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                System.out.println("TCP Server started on port " + TCP_PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void runUDPServer() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                System.out.println("UDP Server started on port " + UDP_PORT);

                while (true) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    String response = processCommand(clientMessage);

                    byte[] sendData = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(sendPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String processCommand(String command) {
        String[] parts = command.split(" ");
        String response;

        synchronized (lock) {
            switch (parts[0].toLowerCase()) {
                case "put":
                    response = handlePutCommand(parts);
                    break;
                case "get":
                    response = handleGetCommand(parts);
                    break;
                case "del":
                    response = handleDeleteCommand(parts);
                    break;
                case "store":
                    response = handleStoreCommand();
                    break;
                case "exit":
                    response = "Server shutting down.";
                    System.exit(0);
                    break;
                default:
                    response = "Invalid command";
            }
        }
        return response;
    }

    private static String handlePutCommand(String[] parts) {
        if (parts.length >= 3) {
            String key = parts[1];
            String value = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

            List<String> values = data.get(key);
            if (values == null) {
                values = new ArrayList<>();
                data.put(key, values);
            }
            values.add(value);

            return "Put operation successful";
        } else {
            return "Invalid put command";
        }
    }

    private static String handleGetCommand(String[] parts) {
        if (parts.length == 2) {
            String key = parts[1];
            List<String> values = data.getOrDefault(key, Collections.emptyList());
            return values.toString();
        } else {
            return "Invalid get command";
        }
    }

    private static String handleDeleteCommand(String[] parts) {
        if (parts.length == 2) {
            String key = parts[1];
            if (data.containsKey(key)) {
                data.remove(key);
                return "Delete operation successful";
            } else {
                return "Key not found. Delete operation failed.";
            }
        } else {
            return "Invalid delete command";
        }
    }

    private static String handleStoreCommand() {
        StringBuilder storeContent = new StringBuilder();
        data.forEach((key, values) -> storeContent.append(key).append(": ").append(values).append("\n"));

        String storeResult = storeContent.toString();

        if (storeResult.length() > 65_000) {
            storeResult = "TRIMMED: " + storeResult.substring(0, 65_000);
        }

        return storeResult;
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String command;
                while ((command = reader.readLine()) != null) {
                    String response = processCommand(command);
                    writer.println(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runClient() {
        while (true) {
            System.out.print("Enter command (type 'exit' to stop): ");
            String command = scanner.nextLine();
    
            if ("exit".equalsIgnoreCase(command)) {
                break;
            }
    
            CountDownLatch latch = new CountDownLatch(2);
            AtomicReference<String> tcpResponse = new AtomicReference<>();
            AtomicReference<String> udpResponse = new AtomicReference<>();
    
            new Thread(() -> {
                tcpResponse.set(runTCPClient(command));
                latch.countDown();
            }, "TCP-Client-Thread").start();
    
            new Thread(() -> {
                udpResponse.set(runUDPClient(command));
                latch.countDown();
            }, "UDP-Client-Thread").start();
    
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "InterruptedException while waiting for latch", e);
            }
        }
    
        scanner.close();
    }
    

    private static String runTCPClient(String command) {
        String response = "Error: Unable to get server response";
        try (Socket socket = new Socket("localhost", TCP_PORT)) {
            socket.setSoTimeout(5000); // Set a timeout of 5 seconds
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
            long startTime = System.currentTimeMillis(); // Log start time
    
            writer.println(command);
            response = serverReader.readLine();
    
            long endTime = System.currentTimeMillis(); // Log end time
            LOGGER.info("[TCP Client] Server response: " + response);
            LOGGER.info("[TCP Client] Total time: " + (endTime - startTime) + " milliseconds");
    
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException in TCP client", e);
        }
        return response;
    }

    private static String runUDPClient(String command) {
        String response = "Error: Unable to get server response";
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName("localhost");

            byte[] sendData = command.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, UDP_PORT);

            // Capture start time before sending the packet
            long startTime = System.currentTimeMillis();

            socket.send(sendPacket);

            // Receive response from the server
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            // Extract and print the response from the received packet
            response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            LOGGER.info("Server response (UDP): " + response);

            // Capture end time after receiving the response
            long endTime = System.currentTimeMillis();

            // Print total time for data transfer only
            LOGGER.info("Total time (UDP): " + (endTime - startTime) + " milliseconds");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception in UDP client", e);
        }

        return response;
    }
}
