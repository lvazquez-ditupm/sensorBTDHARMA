package sensorbluetooth;

/**
 * This class executes the "Presencia+Activos+Estado" (PAE) sensor
 *
 * @author UPM (member of DHARMA Development Team) (http://dharma.inf.um.es)
 * @version 1.0
 */
import com.google.gson.Gson;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        int lastTime = -1;
        final int T = Integer.parseInt(args[1]);
        FileReader f = null;
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        Calendar nowCal = Calendar.getInstance();
        long secondsFromMidnight = 0;

        if (args.length != 6 || (!args[0].equals("exec") && !args[0].equals("train"))) {
            System.err.println("No se ha introducido un parámentro correcto (exec/train)");
            System.exit(0);
        }

        // Espera hasta que el tiempo actual acabe en 0
        while (true) {
            nowCal.setTime(new Date());
            secondsFromMidnight = (nowCal.getTimeInMillis() - midnight.getTimeInMillis()) / 1000;

            if (secondsFromMidnight % 10 == 0) {
                break;
            }
        }

        System.out.println("-- Start Bluetooth sensor --");

        int weekDay;
        int time;

        while (true) {
            nowCal.setTime(new Date());
            secondsFromMidnight = (nowCal.getTimeInMillis() - midnight.getTimeInMillis()) / 1000;

            weekDay = nowCal.get(Calendar.DAY_OF_WEEK);
            time = (int) (T * (Math.ceil(Math.abs(secondsFromMidnight / T)))); //Redondea a múltiplo de T (segundos)

            try {
                Path path = Paths.get(args[4]);
                if (lastTime != time) {
                    lastTime = time;
                    if (args[0].equals("exec")) {
                        ArrayList<String> anomalies = detectAnomalies(args[2], args[3], path, weekDay, time);
                        processAnomalies(anomalies, secondsFromMidnight, args[5]);
                    } else if (args[0].equals("train")) {
                        addDataToDB(args[2], args[3], path, weekDay, time);
                    } else {
                        throw new Exception("Parámetro incorrecto (exec/train)");
                    }
                }
                Thread.sleep(T*1000);
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(0);
            }
        }

    }

    /**
     * Obtiene los valores de estado de las salas y activos, y los compara con
     * la base de datos para detectar anomalías
     *
     * @param path ruta al fichero input
     * @param weekDay día de la semana (1-domingo, 7-sábado)
     * @param time segundos desde la medianoche
     * @return lista con las anomalías detectadas
     */
    private static ArrayList<String> detectAnomalies(String user, String passwd, Path path, int weekDay, int time) throws Exception {
        ArrayList<String> finalAddrs = new ArrayList<>();
        ArrayList<String> anomalies = new ArrayList<>();

        String content = new String(Files.readAllBytes(path), "UTF-8");

        PrintWriter pw = new PrintWriter(path.toString());
        pw.close();

        if (content.equals("")) {
            return anomalies;
        }

        content = content.replace(" ", "");
        content = content.replace("\"", "");
        content = content.substring(0, content.length() - 1);

        String[] addrs = content.split(",");

        for (String addr : addrs) {
            if (!finalAddrs.contains(addr)) {
                finalAddrs.add(addr);
            }
        }

        String expectedAddrs = MySQL.getAddresses(user, passwd, weekDay, time);
        addrs = expectedAddrs.split(",");
        ArrayList<String> finalAddrsDB = new ArrayList<>(Arrays.asList(addrs));

        for (String addr : finalAddrs) {
            if (!finalAddrsDB.contains(addr)) {
                anomalies.add(addr);
            }
        }

        return anomalies;
    }

    /**
     * Añade los valores de estado de salas y activos a la base de datos
     *
     * @param content direcciones Bluetooth detectadas
     * @param weekDay día de la semana (1-domingo, 7-sábado)
     * @param time segundos desde la medianoche
     */
    private static void addDataToDB(String user, String passwd, Path path, int weekDay, int time) throws Exception {

        String content = new String(Files.readAllBytes(path), "UTF-8");
        PrintWriter pw = new PrintWriter(path.toString());
        pw.close();

        ArrayList<String> finalAddrs = new ArrayList<>();

        if (content.equals("")) {
            return;
        }
        content = content.replace(" ", "");
        content = content.replace("\"", "");
        content = content.substring(0, content.length() - 1);

        String[] addrs = content.split(",");

        for (String addr : addrs) {
            if (!finalAddrs.contains(addr)) {
                finalAddrs.add(addr);
            }
        }

        MySQL.addBTAddr(user, passwd, finalAddrs, weekDay, time);

        System.out.println("Añadidos datos a la base de datos");
    }

    /**
     * Clasifica las anomalías detectadas
     *
     * @param anomalies lista de anomalías
     * @param secondsFromMidnight segundos desde la medianoche
     * @param path ruta al fichero output
     */
    private static void processAnomalies(ArrayList<String> anomalies, long secondsFromMidnight, String path) {

        try {
            PrintWriter writer = new PrintWriter(path, "UTF-8");
            writer.close();
            String output = anomalies.toString();
            writer = new PrintWriter(path, "UTF-8");
            writer.println(output.substring(1, output.length() - 1));
            writer.close();

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
