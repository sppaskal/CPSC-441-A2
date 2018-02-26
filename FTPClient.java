
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;



public class FTPClient {

    /**
     * Constructor to initialize the program 
     * 
     * @param serverName	server name
     * @param server_port	server port
     * @param file_name		name of file to transfer
     * @param timeout		Time out value (in milli-seconds).
     */
	
	public String serverName;
	public int serverPort;
	public String fileName;
	public int timeOut;
	
	public FTPClient(String server_name, int server_port, String file_name, int timeout) {
	
	/* Initialize values */
		serverName = server_name;
		serverPort = server_port;
		fileName = file_name;
		timeOut = timeout;

	}
	

    /**
     * Send file content as Segments
     * @throws IOException 
     * 
     */
	public void send() throws IOException {
		
	
		byte response;
		Socket socket = null;
		DatagramSocket socketUDP = null;
		FileInputStream inputStream = null;

		try
		{
			// connects to port server app listesing at port 8888 in the same machine
			socket = new Socket(serverName, 8888);
	
			// Create necessary streams
			DataOutputStream clientOutput= new DataOutputStream(socket.getOutputStream());
			DataInputStream serverResponse = new DataInputStream(socket.getInputStream());
			
			clientOutput.writeUTF(fileName);
			response = serverResponse.readByte();
			
			System.out.println("Server handshake response: " + response);
			System.out.println("-----------------------------------");
			
			File file = new File(fileName);
			inputStream = new FileInputStream(file); //Getting contents of file into inputStream.
			
			//If we get an okay from the server and file exists
			if(response == 0 && file.exists()) {
				
				socketUDP = new DatagramSocket();
				socketUDP.setSoTimeout(timeOut);
				
				InetAddress ip = InetAddress.getByName("localhost"); //This is needed for the DatagramPacket parameter.	
				byte[] dataChunk = new byte[Segment.MAX_PAYLOAD_SIZE]; //This will contain the byte chunk that will be put in the segment
				int sequenceNumber = 0;
				int bytesRead = 0;
				while ((bytesRead = inputStream.read(dataChunk)) != -1) {	
					
					byte[] sendChunk = new byte[bytesRead];
					System.arraycopy(dataChunk, 0, sendChunk, 0, bytesRead);
					//Create the segment, containing the sequence number and the data chunk
					Segment sendSegment = new Segment(sequenceNumber, sendChunk);
					//Put the segment into the DatagramPacket
					DatagramPacket sendPacket = new DatagramPacket(sendSegment.getBytes(), sendSegment.getLength(), ip, 8888);
					//Send the DatagramPacket over to the server
					socketUDP.send(sendPacket);
					System.out.println("Segment has been sent to server.");
					
					//Receive server response into recievePacket
					DatagramPacket recievePacket = new DatagramPacket(new byte[Segment.MAX_SEGMENT_SIZE], Segment.MAX_SEGMENT_SIZE);				
	
					/////////////////////////////////////////
					int responseSeqNum = timeOutCheck(socketUDP, recievePacket, sendPacket, sequenceNumber);
					while( responseSeqNum != sequenceNumber){
						responseSeqNum = timeOutCheck(socketUDP, recievePacket, sendPacket, sequenceNumber);					
					};
					/////////////////////////////////////////
					if(sequenceNumber == 0){
						sequenceNumber = 1;
					}
					
					else {
						sequenceNumber = 0;
					}
					////////////////////////////////////
	
				}	
				
			}
						
			//If we get an error
			else{
				System.out.println("Something went wrong");
			}

			clientOutput.writeByte(0); //Close the TCP connection
			
			
		}	
	
		catch (Exception e)
		{
			System.out.println("Error: " + e.getMessage());
		}
		finally 
		{
			if (socket != null) 
			{
				try 
				{
					socket.close();
					socketUDP.close();
					inputStream.close();
				} 
				catch (IOException ex) 
				{
				// ignore
				}	

			}
		}
		
	}
	
	public int timeOutCheck(DatagramSocket socketUDP, DatagramPacket recievePacket, DatagramPacket sendPacket, int sequenceNumber) throws IOException {
		while(true) {
			try{
				socketUDP.receive(recievePacket);
				//Store server response into a variable of type Segment
				Segment recievedSegment = new Segment(recievePacket.getData());
				System.out.println("Response sequence num: " + recievedSegment.getSeqNum());
				return recievedSegment.getSeqNum();
			}
			
			catch(SocketTimeoutException ste){
				System.out.println("Connection has timed out...");
				socketUDP.send(sendPacket);
				System.out.println("Resending segment " + sequenceNumber);
			}
		}			
	}

/**
     	* A simple test driver
 * @throws IOException 
    	 * 
     	*/
	public static void main(String[] args) throws IOException {
		
		
		String server = "localhost";
		String file_name = "";
		int server_port = 8888;
        int timeout = 50; // milli-seconds (this value should not be changed)

		
		// check for command line arguments
		if (args.length == 3) {
			// either provide 3 parameters
			server = args[0];
			server_port = Integer.parseInt(args[1]);
			file_name = args[2];
		}
		else {
			System.out.println("wrong number of arguments, try again.");
			System.out.println("usage: java FTPClient server port file");
			System.exit(0);
		}

		
		FTPClient ftp = new FTPClient(server, server_port, file_name, timeout);
		
		System.out.printf("sending file \'%s\' to server...\n", file_name);
		ftp.send();
		System.out.println("file transfer completed.");
	}

}