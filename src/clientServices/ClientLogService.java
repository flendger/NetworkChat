package clientServices;

import java.io.*;
import java.util.ArrayList;

public class ClientLogService {
    public static String readLog(int id) {
        ArrayList<String> strList = new ArrayList<>();

        try {
            File logFile = openLog(id);
            if (!logFile.exists()) {
                return "";
            }

            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String curLine = "";

            int lineCounter = 0;
            while ((curLine = reader.readLine()) !=null){
                lineCounter++;
                if (lineCounter > 100) {
                    strList.remove(0);
                }
                strList.add(curLine);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        for (String strLine: strList
             ) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(strLine);
        }

        return sb.toString();
    }

    public static void appendLog(int id, String txt) {
        if (txt.trim().isEmpty()) {
            return;
        }

        try {
            File file = openLog(id);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            if (file.length() > 0) {
                writer.newLine();
            }
            writer.write(txt);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File openLog(int id) {
        return new File(id + ".log");
    }
}
