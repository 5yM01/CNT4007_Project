import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client_Utils {
    public static ArrayList<String> read_file(String path) {
        ArrayList<String> stringArr = new ArrayList<String>();

        try {
            Scanner reader = new Scanner(new File(path));
            while (reader.hasNextLine()) {
                stringArr.add(reader.nextLine());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return stringArr;
    }
}