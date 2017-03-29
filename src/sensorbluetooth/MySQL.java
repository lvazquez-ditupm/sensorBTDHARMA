package sensorbluetooth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class acts as DAO for the MySQL database
 *
 * @author UPM (member of DHARMA Development Team) (http://dharma.inf.um.es)
 * @version 1.0
 */
public class MySQL {

    /**
     * Establece una conexión con la base de datos
     *
     * @param user usuario
     * @param password contraseña
     */
    private static Connection getConToDB(String user, String password) throws SQLException {
        String dbURL = "jdbc:mysql://localhost:3306/BluetoothSensor"
                + "?verifyServerCertificate=false"
                + "&useSSL=false"
                + "&requireSSL=false";

        return DriverManager.getConnection(dbURL, user, password);
    }

    /**
     * Recupera las direcciones vistas en un momento dado
     *
     * @param user usuario DB
     * @param passwd contraseña DB
     * @param dayWeek día de la semana
     * @param time segundos desde medianoche
     * @return mapa con los activos y sus estados (0-3)
     */
    public static String getAddresses(String user, String passwd, int dayWeek, int time) {
        String res = "";
        try {
            Connection con = getConToDB(user, passwd);
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT addresses FROM Addresses WHERE "
                    + " day_week=4"
                    + " and time=50000"
            //+ " and day_week=" + dayWeek
            //+ " and time=" + time
            );
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                res = rs.getString("addresses");
                break;
            }

            con.close();

        } catch (SQLException sqle) {
            System.out.println("Error en la ejecución:"
                    + sqle.getErrorCode() + " " + sqle.getMessage());
        }
        return res;
    }

    /**
     * Registra en la base de datos las direcciones Bluetooth observadas
     *
     * @param user usuario DB
     * @param passwd contraseña DB
     * @param addrList direcciones BT
     * @param dayWeek día de la semana
     * @param time segundos desde medianoche
     */
    static void addBTAddr(String user, String passwd, ArrayList<String> addrList, int weekDay, int time) {
        try {
            Connection con = getConToDB(user, passwd);

            String query = "INSERT INTO Addresses(addresses, day_week, time) VALUES ";

            String addresses = addrList.toString();
            addresses = addresses.replace(" ", "");
            addresses = addresses.substring(1, addresses.length() - 1);

            query += "(\"" + addresses + "\"," + weekDay + "," + time + ")";

            PreparedStatement stmt = con.prepareStatement(query);
            stmt.executeUpdate();

            con.close();

        } catch (SQLException sqle) {
            System.out.println("Error en la ejecución:"
                    + sqle.getErrorCode() + " " + sqle.getMessage());
        }
    }

}
