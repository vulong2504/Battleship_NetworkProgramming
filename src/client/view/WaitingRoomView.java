package client.view;

import client.Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class WaitingRoomView extends JFrame {

    private Client client;
    private DefaultListModel<RoomPlayer> playersListModel = new DefaultListModel<RoomPlayer>();
    private boolean firstTimeListing = true;
    private HashMap<String, String> waitingList;
    private JList<RoomPlayer> playersList;
    private JButton sendInvite;
    private JLabel playersNumber;

    public WaitingRoomView() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 5));
        setTitle("Game tàu chiến");
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        playersList = new JList<>();
        playersList.setModel(playersListModel);
        playersList.addMouseListener(new PlayersListMouseAdapter());
        playersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sendInvite = new JButton("Gửi thách đấu");
        sendInvite.setEnabled(false);
        sendInvite.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RoomPlayer player = playersList.getSelectedValue();
                client.sendJoinGameRequest(player.getKey(), player.getName());
            }
        });


        playersList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                sendInvite.setEnabled(true);
            }
        });

        playersNumber = new JLabel("Người chơi trong sảnh: " + playersListModel.getSize());
        playersNumber.setHorizontalAlignment(JLabel.CENTER);

        mainPanel.add(playersNumber, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(playersList), BorderLayout.CENTER);
        mainPanel.add(sendInvite, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
        setLocationRelativeTo(null);
        pack();

        this.client = new Client(this);
        createNickname();
        client.joinLobby();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private class PlayersListMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) {
                return;
            }

            RoomPlayer player = playersList.getSelectedValue();

            if (player != null) {
                client.sendJoinGameRequest(player.getKey(), player.getName());
            }
        }

    }

    private void createNickname() {
        String message = "Nhập nickname của bạn.";
        while (true) {
            String name = (String) JOptionPane.showInputDialog(this, message,
                    "Nickname", JOptionPane.PLAIN_MESSAGE, null, null, "");
            this.client.sendName(name);
            synchronized (client) {
                try {
                    if (client.getNameState() == Client.NameState.WAITING) {
                        client.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Client.NameState state = client.getNameState();
            if (state == Client.NameState.ACCEPTED) {
                client.setOwnName(name);
                break;
            } else if (state == Client.NameState.INVALID) {
                message = "Nickname không hợp lệ.";
            } else if (state == Client.NameState.TAKEN) {
                message = "Nickname đã tồn tại, thử lại.";
            }
        }
    }

    public boolean playerNameExists(String name) {
        boolean exists = false;
        for (Map.Entry<String, String> entry : waitingList.entrySet()) {
            if (entry.getValue().equals(name)) {
                return true;
            }
        }
        return exists;
    }

    public synchronized void updateWaitingList(HashMap<String, String> waitingList) {
        this.waitingList = waitingList;
        this.playersListModel.clear();
        for (Map.Entry<String, String> entry : waitingList.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(client.getKey())) {
                String name = entry.getValue();
                RoomPlayer player = new RoomPlayer(key, name);
                this.playersListModel.addElement(player);
            }
        }
        if (playersList.isSelectionEmpty()) {
            sendInvite.setEnabled(false);
        }
        playersNumber.setText("Người chơi trong sảnh: " + playersListModel.getSize());
    }

    public static void main(String[] args) {
        new WaitingRoomView();
    }

    private class RoomPlayer {

        private String key;
        private String name;

        public RoomPlayer(String key, String name) {
            this.key = key;
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        public String getKey() {
            return this.key;
        }

        public String getName() {
            return this.name;
        }

    }

    public int showInitialConnectionError() {
        String message = "Không thể kết nối đến server";
        String options[] = {"Quit", "Retry"};
        return JOptionPane.showOptionDialog(this, message,
                "Lỗi kết nối", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE, null, options, options[1]);
    }

    public void showLostConnectionError() {
        JOptionPane.showMessageDialog(this,
                "Mất kết nối server.", "Connection Error",
                JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }
}
