import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ParkingManagementSystem extends JFrame implements ActionListener {
    // Database connection
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

    // UI Components
    JTextField vehicleField;
    JButton parkButton, releaseButton, statusButton;
    JLabel statusLabel;

    ParkingManagementSystem() {
        setTitle("Parking Management System");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(5, 2, 10, 10));
        setLocationRelativeTo(null);

        // Initialize components
        add(new JLabel("Vehicle Number:"));
        vehicleField = new JTextField();
        add(vehicleField);

        parkButton = new JButton("Park Vehicle");
        releaseButton = new JButton("Release Vehicle");
        statusButton = new JButton("View Status");
        statusLabel = new JLabel("Ready", SwingConstants.CENTER);

        parkButton.addActionListener(this);
        releaseButton.addActionListener(this);
        statusButton.addActionListener(this);

        // Add components
        add(parkButton);
        add(releaseButton);
        add(statusButton);
        add(new JLabel()); // Placeholder
        add(statusLabel);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String vehicleNo = vehicleField.getText().trim();
        if (vehicleNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a vehicle number.");
            return;
        }

        if (ae.getSource() == parkButton) {
            parkVehicle(vehicleNo);
        } else if (ae.getSource() == releaseButton) {
            releaseVehicle(vehicleNo);
        } else if (ae.getSource() == statusButton) {
            viewStatus();
        }
    }

    private void parkVehicle(String vehicleNo) {
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("INSERT INTO parking_slots (vehicle_number, status, entry_time) VALUES (?, 'OCCUPIED', NOW())")) {

            ps.setString(1, vehicleNo);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                statusLabel.setText("Parked: " + vehicleNo);
                JOptionPane.showMessageDialog(this, "Vehicle parked successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to park vehicle.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this, "Vehicle already parked!");
            } else {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void releaseVehicle(String vehicleNo) {
        try (Connection con = connect();
             PreparedStatement select = con.prepareStatement("SELECT entry_time FROM parking_slots WHERE vehicle_number = ? AND status = 'OCCUPIED'");
             PreparedStatement update = con.prepareStatement("UPDATE parking_slots SET status = 'FREE', exit_time = NOW() WHERE vehicle_number = ?")) {

            select.setString(1, vehicleNo);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                LocalDateTime entry = rs.getTimestamp("entry_time").toLocalDateTime();
                LocalDateTime exit = LocalDateTime.now();
                long hours = ChronoUnit.HOURS.between(entry, exit);
                long charge = Math.max(1, hours) * 50; // Rs. 50 per hour

                update.setString(1, vehicleNo);
                update.executeUpdate();

                statusLabel.setText("Released: " + vehicleNo);
                JOptionPane.showMessageDialog(this, String.format("Vehicle released!\nDuration: %d hours\nCharge: Rs. %d", hours, charge));
            } else {
                JOptionPane.showMessageDialog(this, "Vehicle not found or alreassssdy released.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void viewStatus() {
        StringBuilder sb = new StringBuilder("Parking Status:\n");
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT slot_id, vehicle_number, status FROM parking_slots")) {

            while (rs.next()) {
                sb.append(String.format("Slot %d: %s (%s)\n",
                        rs.getInt("slot_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            sb.append("Error retrieving status.");
        }
        JOptionPane.showMessageDialog(this, sb.toString());
    }

    public static void main(String[] args) {
        new ParkingManagementSystem();
    }
}
