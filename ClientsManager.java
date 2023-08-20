package mmn16q1;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.*;

public class ClientsManager extends Thread {
	private ArrayList<Connection> connections;
	private Server server;
	private String un; // holds a username that will be send to all the (other) connections.
	private String msg; // holds a msg that will be send to all the connections.
	private String loggedout; // holds the loggout username that will be streamed to all the (other) connections.
	private Thread userConnectedHandler,msgSentHandler,userLeftHandler;
	private Connection lastConnection; // holds the un's Connection
	Lock unLock=new ReentrantLock();
	Condition unCon=unLock.newCondition();
	Lock msgLock=new ReentrantLock();
	Condition msgCon=msgLock.newCondition();
	Lock logoutLock=new ReentrantLock();
	Condition logoutCon=logoutLock.newCondition();
	
	public ClientsManager(Server server) {
		this.server=server;
		connections=new ArrayList<Connection>();
		un=null;
		msg=null;
		loggedout=null;
	}
	//uses 2 threads to send the user connected message,to send the message from 1 client to others.
	public void run() {
		userConnectedHandler=new Thread() {
			public void run() {
				while(true) {
					if(server.getServerSocket().isClosed())
						return;
					unLock.lock();
					// announce the connection of un to all other connections
					if(un!=null) { 
						server.getJTextArea().append(un+" connected.\n");
						for (Connection connection : connections) {
							if(!connection.equals(lastConnection)) {
								connection.getOutput().format("Connected\n");
								connection.getOutput().format(un+"\n");
								connection.getOutput().flush();
							}
						}
					}
					un=null;
					unCon.signalAll();
					unLock.unlock();
				}
			}
		};
		userConnectedHandler.start();
		msgSentHandler=new Thread() {
			public void run() {
				while (true) {
					if(server.getServerSocket().isClosed())
						return;
					msgLock.lock();
					// send to message to all connections
					if(msg!=null) {
						server.getJTextArea().append(msg+"\n");
						for (Connection connection : connections) {
								connection.getOutput().format(msg+"\n");
								connection.getOutput().flush();
						}
					}
					msg=null;
					msgCon.signalAll();
					msgLock.unlock();
				}
			}
		};
		msgSentHandler.start();
	}
	//creating a new connection and adding it to the arraylist.
	public void addConnection(Socket s) {
		Connection c=new Connection(s,this);
		connections.add(c);
		c.start();
	}
	// returns all the usernames that doesn't belong to c.
	public String getUsernames() {
		String st="";
		for (Connection connection : connections) {
			if(!connection.equals(lastConnection))
				st+=connection.getUsername()+"\n";
		}
		return st;
	}
	public void setUn(String st) {
		un=st;
	}
	public String getUn() {
		return un;
	}
	public int getConnectionSize() {
		return connections.size();
	}
	public void setMsg(String st) {
		msg=st;
	}
	public String getMsg() {
		return msg;
	}
	ServerSocket getServerSocket() {
		return server.getServerSocket();
	}
	//close all the connections.
	public void closeConnections() {
		for (Connection connection : connections)
			connection.closeConnection();
	}
	 // remove from connections after user logged out and announce all other clients about the logout.
	public void removeFromConnections(Connection c) {
		logoutLock.lock();
		while(loggedout!=null)
			try {
				logoutCon.await();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		loggedout=c.getUsername();
		logoutLock.unlock();
		connections.remove(c);
		userLeftHandler=new Thread() {
			public void run() {
				if(server.getServerSocket().isClosed())
					return;
				logoutLock.lock();
				 // announce the logout of loggedout to all other connections
				if(loggedout!=null) {
					server.getJTextArea().append(loggedout+" left.\n");
					for (Connection connection : connections) {
							connection.getOutput().format("Left\n");
							connection.getOutput().format(loggedout+" left.\n");
							connection.getOutput().format("%d\n",connections.size());
							connection.getOutput().flush();
							for (Connection connection2 : connections)
								if(!connection2.equals(connection)) {
									connection.getOutput().format(connection2.getUsername()+"\n");
									connection.getOutput().flush();
								}
					}
				}
					loggedout=null;
					logoutCon.signalAll();
					logoutLock.unlock();
			}
		};
		userLeftHandler.start();
		
	}
	 // checks for username duplicates and returns a unique username.
	public String UserNameChecker(String username) {
		int unDup=0;
		ArrayList<String> duplicatedNames=new ArrayList<String>();
		for (Connection connection : connections) {
			if(!connection.equals(lastConnection)) {
				String tempUsername;
				if((tempUsername=connection.getUsername()).charAt(0)=='(')
					tempUsername=tempUsername.substring(3); // remove the (x) from (x)SOMEUSERNAME
				if(tempUsername.equals(username)) {
					unDup++;
					duplicatedNames.add(connection.getUsername());
				}
			}
		}
		if(unDup>0) {
			// for the case that theres duplicated nickname  and some of the those useres left. for example: if the users name is tal and  tal and (2)tal are conncted right now,user will get the username (1)tal. 
			ArrayList<Integer> a=new ArrayList<Integer>();
			for (String st : duplicatedNames) {
				if(st.charAt(0)=='(')
					a.add(Integer.parseInt(st.substring(1, 2)));
				else
					a.add(0);
			}
			if(a.indexOf(0)==-1)
				return username;
			for (int i = 1; i <=a.size(); i++)
				if(a.indexOf(i)==-1)
					return "("+i+")"+username;
				
			return "("+unDup+")"+username;	
		}
		else
			return username;
		
	}
	public void setLastConnection(Connection connection) {
		lastConnection=connection;
		
	}
	
}
