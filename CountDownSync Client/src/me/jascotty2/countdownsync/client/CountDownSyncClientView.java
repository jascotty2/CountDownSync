/**
 * Copyright (C) 2012 Jacob Scott <jascottytechie@gmail.com>
 * Description: ( TODO )
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.countdownsync.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
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
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * The application's main frame.
 */
public class CountDownSyncClientView extends FrameView {

	static final String VERSION = "1.2";
	static final int MAXNAMELEN = 15;
	SyncClient client;
	Settings conf = new Settings();
	protected ClientList clients = new ClientList();
	CountDownThread countdown = new CountDownThread(this);
	ReadyStateChecker checker;
	String nickname;
	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	int clickedField = 0; // for copy/paste menu
	boolean in_countdown = false;

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
		mnuChkWorkaround.setState(conf.getSettings().getBool("useWorkaround"));

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
		// set findWOT exe
		FindButton.setFindWOT(conf.savefolder + "GetWOT.exe");
		// save settings on close
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				conf.save();
			}
		}));
	}

	@Action
	public void showAboutBox() {
		if (aboutBox == null) {
			JFrame mainFrame = CountDownSyncClientApp.getApplication().getMainFrame();
			aboutBox = new CountDownSyncClientAboutBox(mainFrame);
			aboutBox.setLocationRelativeTo(mainFrame);
			aboutBox.setTitle("About: CountDownSync " + VERSION);
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
		clientPopupMenu.setVisible(false);
		if (nickname == null) {
			return;
		}
		int i = lstClients.getSelectedIndex();
		if (i >= 0) {
			if (clients.getClient(i).nick.equals(nickname)) {
				lblLeaderName.setForeground(
						org.jdesktop.application.Application.getInstance(
						me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().
						getResourceMap(CountDownSyncClientView.class).getColor("lblLeaderName.foreground"));
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

	@Action
	public void selectSelf() {
		clientPopupMenu.setVisible(false);
		if (nickname == null) {
			return;
		}
		lblLeaderName.setForeground(
				org.jdesktop.application.Application.getInstance(
				me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().
				getResourceMap(CountDownSyncClientView.class).getColor("lblLeaderName.foreground"));
		lblLeaderName.setText("(you are leader)");
		btnStart.setVisible(true);
		client.setLeader(null);
		lstClients.clearSelection();
		btnSelect.setEnabled(false);
	}

	@Action
	public void clearSelect() {
		lstClients.clearSelection();
		btnSelect.setEnabled(false);
		clientPopupMenu.setVisible(false);
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

//	@Action
//	public void requestClientUpdate() {
//		clientPopupMenu.setVisible(false);
//		int i = lstClients.getSelectedIndex();
//		if (i >= 0) {
//			client.sendForceUpdate(clients.getClient(i).nick);
//		}
//	}
//	public void noRequestClientUpdate() {
//		JOptionPane.showMessageDialog(this.getFrame(), "can only run that command on a follower client",
//				"Disallowed", JOptionPane.ERROR_MESSAGE);
//	}
	@Action
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
		in_countdown = true;
	}

	public void click() {
		System.out.println("click!");
		FindButton.clickButton();
		btnStart.setEnabled(true);
		lblStat.setText("Connected.");
		in_countdown = false;
		FindButton.scr.save(new File(conf.savefolder + "btnImg.png"));
		FindButton.scr.screenCap();
		FindButton.scr.save(new File(conf.savefolder + "scr.png"));
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

	@Action
	public void paste() {
		try {
			JTextField txt;
			switch (clickedField) {
				case 0: // txtServer
					txt = txtServer;
					break;
				case 1: // txtNick
					txt = txtNick;
					break;
				default:
					return;
			}
			String str = txt.getText(), clip = clipboard.getData(DataFlavor.stringFlavor).toString();
			if (clip.contains("\n")) {
				clip = clip.substring(0, clip.indexOf("\n"));
			}

			if (txt.getSelectionStart() >= 0) {
				str = str.substring(0, txt.getSelectionStart())
						+ clip
						+ str.substring(txt.getSelectionEnd());
			} else {
				str = clipboard.getData(DataFlavor.stringFlavor).toString();
			}

			txt.setText(str);

		} catch (Exception ex) {
			Logger.getLogger(CountDownSyncClientView.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Action
	public void copy() {
		switch (clickedField) {
			case 0: // txtServer
				clipboard.setContents(new StringSelection(txtServer.getSelectedText()), null);
				break;
			case 1: // txtNick
				clipboard.setContents(new StringSelection(txtNick.getSelectedText()), null);
		}
	}
	
	@Action
	public void chkWorkaroundChanged() {
		conf.getSettings().set("useWorkaround", mnuChkWorkaround.getState());
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
        mnuOptions = new javax.swing.JMenu();
        mnuChkWorkaround = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        lblStat = new javax.swing.JLabel();
        lblProg = new javax.swing.JLabel();
        clientPopupMenu = new javax.swing.JPopupMenu();
        mnuSelect2 = new javax.swing.JMenuItem();
        mnuSelectSelf = new javax.swing.JMenuItem();
        mnuCls = new javax.swing.JMenuItem();
        mnuRedraw = new javax.swing.JMenuItem();
        copyPastePopup = new javax.swing.JPopupMenu();
        mnuCopy = new javax.swing.JMenuItem();
        mnuPaste = new javax.swing.JMenuItem();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().getResourceMap(CountDownSyncClientView.class);
        lblServer.setText(resourceMap.getString("lblServer.text")); // NOI18N
        lblServer.setName("lblServer"); // NOI18N

        lblNick.setText(resourceMap.getString("lblNick.text")); // NOI18N
        lblNick.setName("lblNick"); // NOI18N

        txtServer.setText(resourceMap.getString("txtServer.text")); // NOI18N
        txtServer.setName("txtServer"); // NOI18N
        txtServer.setNextFocusableComponent(txtNick);
        txtServer.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtServerMouseClicked(evt);
            }
        });

        txtNick.setText(resourceMap.getString("txtNick.text")); // NOI18N
        txtNick.setName("txtNick"); // NOI18N
        txtNick.setNextFocusableComponent(btnConnect);
        txtNick.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtNickMouseClicked(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(me.jascotty2.countdownsync.client.CountDownSyncClientApp.class).getContext().getActionMap(CountDownSyncClientView.class, this);
        btnConnect.setAction(actionMap.get("connect")); // NOI18N
        btnConnect.setText(resourceMap.getString("btnConnect.text")); // NOI18N
        btnConnect.setName("btnConnect"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        lstClients.setEnabled(false);
        lstClients.setName("lstClients"); // NOI18N
        lstClients.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstClientsMouseClicked(evt);
            }
        });
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
        mnuDisconnect.setName("mnuDisconnect"); // NOI18N
        fileMenu.add(mnuDisconnect);

        mnuRefresh.setAction(actionMap.get("requestUpdate")); // NOI18N
        mnuRefresh.setText(resourceMap.getString("mnuRefresh.text")); // NOI18N
        mnuRefresh.setName("mnuRefresh"); // NOI18N
        fileMenu.add(mnuRefresh);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        mnuOptions.setText(resourceMap.getString("mnuOptions.text")); // NOI18N
        mnuOptions.setToolTipText(resourceMap.getString("mnuOptions.toolTipText")); // NOI18N
        mnuOptions.setName("mnuOptions"); // NOI18N

        mnuChkWorkaround.setAction(actionMap.get("chkWorkaroundChanged")); // NOI18N
        mnuChkWorkaround.setText(resourceMap.getString("mnuChkWorkaround.text")); // NOI18N
        mnuChkWorkaround.setToolTipText(resourceMap.getString("mnuChkWorkaround.toolTipText")); // NOI18N
        mnuChkWorkaround.setName("mnuChkWorkaround"); // NOI18N
        mnuOptions.add(mnuChkWorkaround);

        menuBar.add(mnuOptions);

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

        clientPopupMenu.setName("clientPopupMenu"); // NOI18N

        mnuSelect2.setAction(actionMap.get("selectCurrent")); // NOI18N
        mnuSelect2.setText(resourceMap.getString("mnuSelect2.text")); // NOI18N
        mnuSelect2.setName("mnuSelect2"); // NOI18N
        clientPopupMenu.add(mnuSelect2);

        mnuSelectSelf.setAction(actionMap.get("selectSelf")); // NOI18N
        mnuSelectSelf.setText(resourceMap.getString("mnuSelectSelf.text")); // NOI18N
        mnuSelectSelf.setName("mnuSelectSelf"); // NOI18N
        clientPopupMenu.add(mnuSelectSelf);

        mnuCls.setAction(actionMap.get("clearSelect")); // NOI18N
        mnuCls.setText(resourceMap.getString("mnuCls.text")); // NOI18N
        mnuCls.setName("mnuCls"); // NOI18N
        clientPopupMenu.add(mnuCls);

        mnuRedraw.setAction(actionMap.get("updateList")); // NOI18N
        mnuRedraw.setText(resourceMap.getString("mnuRedraw.text")); // NOI18N
        mnuRedraw.setName("mnuRedraw"); // NOI18N
        clientPopupMenu.add(mnuRedraw);

        copyPastePopup.setName("copyPastePopup"); // NOI18N

        mnuCopy.setAction(actionMap.get("copy")); // NOI18N
        mnuCopy.setText(resourceMap.getString("mnuCopy.text")); // NOI18N
        mnuCopy.setName("mnuCopy"); // NOI18N
        copyPastePopup.add(mnuCopy);

        mnuPaste.setAction(actionMap.get("paste")); // NOI18N
        mnuPaste.setText(resourceMap.getString("mnuPaste.text")); // NOI18N
        mnuPaste.setName("mnuPaste"); // NOI18N
        copyPastePopup.add(mnuPaste);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

	private void lstClientsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstClientsValueChanged
		btnSelect.setEnabled(true);
	}//GEN-LAST:event_lstClientsValueChanged

	private void lstClientsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstClientsMouseClicked
		if (evt.getButton() == MouseEvent.BUTTON3) {
			clientPopupMenu.show(lstClients, evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_lstClientsMouseClicked

	private void txtServerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtServerMouseClicked
		if (evt.getButton() == MouseEvent.BUTTON3) {
			try {
				mnuCopy.setEnabled(!txtServer.getSelectedText().isEmpty());
				mnuPaste.setEnabled(clipboard.getData(DataFlavor.stringFlavor) != null);
			} catch (Exception ex) {
				Logger.getLogger(CountDownSyncClientView.class.getName()).log(Level.SEVERE, null, ex);
				return;
			}
			clickedField = 0;
			copyPastePopup.show(txtServer, evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_txtServerMouseClicked

	private void txtNickMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtNickMouseClicked
		if (evt.getButton() == MouseEvent.BUTTON3) {
			try {
				mnuCopy.setEnabled(txtNick.getSelectedText() != null);
				mnuPaste.setEnabled(clipboard.getData(DataFlavor.stringFlavor) != null);
			} catch (Exception ex) {
				Logger.getLogger(CountDownSyncClientView.class.getName()).log(Level.SEVERE, null, ex);
				return;
			}
			clickedField = 1;
			copyPastePopup.show(txtNick, evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_txtNickMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnSelect;
    private javax.swing.JButton btnStart;
    private javax.swing.JPopupMenu clientPopupMenu;
    private javax.swing.JPopupMenu copyPastePopup;
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
    private javax.swing.JCheckBoxMenuItem mnuChkWorkaround;
    private javax.swing.JMenuItem mnuCls;
    private javax.swing.JMenuItem mnuCopy;
    private javax.swing.JMenuItem mnuDisconnect;
    private javax.swing.JMenu mnuOptions;
    private javax.swing.JMenuItem mnuPaste;
    private javax.swing.JMenuItem mnuRedraw;
    private javax.swing.JMenuItem mnuRefresh;
    private javax.swing.JMenuItem mnuSelect2;
    private javax.swing.JMenuItem mnuSelectSelf;
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
}
