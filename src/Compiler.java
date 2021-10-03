import java.io.IOException;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws IOException {
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        ArrayList<Symbol> symbols = lexicalAnalyzer.analyse();
    }

}
