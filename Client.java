package mmn16q1;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends JFrame {
	private Socket connection;
	private JTextArea ta;
	private JPanel Panel;
	private JPanel bottomPanel;
	private JTextField tf;
	private Formatter output;
	private Scanner input;
	private String in;
	private String un;
	private JTextArea tfUn;
	private JScrollPane chatSp;
	private JScrollPane unSp;
	private boolean clientLeft=false; // turns true if client decided to close the chat.
	private ArrayList<String> connected;
	private JTextArea unTa;
	private String host;

	
	public Client() {
		super("Chat");
		host=null;
		boolean firstTime=true;
		do {
			if(firstTime) {
				firstTime=false;
				host=JOptionPane.showInputDialog("Enter the server IP and port (IP:port)");
			}else
				host=JOptionPane.showInputDialog("Something's wrong with the IP:Port you entered\nEnter the server IP and port (IP:port)");
			if(host==null)
				return;
			int portLoc=host.indexOf(":");
			if(portLoc==-1) {
				host=null;
				continue;
			}
			try{
				int port=Integer.parseInt(host.substring(portLoc+1, host.length()));
				if(port<0||port>65535) {
					host=null;
					continue;
				}
				InetAddress.getByName(host.substring(0,portLoc));
			}catch (NumberFormatException|UnknownHostException e) {
				host=null;
			}
		} while (host==null);
		Panel=new JPanel();
		ta=new JTextArea();
		unTa=new JTextArea();
		unTa.setEditable(false);
		connected=new ArrayList<String>();
		Panel.setLayout(new BorderLayout());
		add(Panel);
		chatSp=new JScrollPane(ta);
		unSp=new JScrollPane(unTa);
		Panel.add(unSp,BorderLayout.WEST);
		Panel.add(chatSp);
		ta.setEditable(false);
		bottomPanel=new JPanel();
		tf=new JTextField();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(tf);
		tfUn=new JTextArea();
		tfUn.setEditable(false);
		bottomPanel.add(tfUn,BorderLayout.WEST);
		Panel.add(bottomPanel,BorderLayout.SOUTH);
		tf.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!e.getActionCommand().equals(""))
					output.format(un+": "+e.getActionCommand()+"\n");
					output.flush();
					tf.setText("");
			}
		});
		// TODO: find a way to disable the send button when tf="". 
		ta.setEditable(false);
		setSize(300, 300);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					clientLeft=true;
					output.format("Logout\n");
					output.flush();
					connection.close();
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		// setVisible(true); // trying to set frame's visibility  to true after user is connected to the chat.
		try {
			connection=new Socket(InetAddress.getByName(host.substring(0, host.indexOf(":"))), Integer.parseInt((host.substring(host.indexOf(":")+1, host.length()))));
			output=new Formatter(connection.getOutputStream());
			input=new Scanner(connection.getInputStream());
		}
		catch (UnknownHostException |java.net.ConnectException e) { 
			JOptionPane.showMessageDialog(this, "Can't connect to host.");
			//setVisible(false); same as setvisible true above.
			dispose();
			return;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// connecting to the chat
			firstTime=true;
			do {
				if(firstTime) {
					un=JOptionPane.showInputDialog("Enter your name");
					firstTime=false;
				}else
					un=JOptionPane.showInputDialog("Use English letters only with capital letter first to describe your name.\nEnter your name"); // need to check if you can 
				if(un==null) {
					output.format("No username entered");
					output.flush();
					connection.close();
					dispose();
					return;
				}
			} while (!isUsernameLegal(un));
			output.format(un+"\n"); // sends the username to the server.
			output.flush();
			un=input.nextLine(); // add the duplicate number to the username.
			tfUn.setText(un+" :");
			initConnectedArea(Integer.parseInt(input.nextLine()));
			//connected to the chat
			setVisible(true);
			in=null;
			while(input.hasNextLine()) {					
				switch (in=input.nextLine()) {
				case "Shutdown":
					break;
				case "Connected":
					unTa.append(input.nextLine()+"\n");
					String unTaHolder=unTa.getText();
					int firstNumberIndex=unTaHolder.indexOf(":")+1,lastNumberIndex=firstNumberIndex;
					try {
						//updates the number of online useres.
						while(true) {
							Integer.parseInt(unTaHolder.substring(firstNumberIndex,lastNumberIndex+1));
							lastNumberIndex++;
						}
					}catch(NumberFormatException e) {} //Nothing to handle, continue to update after I found the index of the last number at unTa
					if(Integer.parseInt(unTaHolder.substring(firstNumberIndex,lastNumberIndex))==1)
						unTa.setText(unTaHolder.substring(0, firstNumberIndex)+""+(Integer.parseInt(unTaHolder.substring(firstNumberIndex,lastNumberIndex))+1)+"\nUsernames:"+unTaHolder.substring(lastNumberIndex+1, unTa.getText().length()));
					else
						unTa.setText(unTaHolder.substring(0, firstNumberIndex)+""+(Integer.parseInt(unTaHolder.substring(firstNumberIndex,lastNumberIndex))+1)+""+unTaHolder.substring(lastNumberIndex+1, unTa.getText().length()));
					break;
				case "Left":
					ta.append(input.nextLine()+"\n");
					initConnectedArea(Integer.parseInt(input.nextLine()));
					/*for (String st : connected) {
						if(st.equals(in))
							connected.remove(st);
					}
					initUnTa();*/
				break;
				default:
					ta.append(in+"\n");
					break;
				}
			}
			if(!clientLeft) {
				JOptionPane.showMessageDialog(null, "Connection lost");
			}
			dispose();
			output.close();
			input.close();
			return;
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void initConnectedArea(int numOfOnlineClients) {
		unTa.setText("");
		unTa.append("Users online:"+numOfOnlineClients+".\n");
		if(numOfOnlineClients>1)
			unTa.append("Usernames:\n");
		for (int i = 0; i < numOfOnlineClients-1; i++) {
			unTa.append(input.nextLine()+"\n"); // receive and show the 1 user from list of online users.
		}
	}
	//Verify that the username is written with English letters only.
	private boolean isUsernameLegal(String un) {
		if(un.charAt(0)<'A'||un.charAt(0)>'Z')
			return false;
		for (int i = 1; i < un.length(); i++) {
			if(un.charAt(i)<'a'||un.charAt(i)>'z') 
				return false;
		}
		return true;
	}
}
