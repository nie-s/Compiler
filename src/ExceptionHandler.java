import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;

public class ExceptionHandler extends Exception {
    BufferedWriter out;
    HashMap<Integer, String> errors = new HashMap<Integer, String>();

    public ExceptionHandler() {
        try {
            out = new BufferedWriter(new FileWriter("error.txt"));
            init();
        } catch (Exception e) {
            ;
        }
    }

    public void init() {
        errors.put((int) 'a', "Illegal char in <FormatString>");
        errors.put((int) 'b', "Duplicate definition");


        errors.put(1, "Expect <Ident> but got");
    }

    public void output(MyException e) {
        try {
            out.write(e.errorLine + " " + e.errorCode + "\n");
            System.out.println(e.errorLine + " " + e.errorCode);
        } catch (Exception error) {
            ;
        }
    }
}
