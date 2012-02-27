/*
 * CountDownSyncClientView.java
 */
package me.jascotty2.countdownsync.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * The application's main frame.
 */
public class CountDownSyncClientView extends FrameView {

	static final String VERSION = "1.0.2";
	static final int MAXNAMELEN = 15;
	SyncClient client;
	Settings conf = new Settings();
	protected ClientList clients = new ClientList();
	CountDownThread countdown = new CountDownThread(this);
	ReadyStateChecker checker;
	String nickname;

	public CountDownSyncClientView(SingleFrameApplication app) {
		super(app);
		initComponents();

		super.getFrame().setMinimumSize(new Dimension(350, 270));
		//super.getFrame().setResizable(false);

		txtNick.setDocument(new PlainDocument() {

			@Override
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				if (str != null && (getLength() + str.length() < MAXNAMELEN)) {
					super.insertString(offs, str, a);
				}
			}
		});

		try {
			Image icon = ImageIO.read(FindButton.class.getResource("resources/icon.png"));
			super.getFrame().setIconImage(icon);
		} catch (IOException ex) {
			Logger.getLogger(CountDownSyncClientView.class.getName()).log(Level.SEVERE, null, ex);
		}
		txtServer.setText(conf.getSettings().getString("lastServer"));
		txtNick.setText(conf.getSettings().getString("nickname"));

//		btnRefresh = new javax.swing.JButton();
//		btnRefresh.setSize(15, 15);
//		btnRefresh.setLocation(50, 50);
//		super.getFrame().add(btnRefresh);

		// status bar initialization - message timeout, idle icon and busy animation, etc
		ResourceMap resourceMap = getResourceMap();
		int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
		messageTimer = new Timer(messageTimeout, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				lblStat.setText("");
			}
		});
		messageTimer.setRepeats(false);
		int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
		for (int i = 0; i < busyIcons.length; i++) {
			busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
		}
		busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
				//lblStat.setIcon(busyIcons[busyIconIndex]);
			}
		});
		idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
		//lblStat.setIcon(idleIcon);
		progressBar.setVisible(false);

		// connecting action tasks to status bar via TaskMonitor
		TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
		taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

			public void propertyChange(java.beans.PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if ("started".equals(propertyName)) {
					if (!busyIconTimer.isRunning()) {
						//lblStat.setIcon(busyIcons[0]);
						busyIconIndex = 0;
						busyIconTimer.start();
					}
					progressBar.setVisible(true);
					progressBar.setIndeterminate(true);
				} else if ("done".equals(propertyName)) {
					busyIconTimer.stop();
					//lblStat.setIcon(idleIcon);
					progressBar.setVisible(false);
					progressBar.setValue(0);
				} else if ("message".equals(propertyName)) {
					String text = (String) (evt.getNewValue());
					lblStat.setText((text == null) ? "" : text);
					messageTimer.restart();
				} else if ("progress".equals(propertyName)) {
					int value = (Integer) (evt.getNewValue());
					progressBar.setVisible(true);
					progressBar.setIndeterminate(false);
					progressBar.setValue(value);
				}
			}
		});

		// write version #
		File file = new File(conf.savefolder + "VERSION");
		try {
			FileOutputStream fos = new FileOutputStream(file, false);
			fos.write(VERSION.getBytes());
			fos.close();
		} catch (Exception e) {
		}
	}

	@Action
	public void showAboutBox() {
		if (aboutBox == null) {
			JFrame mainFrame = CountDownSyncClientApp.getApplication().getMainFrame();
			aboutBox = new CountDownSyncClientAboutBox(mainFrame);
			aboutBox.setLocationRelativeTo(mainFrame);
		}
		CountDownSyncClientApp.getApplication().show(aboutBox);
	}

	@Action
	public void connect() {
		if (txtServer.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this.getFrame(), "You must input a server to connect to",
					"Connect Error", JOptionPane.ERROR_MESSAGE);
			return;
		} else if (txtServer.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this.getFrame(), "Input a username",
					"Connect Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (client != null) {
			// inputting an alternate
			client.setNickname(txtNick.getText());
			btnConnect.setEnabled(false);
			txtNick.setEnabled(false);
			return;
		}
		int port = 3210;
		try {
			String serverStr = txtServer.getText();
			if (serverStr.contains(":")) {
				port = Integer.parseInt(serverStr.substring(serverStr.indexOf(":") + 1));
				serverStr = serverStr.substring(0, serverStr.indexOf(":"));
			}
			setStatus("Connecting to server at " + serverStr + ":" + port);

			ChatClient cl = new ChatClient(serverStr, port);
			client = new SyncClient(cl, this);
			btnConnect.setEnabled(false);
			txtServer.setEnabled(false);
			txtNick.setEnabled(false);
			lblStat.setText("Connected.");
			mnuDisconnect.setEnabled(true);
			mnuRefresh.setEnabled(true);
			
			client.setNickname(txtNick.getText());

		} catch (Exception e) {
			lblStat.setText(e.toString());
			JOptionPane.showMessageDialog(this.getFrame(), e.toString(),
					"Connect Error", JOptionPane.ERROR_MESSAGE);

		}
	}

	@Action
	public void selectCurrent() {
		if (nickname == null) {
			return;
		}
		int i = lstClients.getSelectedIndex();
		if (i >= 0) {
			if (clients.getClient(i).nick.equals(nickname)) {
				lblLeaderName.setForeground(
						org.jdesktop.application.Application.getInstance(
						me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().getResourceMap(CountDownSyncClientView.class).getColor("lblLeaderName.foreground"));
				lblLeaderName.setText("(you are leader)");
				btnStart.setVisible(true);
				client.setLeader(null);
			} else {
				if (!clients.getClient(i).isLeader) {
					JOptionPane.showMessageDialog(this.getFrame(),
							"<html>Cannot use a follower as a leader!<br>"
							+ "<span style='color:blue'>(Leaders are shown in Blue)</span></html>",
							"Leader Error", JOptionPane.INFORMATION_MESSAGE);
				} else {
					String n = clients.getClient(i).nick;
					lblLeaderName.setForeground(Color.black);
					lblLeaderName.setText(n);
					btnStart.setVisible(false);
					client.setLeader(n);
				}
			}
			lstClients.clearSelection();
			btnSelect.setEnabled(false);
		}
	}

	public void setStatus(String msg) {
		lblStat.setText(msg);
		lblStat.update(lblStat.getGraphics());
	}

	public void nick(boolean ok) {
		if (ok) {
			nickname = txtNick.getText();
			btnStart.setEnabled(true);
			lstClients.setEnabled(true);
			conf.getSettings().set("lastServer", txtServer.getText());
			conf.getSettings().set("nickname", nickname);
			conf.save();
			checker = new ReadyStateChecker(client);
			checker.start();
		} else {
			client.requestUpdate();
			JOptionPane.showMessageDialog(this.getFrame(), "Nickname is in use - please choose another",
					"Connect Error", JOptionPane.ERROR_MESSAGE);
			txtNick.setEnabled(true);
			btnConnect.setEnabled(true);
		}
	}

	@Action
	public void requestUpdate() {
		client.requestUpdate();
	}

	public void updateList() {
		lstClients.removeAll();
		lstClients.setListData(clients.getList());
		//lstClients.revalidate();
		lstClients.repaint();
	}

	@Action
	public void sendStart() {
		client.sendStart();
		btnStart.setEnabled(false);
	}

	public void startCount(int sec) {
		countdown.start(sec);
	}

	public void click() {
		System.out.println("click!");
		FindButton.clickButton();
		btnStart.setEnabled(true);
		lblStat.setText("Connected.");
		FindButton.scr.save(new File(conf.savefolder + "btnImg.png"));
	}

	public void reconnected() {
		setStatus("Connected.");

		mnuDisconnect.setEnabled(true);
		mnuRefresh.setEnabled(true);
	}

	@Action
	public void disconnect() {
		disconnect(false);
	}

	public void disconnect(boolean reconnecting) {
		if (checker != null) {
			checker.stop();
		}
		mnuDisconnect.setEnabled(false);
		mnuRefresh.setEnabled(false);
		if (reconnecting) {
			setStatus("disconnected.. retrying connection");
			btnStart.setEnabled(false);
			lstClients.setEnabled(false);
			btnSelect.setEnabled(false);
		} else {
			btnConnect.setEnabled(true);
			txtServer.setEnabled(true);
			txtNick.setEnabled(true);
			nickname = null;

			btnStart.setEnabled(false);
			btnSelect.setEnabled(false);

			clients.clear();
			lstClients.setListData(clients.getList());

			setStatus("disconnected from server");

			if (client != null) {
				client.close();
				client = null;
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        lblServer = new javax.swing.JLabel();
        lblNick = new javax.swing.JLabel();
        txtServer = new javax.swing.JTextField();
        txtNick = new javax.swing.JTextField();
        btnConnect = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstClients = new javax.swing.JList();
        lblLeader = new javax.swing.JLabel();
        lblLeaderName = new javax.swing.JLabel();
        btnSelect = new javax.swing.JButton();
        btnStart = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        mnuDisconnect = new javax.swing.JMenuItem();
        mnuRefresh = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        lblStat = new javax.swing.JLabel();
        lblProg = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().getResourceMap(CountDownSyncClientView.class);
        lblServer.setText(resourceMap.getString("lblServer.text")); // NOI18N
        lblServer.setName("lblServer"); // NOI18N

        lblNick.setText(resourceMap.getString("lblNick.text")); // NOI18N
        lblNick.setName("lblNick"); // NOI18N

        txtServer.setText(resourceMap.getString("txtServer.text")); // NOI18N
        txtServer.setName("txtServer"); // NOI18N
        txtServer.setNextFocusableComponent(txtNick);

        txtNick.setText(resourceMap.getString("txtNick.text")); // NOI18N
        txtNick.setName("txtNick"); // NOI18N
        txtNick.setNextFocusableComponent(btnConnect);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().getActionMap(CountDownSyncClientView.class, this);
        btnConnect.setAction(actionMap.get("connect")); // NOI18N
        btnConnect.setText(resourceMap.getString("btnConnect.text")); // NOI18N
        btnConnect.setName("btnConnect"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        lstClients.setEnabled(false);
        lstClients.setName("lstClients"); // NOI18N
        lstClients.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstClientsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(lstClients);

        lblLeader.setText(resourceMap.getString("lblLeader.text")); // NOI18N
        lblLeader.setName("lblLeader"); // NOI18N

        lblLeaderName.setForeground(resourceMap.getColor("lblLeaderName.foreground")); // NOI18N
        lblLeaderName.setText(resourceMap.getString("lblLeaderName.text")); // NOI18N
        lblLeaderName.setName("lblLeaderName"); // NOI18N

        btnSelect.setAction(actionMap.get("selectCurrent")); // NOI18N
        btnSelect.setText(resourceMap.getString("btnSelect.text")); // NOI18N
        btnSelect.setName("btnSelect"); // NOI18N

        btnStart.setAction(actionMap.get("sendStart")); // NOI18N
        btnStart.setFont(resourceMap.getFont("btnStart.font")); // NOI18N
        btnStart.setText(resourceMap.getString("btnStart.text")); // NOI18N
        btnStart.setEnabled(false);
        btnStart.setName("btnStart"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addComponent(lblServer)
                                .addGap(18, 18, 18)
                                .addComponent(txtServer, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addComponent(lblNick)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtNick, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(lblLeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnConnect, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnSelect, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                            .addComponent(btnStart, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                            .addComponent(lblLeaderName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(btnConnect, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblServer)
                            .addComponent(txtServer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblNick)
                            .addComponent(txtNick, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(lblLeader)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblLeaderName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        mnuDisconnect.setAction(actionMap.get("disconnect")); // NOI18N
        mnuDisconnect.setText(resourceMap.getString("mnuDisconnect.text")); // NOI18N
        mnuDisconnect.setEnabled(false);
        mnuDisconnect.setName("mnuDisconnect"); // NOI18N
        fileMenu.add(mnuDisconnect);

        mnuRefresh.setAction(actionMap.get("requestUpdate")); // NOI18N
        mnuRefresh.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        mnuRefresh.setText(resourceMap.getString("mnuRefresh.text")); // NOI18N
        mnuRefresh.setEnabled(false);
        mnuRefresh.setName("mnuRefresh"); // NOI18N
        fileMenu.add(mnuRefresh);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setPreferredSize(new java.awt.Dimension(328, 22));

        lblStat.setName("lblStat"); // NOI18N

        lblProg.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblProg.setText(resourceMap.getString("lblProg.text")); // NOI18N
        lblProg.setName("lblProg"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblStat, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addComponent(lblProg)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStat, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblProg))
                .addContainerGap())
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

	private void lstClientsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstClientsValueChanged
		btnSelect.setEnabled(true);
	}//GEN-LAST:event_lstClientsValueChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnSelect;
    private javax.swing.JButton btnStart;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblLeader;
    private javax.swing.JLabel lblLeaderName;
    private javax.swing.JLabel lblNick;
    private javax.swing.JLabel lblProg;
    private javax.swing.JLabel lblServer;
    private javax.swing.JLabel lblStat;
    private javax.swing.JList lstClients;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem mnuDisconnect;
    private javax.swing.JMenuItem mnuRefresh;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextField txtNick;
    private javax.swing.JTextField txtServer;
    // End of variables declaration//GEN-END:variables
	private final Timer messageTimer;
	private final Timer busyIconTimer;
	private final Icon idleIcon;
	private final Icon[] busyIcons = new Icon[15];
	private int busyIconIndex = 0;
	private JDialog aboutBox;
	private javax.swing.JButton btnRefresh;
}