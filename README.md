# Advance Operating System 
# Java TCP/UDP Client-Server Project

This is a Java project that implements a simple client-server architecture using both TCP and UDP protocols. Clients can interact with the server by sending commands such as "put," "get," "del," "store," and "exit."

## Features

- **TCP and UDP Communication:** Demonstrates the use of both TCP and UDP communication between the client and the server.

- **Command-Based Interaction:** Clients can send commands to the server, including putting data, getting data, deleting data, storing data, and exiting the application.

## Getting Started

Follow these instructions to set up and run the project on your local machine.

### Prerequisites

- Java Development Kit (JDK) installed on your machine.
- A code editor, such as Visual Studio Code or IntelliJ IDEA.

### Running the Server

1. Open a terminal.
2. Navigate to the server's source code directory.
3. Compile the server code using `javac Server.java`.
4. Run the server using `java Server`.

### Running the Client

1. Open a new terminal.
2. Navigate to the client's source code directory.
3. Compile the client code using `javac Client.java`.
4. Run the client using `java Client`.

## Usage

1. Start the server before running any clients.
2. Run one or more clients and enter commands as prompted.
3. Interact with the server using commands like "put," "get," "del," "store," and "exit."

## Example

```shell
$ java Client

Enter command (type 'exit' to stop): put name John
Server response (TCP): Put operation successful
Total time (TCP): 20 milliseconds

Server response (UDP): Put operation successful
Total time (UDP): 25 milliseconds

Enter command (type 'exit' to stop): get name
Server response (TCP): [John]
Total time (TCP): 15 milliseconds

Server response (UDP): [John]
Total time (UDP): 18 milliseconds

...

Enter command (type 'exit' to stop): exit



