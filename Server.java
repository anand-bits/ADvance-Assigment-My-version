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
            System.out.println("Server started on port " + PORT + "Its using TCP");

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
                                if (parts.length >= 3) {
                                    String key = parts[1];
                                    String value = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

                                    // Check if the key is already in the map
                                    if (data.containsKey(key)) {
                                        // If the key exists, append the new value to the list
                                        data.get(key).add(value);
                                    } else {
                                        // If the key doesn't exist, create a new list with the value
                                        List<String> values = new ArrayList<>();
                                        values.add(value);
                                        data.put(key, values);
                                    }
                                    response = "Put operation successful";
                                } else {
                                    response = "Invalid put command";
                                }
                                break;

                            case "get":
                                if (parts.length == 2) {
                                    String key = parts[1];
                                    List<String> values = data.getOrDefault(key, Collections.emptyList());
                                    response = values.toString();
                                } else {
                                    response = "Invalid get command";
                                }
                                break;

                            case "del":
                                if (parts.length == 2) {
                                    String key = parts[1];
                                    data.remove(key);
                                    response = "Delete operation successful";
                                } else {
                                    response = "Invalid delete command";
                                }
                                break;

                            case "store":
                                response = data.toString();
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
    }
}
