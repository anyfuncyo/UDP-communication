
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class MyClient {

	// Set the Hostname and Port number where the client socket is going to run on
	public final static int SERVICE_PORT = 9090;
	private static final int BUFFER_SIZE = 1024;
	private static final String HOSTNAME = "localhost";

	/*
	 * The assumption made here is that the total number of packets needed to be
	 * transfer from client to server is 8. Hence, each packet will have a SN
	 * (sequenceNumber) assigned to it, the count starts from 0 and ends at 8. ie SN
	 * = 1, SN = 2, SN = 3 ... SN = 8. Since the last packet sent has SN = 8,
	 * therefore the last acknowledgement number (lastACK) the client expects to
	 * receive is 9. The parameter packet_to_be_ACK is to keep track of the number
	 * of packets that have been sent from the client but yet to be acknowledged by
	 * the server. It starts counting from 1 as it assumes a packet has already been
	 * sent.
	 */
	public static int sequenceNumber = 0;
	public static int totalNumberOfPackets = 8;
	public static int lastACK = 9;
	public static int packet_to_be_ACK = 1;

	/*
	 * These loops here represents when to start and terminate the client sockets.
	 * loop 1 runs by default to perform STOP and WAIT; the timeout loop runs by
	 * default to perform GO-BACK-N. Loop 2 and Loop 3 run inside the timeout loop,
	 * loop 2 runs by default to allow packet to be sent continuously until it
	 * reaches the window limit; whereas loop 3 only runs when the client is ready
	 * to receive acknowledgement from the server.
	 */
	public static boolean loop1 = true;
	public static boolean loop2 = true;
	public static boolean loop3 = false;
	public static boolean timeout = true;

	// Perform STOP and WAIT with initial window size 1
	public static int windowSize = 1;

	public static void main(String[] args) throws IOException {
		try {

			/*
			 * Instantiate client socket. The socket is set to close automatically after
			 * timeout if it is idel for a period of time.
			 */
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(1000);

			// Resolve client hostname to IP address
			InetAddress IPAddress = InetAddress.getByName(HOSTNAME);

			/*
			 * Create buffer to temporarily stores data sending and receiving data in case
			 * of communication delays
			 */
			byte[] dataBuffer = new byte[BUFFER_SIZE];

			/*
			 * Start the STOP AND WAIT process, this simulates a handshake between client
			 * and server to establish connection before sending any file.
			 */
			while (loop1) {

				/*
				 * Create a packet to be sent to the server, the only information it contains is
				 * the sequence number which is 0.
				 */
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(outputStream);
				dos.writeInt(sequenceNumber);
				byte[] udpPacket = outputStream.toByteArray();

				try {

					// Sending packet to the server
					DatagramPacket packetOut = new DatagramPacket(udpPacket, udpPacket.length, IPAddress, SERVICE_PORT);
					clientSocket.send(packetOut);
					System.out.println("Client sending packet with Sequence number: " + sequenceNumber);

					// Getting the server response
					DatagramPacket packetIn = new DatagramPacket(dataBuffer, dataBuffer.length);
					clientSocket.receive(packetIn);

					// Printing the received data and the ACK
					DataInputStream input = new DataInputStream(
							new ByteArrayInputStream(packetIn.getData(), packetIn.getOffset(), packetIn.getLength()));
					int ack = input.readInt();

					/*
					 * Checking the acknowledgement number coming from the server, if it matches
					 * with the number client is expecting to get, which is 1 in this case, then
					 * terminate this loop; if not the client socket will close after timeout and
					 * throw a socketException error, which means connection fails.
					 */
					if (ack == sequenceNumber + 1) {
						System.out.println(
								"Client received Acknowledge number " + ack + " for Sequence number " + sequenceNumber);
						System.out.println("Connection has established");
						sequenceNumber++;
						loop1 = false;
					} else {
						System.out.println("Connection failed");
					}

				} catch (SocketException e) {
					e.printStackTrace();
				}

			}

			/*
			 * Once the connection is established, which is the client has received an
			 * acknowledgement from the server, start the GO-BACK-N process and increase the
			 * sliding window size from 1 to 5.
			 */
			while (timeout) {

				while (loop2) {

					windowSize = 5;

					/*
					 * Within the for loop, client will continue to send packets until either the
					 * number of packet sent has reached the maximum window size, or when all
					 * packets have been sent at least once from the client. The sequence number SN
					 * is used here to track the number of sent packets.
					 */
					for (int i = 1; i <= windowSize; i++) {

						/*
						 * Check if the total number of packets client should send to the server has
						 * been reached, if so stop creating more packets.
						 */
						if (sequenceNumber > totalNumberOfPackets) {
							loop2 = false;
							loop3 = true;
							break;
						}

						/*
						 * Read the line of data from file and add sequence number to the packet. Each
						 * packet created will contain the string `hello world` and a unique sequence
						 * number in its payload.
						 */
						FileReader fr = new FileReader("hello.txt");
						BufferedReader br = new BufferedReader(fr);
						String fileText;
						fileText = br.readLine();
						byte[] fileBytes = fileText.getBytes();
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(outputStream);
						dos.writeInt(sequenceNumber);
						dos.write(fileBytes);
						byte[] udpPacket = outputStream.toByteArray();

						/*
						 * Sending the packet to the server, take the client's IP address and Port
						 * number as arguments for the datagram packet. The port number 9090 is used on
						 * both the client and server side.
						 */
						DatagramPacket packetOut = new DatagramPacket(udpPacket, udpPacket.length, IPAddress,
								SERVICE_PORT);
						clientSocket.send(packetOut);
						System.out.println("Client sending packet with Sequence Number " + sequenceNumber);
						sequenceNumber++;

						/*
						 * Check if number of packet sent has reached the window size limit, if yes then
						 * stop sending packets by stopping loop2, and start collecting acknowledgements
						 * from server by starting loop3.
						 * 
						 */
						if (sequenceNumber > totalNumberOfPackets) {
							System.out.println("Client has sent all the packets.");
						} else if (sequenceNumber > windowSize) {
							loop2 = false;
							loop3 = true;
							break;
						}

					}
				}

				while (loop3) {

					// Getting the server response
					DatagramPacket packetIn = new DatagramPacket(dataBuffer, dataBuffer.length);
					clientSocket.receive(packetIn);

					// Printing the acknowledgement number (ACK) and response message `hello world`
					// from the packet receives.
					DataInputStream input = new DataInputStream(
							new ByteArrayInputStream(packetIn.getData(), packetIn.getOffset(), packetIn.getLength()));
					int ack = input.readInt();
					String receivedData = input.readUTF();
					System.out.println("Client received Acknowledge number " + ack + " for Sequence number "
							+ packet_to_be_ACK + ", it contains the message: " + receivedData);

					/*
					 * Use the ACK from server's packet to determine how many packets have been
					 * successfully sent abd received by the server. It assumes the packets are acknowledged
					 * sequentially.
					 */
					if (ack == lastACK) {

						System.out.println("The server has received all the packets");

						/*
						 *  If all packets have been received and acknowledged (ack == lastACK), 
						 *  send a packet which has sequence number = lastACK to inform
						 *  the server to terminate the connection.
						 */
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(outputStream);
						dos.writeInt(lastACK);
						byte[] udpPacket = outputStream.toByteArray();
						DatagramPacket packetOut = new DatagramPacket(udpPacket, udpPacket.length, IPAddress,
								SERVICE_PORT);
						clientSocket.send(packetOut);
						System.out.println("Client sending packet with Sequence Number " + lastACK
								+ ", informing server to terminate the connection.");
						timeout = false;

					} else if (ack == packet_to_be_ACK + 1) {

						/*
						 * If the ack received from server equals to the value client expected to get,
						 * then allow the client to send more packet by resuming loop2. For instance, when the ack for
						 * packet 1 is received, allow the client to send packet 6.
						 */
						packet_to_be_ACK++;
						loop2 = true;
						
					} else {
						
						/*
						 * If the ack received from server does not equal to the value client expected to get, 
						 * this might be a result of packet drop or acknowledgements are not sent sequentially. 
						 * Update the sequence number with the value of the lost packet (packet_to_be_ACK),
						 * resend the lost packet and all packets that are being sent after it.
						 */
						System.out.println("Missing Acknowledgement for " + packet_to_be_ACK);
						sequenceNumber = packet_to_be_ACK;
						System.out.println("Attempting to resend packet with Sequence number " + packet_to_be_ACK);
						loop2 = true;
					}

					loop3 = false;

				}

			}

			// Closing the socket connection with the server
			clientSocket.close();

		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}