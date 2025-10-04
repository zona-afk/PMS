import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ParkingGUI extends JFrame implements ActionListener {
    JTextField vehicleField, slotField;
    JButton parkButton, releaseButton, statusButton, clearButton;
    JLabel statusLabel;
      public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/PDB", "root", "password");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Database connection failed: " + e.getMessage());
        }
        return conn;
    }

    
    public ParkingGUI() {
        setTitle("Parking Management System");
        setSize(600, 340);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(245, 250, 255));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 14, 12, 14);

        // Heading
        JLabel heading = new JLabel("🚗 Parking Management Dashboard", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heading.setOpaque(true);
        heading.setForeground(new Color(36, 66, 140));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(heading, gbc);

        // Vehicle Field
        gbc.gridwidth = 1; gbc.gridy++;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Vehicle Number:"), gbc);
        vehicleField = new JTextField(12);
        vehicleField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        gbc.gridx = 1;
        mainPanel.add(vehicleField, gbc);

        // Slot Field
        gbc.gridx = 2;
        mainPanel.add(new JLabel("Slot Number:"), gbc);
        slotField = new JTextField(8);
        slotField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        gbc.gridx = 3;
        mainPanel.add(slotField, gbc);

        // Buttons
        parkButton = new JButton("Park Vehicle");
        releaseButton = new JButton("Release Vehicle");
        statusButton = new JButton("View Status");
        clearButton = new JButton("Clear");

        Color good = new Color(120, 210, 150);
        Color danger = new Color(230, 120, 130);
        Color info = new Color(120, 170, 245);

        parkButton.setBackground(good);
        releaseButton.setBackground(danger);
        statusButton.setBackground(info);
        clearButton.setBackground(new Color(250, 220, 110));

        parkButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        releaseButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        statusButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        gbc.gridy++; gbc.gridx = 0;
        mainPanel.add(parkButton, gbc); gbc.gridx = 1;
        mainPanel.add(releaseButton, gbc); gbc.gridx = 2;
        mainPanel.add(statusButton, gbc); gbc.gridx = 3;
        mainPanel.add(clearButton, gbc);

        // Status label
        statusLabel = new JLabel("Ready.", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(240,245,235));
        statusLabel.setBorder(BorderFactory.createLineBorder(new Color(220,220,220), 1));
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(statusLabel, gbc);

        parkButton.addActionListener(this);
        releaseButton.addActionListener(this);
        statusButton.addActionListener(this);
        clearButton.addActionListener(this);

        setContentPane(mainPanel);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String vehicleNo = vehicleField.getText().trim();
        String slotNo = slotField.getText().trim();

        if (ae.getSource() == parkButton) {
            if(vehicleNo.isEmpty() || slotNo.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter both vehicle and slot number.");
                return;
            }
            parkVehicle(vehicleNo, slotNo);
        }
        if (ae.getSource() == releaseButton) {
            if(vehicleNo.isEmpty() || slotNo.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter both vehicle and slot number.");
                return;
            }
            releaseVehicle(vehicleNo, slotNo);
        }
        if (ae.getSource() == statusButton) {
            viewStatus();
        }
        if (ae.getSource() == clearButton) {
            vehicleField.setText("");
            slotField.setText("");
            statusLabel.setText("Ready.");
        }
    }

    private void parkVehicle(String vehicleNo, String slotNo) {
        try (Connection con = connect()) {
            if(con == null) return;
            PreparedStatement ps = con.prepareStatement("INSERT INTO parking_slots (vehicle_number, slot_id, status, entry_time) VALUES (?, ?, 'OCCUPIED', NOW())");
            ps.setString(1, vehicleNo);
            ps.setString(2, slotNo);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                statusLabel.setText("Parked: " + vehicleNo + " at slot " + slotNo);
                JOptionPane.showMessageDialog(this, "Vehicle parked successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to park vehicle.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this, "Vehicle already parked or slot is in use!");
            } else {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void releaseVehicle(String vehicleNo, String slotNo) {
        try (Connection con = connect()) {
            if(con == null) return;
            PreparedStatement select = con.prepareStatement("SELECT entry_time FROM parking_slots WHERE vehicle_number = ? AND slot_id = ? AND status = 'OCCUPIED'");
            PreparedStatement update = con.prepareStatement("UPDATE parking_slots SET status = 'FREE', exit_time = NOW() WHERE vehicle_number = ? AND slot_id = ?");
            select.setString(1, vehicleNo);
            select.setString(2, slotNo);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                LocalDateTime entry = rs.getTimestamp("entry_time").toLocalDateTime();
                LocalDateTime exit = LocalDateTime.now();
                long hours = Math.max(1, ChronoUnit.HOURS.between(entry, exit));
                long charge = hours * 50; // Rs. 50 per hour
                update.setString(1, vehicleNo);
                update.setString(2, slotNo);
                update.executeUpdate();
                statusLabel.setText("Released: " + vehicleNo + " from slot " + slotNo);
                JOptionPane.showMessageDialog(this, String.format("Vehicle released!\nDuration: %d hours\nCharge: Rs. %d", hours, charge));
            } else {
                JOptionPane.showMessageDialog(this, "Vehicle not found or already released.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void viewStatus() {
        StringBuilder sb = new StringBuilder("<html><h3>Parking Status</h3><table border='1'><tr><th>Slot</th><th>Vehicle</th><th>Status</th></tr>");
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT slot_id, vehicle_number, status FROM parking_slots")) {
            while (rs.next()) {
                sb.append("<tr><td>")
                  .append(rs.getString("slot_id")).append("</td>")
                  .append("<td>").append(rs.getString("vehicle_number")).append("</td>")
                  .append("<td>").append(rs.getString("status")).append("</td></tr>");
            }
            sb.append("</table></html>");
        } catch (SQLException e) {
            sb.append("<tr><td colspan='3'>Error retrieving status.</td></tr></table></html>");
        }
        JLabel label = new JLabel(sb.toString());
        label.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, new JScrollPane(label), "Parking Status", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        new ParkingGUI();
    }
}
