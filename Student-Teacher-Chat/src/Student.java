import base.ReadWrite;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;


public class Student {

	public String PrivateIPofServer = "chatserver.local";
	public int ServerID = 0;
	public ReadWrite readwrite = new ReadWrite(System.getProperty("user.home") + "/Desktop/ChatRecord.txt");
	public String yourName;
	public String record = "";
	public boolean toRecord = true;
	public ArrayList<String> list = new ArrayList<String>();
	public Scanner scannerIn = new Scanner(System.in);
	public boolean staySilentNoOutput = false;
	JTextArea incoming;
	JTextField outgoing;
	BufferedReader reader;
	PrintWriter writer;
	Socket sock;
	JFrame frame;
	public static void main(String[] args) {
		new Student().go();
	}

	public Student(int theServerID) {
		ServerID = theServerID;
		staySilentNoOutput = true;
	}

	public Student() {}

	public void go() {
		if (ServerID == 0) { // we check whether ServerID has already been set using the constructor - Student(int)
			System.out.println("Enter Server ID");
			try {
				ServerID = Integer.parseInt(scannerIn.nextLine());
			} catch (Exception e) {
				System.out.println("Invalid ID");
				System.exit(0);
			}
		}
		setUpNetworking();
		String personName = "";
		try {
			while ((personName = reader.readLine()) != null && !personName.strip().equals("end")) { // we get the lust of names which are already connected to the server.
				list.add(personName.toLowerCase());
			}

		} catch (IOException e) {
			System.out.println("Error in Student.go() while trying to get list of names from server. This is just a minor issue. The program will continue to work");
		}
		if (readwrite.exists()) {
			Date date = new Date();
			record += readwrite.read() + System.lineSeparator() + System.lineSeparator() + date.toString() + System.lineSeparator();
		}

		String contentBuffer = readwrite.getContent();
		String pathBuffer = readwrite.getPath();
		readwrite.setPath(System.getProperty("user.home") + "/Desktop/.ChatPrefs.txt");
		if (readwrite.exists()) {
			System.out.println("Use Prefs? Hit enter. If not, type anything else");
			if (scannerIn.nextLine().equals("")) {
				String[] stringArr = readwrite.read().split(System.lineSeparator());
				toRecord = Boolean.parseBoolean(stringArr[1]);
				yourName = stringArr[0];
				if(list.contains(yourName.toLowerCase())) {
					System.out.println("Sorry, the username in your prefs has been taken");
					defaultSetup();
				}
			} else {
				defaultSetup();
			}
			readwrite.setPath(pathBuffer);
			readwrite.setContent(contentBuffer);

		} else {
			readwrite.setPath(pathBuffer);
			readwrite.setContent(contentBuffer);
			defaultSetup();
		}
		frame = new JFrame("Chat - " + yourName);
		JPanel mainPanel = new JPanel();
		incoming = new JTextArea(20,30);
		DefaultCaret caret = (DefaultCaret) incoming.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		incoming.setLineWrap(true);
		incoming.setWrapStyleWord(true);
		incoming.setEditable(false);
		JScrollPane qScroller = new JScrollPane(incoming);
		outgoing = new JTextField(30);
		outgoing.addKeyListener(new KeyPressListener());
		mainPanel.add(qScroller);
		mainPanel.add(outgoing);
		frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
		frame.setSize(425, 425);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
		frame.setVisible(true);
		outgoing.requestFocusInWindow();
		Thread readerThread = new Thread (new IncomingReader());
		readerThread.start();
		writer.println(yourName);
		writer.println(yourName + " has joined the chat");
		writer.flush();
	}

	private void defaultSetup () {
		System.out.println("Enter your chat username");
		while ((yourName = scannerIn.nextLine()).strip().equals("")) {
			System.out.println("Seriously? Choose another name.");
		}
		while (list.contains(yourName.toLowerCase()) || yourName.strip().equals("")) {
			System.out.println("That name's taken or is invalid. Choose another one");
			yourName = scannerIn.nextLine();
		}
		System.out.println("If you want to save a record of your chat, press enter. Else type the letter n");
		if (scannerIn.nextLine().equalsIgnoreCase("n")) {
			toRecord = false;
		}
		System.out.println("Save prefs? Type yes. If not just hit enter");
		if (scannerIn.nextLine().equalsIgnoreCase("yes")) {
			String contentBuffer = readwrite.getContent();
			String pathBuffer = readwrite.getPath();
			readwrite.setPath(System.getProperty("user.home") + "/Desktop/.ChatPrefs.txt");
			readwrite.setContent(yourName + System.lineSeparator() + toRecord);
			readwrite.create();
			readwrite.setPath(pathBuffer);
			readwrite.setContent(contentBuffer);
		}
	}

	private void setUpNetworking() {
		try {
			sock = new Socket(PrivateIPofServer, ServerID);
			InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
			reader = new BufferedReader(streamReader);
			writer = new PrintWriter(sock.getOutputStream());
			if (!staySilentNoOutput) {
				System.out.println("Connected to Server");
			}

		} catch(IOException ex) {System.out.println("The server isn't online or the ID is incorrect. Bye!"); System.exit(1);}
	}

	public class KeyPressListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {}
		@Override
		public void keyPressed(KeyEvent e) {}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				sendStuff();
			}
		}
	}

	public void sendStuff() {
		String text = outgoing.getText().strip();
		try {
			switch(text) {
				case "clear":
					incoming.setText("");
					break;
				case "bye":
				case "au revoir":
					writer.println(yourName + ":  " + text);
					writer.flush();
					close();
					break;
				case "close":
				case "exit":
					close();
					break;
				case "":
					break;
				default:
					writer.println(yourName + ":  " + text);
					writer.flush();
			}
		} catch(Exception ex) {ex.printStackTrace();}
		outgoing.setText("");
		outgoing.requestFocusInWindow();
	}

	public void close() {
		scannerIn.close();
		frame.setVisible(false);
		writer.println(yourName + " has left the chat");
		writer.flush();
		writer.println("STOP LISTENING");
		writer.flush();
		if (toRecord) {
			readwrite.setPath(System.getProperty("user.home") + "/Desktop/ChatRecord.txt");
			readwrite.setContent(record);
			readwrite.create();
		}
		writer.close();
		try {
			reader.close();
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public class IncomingReader implements Runnable {
		@Override
		public void run () {
			String message;
			try {
				while (!sock.isClosed() && (message = reader.readLine()) != null) {
						System.out.println(message);
					record += message + System.lineSeparator();
					incoming.append(message + System.lineSeparator());
					readwrite.setContent(record);
					readwrite.create();
				}
			} catch (Exception e) {if (!sock.isClosed()){e.printStackTrace();}}
		}
	}
}
