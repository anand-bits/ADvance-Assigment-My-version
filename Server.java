import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Map<String, List<String>> data = new HashMap<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT + ". It's using TCP");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    String[] parts = command.split(" ");
                    String response = "";

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
                                clientSocket.close();
                                System.exit(0);

                            default:
                                response = "Invalid command";
                        }
                    }

                    writer.println(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String handlePutCommand(String[] parts) {
            synchronized (lock) {
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
        }

        private String handleGetCommand(String[] parts) {
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

        private String handleDeleteCommand(String[] parts) {
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

        private String handleStoreCommand() {
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
}
