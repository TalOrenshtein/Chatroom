package mmn16q1;


import java.net.Socket;
import java.util.Formatter;
import java.util.Scanner;
import java.io.IOException;

public class Connection extends Thread{
	private Socket connection;
	private Formatter output;
	private Scanner input;
	private String username;
	private ClientsManager cm;
	private String in; // holds the input received from the scanner.


	public Connection(Socket s, ClientsManager clientsManager) {
		connection=s;
		this.cm=clientsManager;

		try {
			input=new Scanner(s.getInputStream());
			output=new Formatter(s.getOutputStream());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Scanner getInput() {
		return input;
	}
	public Formatter getOutput() {
		return output;
	}
	public String getUsername() {
		return username;
	}
	@Override
	public void run() {
		in=input.nextLine(); // getting username from the client
		if(in.equals("No username entered")) {
			closeConnection();
			cm.removeFromConnections(this);
			return;
		}
		cm.unLock.lock();
		cm.setLastConnection(this);
		username=cm.UserNameChecker(in);
		output.format("%s\n",username);
		output.format("%d\n",cm.getConnectionSize());
		if(cm.getConnectionSize()>1)
			output.format(cm.getConnectedUsernames());
		output.flush();
		while(cm.getUn()!=null) // There's already a username that needs to be announced to all the other connections.
			try {
				cm.unCon.await();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		cm.setUn(username);
		cm.unLock.unlock();
		//we can implement msg-seen feature if we want. just check in client side if the gui got focused since the msg sent. and check who focused the gui or something.
		while (true) {
			try {
				if(input.hasNextLine()&&!(in=input.nextLine()).equals("Logout")) {
					cm.msgLock.lock();
					while(cm.getMsg()!=null)// There's already a message that needs to be announce to all the connections.
						try {
							cm.msgCon.await();
						}
						catch (InterruptedException e) {
							e.printStackTrace();
							Thread.currentThread().interrupt();
						}
					cm.setMsg(in);
					cm.msgLock.unlock();
				}
			}catch(java.lang.IllegalStateException e) {
				if(cm.getServerSocket().isClosed())
					return;
				e.printStackTrace();
			}
			if(in.equals("Logout")) {
				closeConnection();
				cm.removeFromConnections(this);
				break;
			}
		}	
	}
	void closeConnection() {
		try {
			output.format("Shutdown\n");
			output.flush();
			output.close();
			input.close();
			if(!connection.isClosed())//The connection will be closed only if the server shutdown manually
				connection.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}


}
