
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.*;

public class MyServer {

	static boolean loop = true;

	/*
	 * Set initial acknowledgement number (ack) and the last ask (lastACK) server
	 * will send
	 */
	public static int ack = 0;
	public static int lastACK = 9;

	private static final int BUFFER_SIZE = 1024;

	// Set the Port number where the server socket is going to run on
	private static final int SERVICE_PORT = 9090;

	public static void main(String[] args) throws IOException {
		try {

			/*
			 * Instantiate server socket to receive responses from the client, it takes port
			 * number as an argument. The socket is set to close automatically after timeout
			 * if it is idel for a period of time.
			 */
			DatagramSocket serverSocket = new DatagramSocket(SERVICE_PORT);
			serverSocket.setSoTimeout(100000);

			/*
			 * Create buffer to temporarily stores data sending and receiving data in case
			 * of communication delays.
			 */
			byte[] dataBuffer = new byte[BUFFER_SIZE];

			// Set Infinite loop to check for connections
			while (loop) {

				// Receiving packet from client
				DatagramPacket packetIn = new DatagramPacket(dataBuffer, dataBuffer.length);
				System.out.println("Waiting for a client to connect...");
				serverSocket.receive(packetIn);

				// Read the first line from the packet which has the sequence number
				DataInputStream in = new DataInputStream(
						new ByteArrayInputStream(packetIn.getData(), packetIn.getOffset(), packetIn.getLength()));
				int sequenceNumber = in.readInt();

				// Read the second line from the packet which has the string `umbrella`.
				InputStreamReader reader = new InputStreamReader(in);
				BufferedReader bf = new BufferedReader(reader);
				String receivedData = bf.readLine();

				/*
				 * Examine the Sequence number SN and check if it matches the SN server is
				 * expecting, print the data on the screen. If the result matches, writes the
				 * acknowledgement number together with the message it receives (if there is
				 * any) in the packet that will be sent to the client.
				 */
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(output);
				if (sequenceNumber == 0) {
					System.out.println("Server receives packet, it has Sequence numner " + sequenceNumber);
					ack++;
					dos.writeInt(ack);
				} else if (sequenceNumber == lastACK) {
					System.out.println("Client has received all the acknowledgements, terminates the connaction.");
					loop = false;
					break;
				} else if (sequenceNumber == ack) {
					System.out.println("Server receives packet with message: " + receivedData
							+ ", it has Sequence number " + sequenceNumber);
					ack++;
					dos.writeInt(ack);
					dos.writeUTF(receivedData);
				} else {
					System.out.println("EORROR! Server is expecting Sequence number " + ack + " but it has got "
							+ sequenceNumber + " instead.");
				}

				byte[] udpPacket = output.toByteArray();

				// Obtains client's IP address and the port number
				InetAddress senderAddress = packetIn.getAddress();
				int senderPort = packetIn.getPort();

				// Creates a new UDP packet with data to send to the client
				DatagramPacket outputPacket = new DatagramPacket(udpPacket, udpPacket.length, senderAddress,
						senderPort);

				// Sending the created packet to client
				serverSocket.send(outputPacket);
				System.out.println(
						"Server sending Acknowledgement number " + ack + " for Sequence number " + sequenceNumber);

			}

			// Close the socket connection when server receives the last packet from the
			// client which indicates connection can be terminated.
			serverSocket.close();

		} catch (SocketException e) {
			e.printStackTrace();

		}
	}
}