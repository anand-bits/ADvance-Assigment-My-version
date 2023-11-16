import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final int TCP_PORT = 12345;
    private static final int UDP_PORT = 54321;
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            // Choose command
            System.out.print("Enter command (type 'exit' to stop): ");
            String command = scanner.nextLine();

            if ("exit".equalsIgnoreCase(command)) {
                break; // Exit the loop if "exit" is entered
            }

            // Start both TCP and UDP clients with the same command
            CountDownLatch latch = new CountDownLatch(2);

            // AtomicReference to store responses
            AtomicReference<String> tcpResponse = new AtomicReference<>(null);
            AtomicReference<String> udpResponse = new AtomicReference<>(null);

            new Thread(() -> {
                tcpResponse.set(runTCPClient(command));
                latch.countDown();
            }).start();

            new Thread(() -> {
                udpResponse.set(runUDPClient(command));
                latch.countDown();
            }).start();

            // Wait for both threads to complete before prompting for the next command
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "InterruptedException while waiting for latch", e);
            }

            // Print the total times only
            logResponse("TCP", tcpResponse.get());
            logResponse("UDP", udpResponse.get());
        }

        // Close the scanner outside the loop
        scanner.close();
    }

    private static String runTCPClient(String command) {
        String response = "Error: Unable to get server response";
        try (Socket socket = new Socket("localhost", TCP_PORT);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            long startTime = System.currentTimeMillis();

            writer.println(command);

            response = serverReader.readLine();
            LOGGER.info("Server response (TCP): " + response);

            long endTime = System.currentTimeMillis();

            // Print total time for both data transfer and server response
            LOGGER.info("Total time (TCP): " + (endTime - startTime) + " milliseconds");

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

    private static void logResponse(String protocol, String response) {
        LOGGER.info("Total time (" + protocol + "): " + response + " milliseconds");
    }
}
