import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) {
        ArrayList<String> lines = input();
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        SymbolTableHandler symbolTableHandler = new SymbolTableHandler();
        AbstractSyntaxTree ast = new AbstractSyntaxTree();
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        GrammarAnalyzer grammarAnalyzer = new GrammarAnalyzer(lexicalAnalyzer, exceptionHandler,
                symbolTableHandler, ast, semanticAnalyzer);
        lexicalAnalyzer.analyse(lines);
        grammarAnalyzer.analyse();
//        exceptionHandler.output();
        semanticAnalyzer.output();

        Optimizer optimizer = new Optimizer(semanticAnalyzer.quadruples);
        optimizer.optimize();
        MipsGenerator mipsGenerator = new MipsGenerator(optimizer.quadruples, optimizer);

        mipsGenerator.analyse();
        mipsGenerator.output();


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
