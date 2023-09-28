package mmn16q1;


import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;

public class Server extends JFrame{
	private ClientsManager cm;
	private ServerSocket server;
	private JTextArea ta;
	private JScrollPane sp;
	
	public Server() {
		super("Server");
		ta=new JTextArea();
		sp=new JScrollPane(ta);
		add(sp);
		ta.setEditable(false);
		cm=new ClientsManager(this);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		// closes all connections and close the server when trying to close the server's jframe.
		addWindowListener(new WindowAdapter() { 
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					server.close();
					cm.closeConnections();
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
				finally {
					e.getWindow().dispose();
				}
			}
		});
		setVisible(true);
		setSize(300, 300);
		try {
			server=new ServerSocket(1234);
			cm.start();
			while(!server.isClosed()) 
				handleConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleConnection() {
		// We're using "--" before to make sure that the human who looks at the server log will understand that no user sent that message.
		ta.append("--ServerMessage: Waiting for connection\n");
		try {
			cm.addConnection(server.accept());
		}
		catch (IOException e) {
			if(server.isClosed())
				return;
			e.printStackTrace();
		}
	}

	public ServerSocket getServerSocket() {
		return server;
	}

	public JTextArea getJTextArea() {
		return ta;
	}

}
