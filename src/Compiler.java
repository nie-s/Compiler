import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws IOException {
        ArrayList<String> lines = input();
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        lexicalAnalyzer.analyse(lines);

    }

    public static ArrayList<String> input() {
        ArrayList<String> lines = new ArrayList<>();
        try {

            File file = new File("testfile.txt");
            InputStreamReader read = new InputStreamReader(new FileInputStream(file));
            BufferedReader bufferedReader = new BufferedReader(read);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            read.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

}
