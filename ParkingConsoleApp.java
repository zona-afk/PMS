import java.sql.*;
import java.util.*;

public class ParkingConsoleApp {
    public static void main(String args[]) throws ClassNotFoundException {
        Scanner sc = new Scanner(System.in);

        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Connect to database
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/PDB", "root", "password");
            Statement st = con.createStatement();

            System.out.println("Enter vehicle number and slot number:");
            String vehicleNumber = sc.next();
            String slotid = sc.next();

            // Insert parking slot
            st.executeUpdate(
                "INSERT INTO parking_slots (vehicle_number, slot_id, entry_time, status) " +
                "VALUES ('" + vehicleNumber + "', '" + slotid + "', NOW(), 'occupied')");

            // Show all parking slot
            ResultSet rs = st.executeQuery("SELECT * FROM parking_slots");
            while (rs.next()) {
                System.out.println("Vehicle: " + rs.getString("vehicle_number"));
                System.out.println("Slot: " + rs.getString("slot_id"));
                System.out.println("Entry: " + rs.getTimestamp("entry_time"));
                System.out.println("Status: " + rs.getString("status"));
                System.out.println("-------------");
            }

            con.close();
        } catch (SQLException e) {
            System.out.println("Error is: " + e);
        }
    }
}

