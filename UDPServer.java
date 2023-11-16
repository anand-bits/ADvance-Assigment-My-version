import java.net.*;
import java.util.*;
import java.io.*;

public class UDPServer {
    private static final int PORT = 54321;
    private static Map<String, List<String>> data = new HashMap<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            System.out.println("UDP Server started on port " + PORT);

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                socket.receive(receivePacket);

                String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String response = processCommand(clientMessage);

                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

                socket.send(sendPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processCommand(String command) {
        String[] parts = command.split(" ");
        String response;

        synchronized (lock) {
            switch (parts[0]) {
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
        synchronized (lock) {
            if (parts.length >= 3) {
                String key = parts[1];
                String value = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

                if (data.containsKey(key)) {
                    data.get(key).add(value);
                } else {
                    List<String> values = new ArrayList<>();
                    values.add(value);
                    data.put(key, values);
                }

                return "Put operation successful";
            } else {
                return "Invalid put command";
            }
        }
    }

    private static String handleGetCommand(String[] parts) {
        synchronized (lock) {
            if (parts.length == 2) {
                String key = parts[1];
                List<String> values = data.getOrDefault(key, Collections.emptyList());
                return values.toString();
            } else {
                return "Invalid get command";
            }
        }
    }

    private static String handleDeleteCommand(String[] parts) {
        synchronized (lock) {
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
    }

    private static String handleStoreCommand() {
        synchronized (lock) {
            StringBuilder storeContent = new StringBuilder();
            data.forEach((key, values) -> {
                storeContent.append(key).append(": ").append(values).append("\n");
            });

            String storeResult = storeContent.toString();

            if (storeResult.length() > 65_000) {
                // Truncate the content and prepend 'TRIMMED:'
                storeResult = "TRIMMED: " + storeResult.substring(0, 65_000);
            }

            return storeResult;
        }
    }
}
