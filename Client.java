import java.io.*;
import java.net.*;

public class Client {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", PORT);

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String command;
            do {
                System.out.print("Enter command: ");

                // Capture start time before sending the command
                long startTime = System.currentTimeMillis();

                command = consoleReader.readLine();
                writer.println(command);

                // Capture end time after sending the command
                long endTime = System.currentTimeMillis();

                // Calculate and print the time taken for data transfer
                System.out.println("Time taken for data transfer: " + (endTime - startTime) + " milliseconds");

                // Capture start time before receiving the server response
                startTime = System.currentTimeMillis();

                String response = serverReader.readLine();
                System.out.println("Server response: " + response);

                // Capture end time after receiving the server response
                endTime = System.currentTimeMillis();

                // Calculate and print the time taken for server response
                System.out.println("Time taken for server response: " + (endTime - startTime) + " milliseconds"+"In TCP protocol");

            } while (!command.equals("exit"));

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
