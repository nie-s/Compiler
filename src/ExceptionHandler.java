import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ExceptionHandler extends Exception {
    BufferedWriter out;
    HashMap<Integer, String> errors = new HashMap<>();
    ArrayList<String> outputs = new ArrayList<>();

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
        errors.put((int) 'b', "Redeclaration of");
        errors.put((int) 'c', "Undefined Ident");
        errors.put((int) 'd', "Unmatched params count in function ");
        errors.put((int) 'e', "Unmatched params type in function ");
        errors.put((int) 'f', "Unexpected return exp in void function");
        errors.put((int) 'g', "Expected return exp in int function  ");
        errors.put((int) 'h', "Can't change const, error in ");
        errors.put((int) 'i', "Expected ; but got ");
        errors.put((int) 'j', "Expected ) but got ");
        errors.put((int) 'k', "Expected ] but got ");
        errors.put((int) 'm', "Not in while but got ");

        errors.put(1, "Expected <Ident> but got");
        errors.put(2, "Running out of words....");
        errors.put(3, "Expected { but got ");
        errors.put(4, "Expected ] but got ");
        errors.put(5, "Expected UnaryOp but got ");
        errors.put(6, "Expected } but got ");
        errors.put(7, "Expected ( but got ");
        errors.put(8, "Expected { but got ");
        errors.put(9, "Unknown type for primaryExp, got ");
        errors.put(10, "Expected = but got ");
        errors.put(11, "Expected <FormatString> but got ");
        errors.put(12, "Unknown type for funcDef, got ");
        errors.put(13, "Unknown type for mainDef, got ");
        errors.put(13, "Expected <Int> but got");
    }

    public void addError(MyException e) {
        try {
            outputs.add(e.errorLine + " " + (char) e.errorCode + "\n");
            System.out.println("Error Line " + e.errorLine + " (" + e.errorCode + ") " + errors.get(e.errorCode) + ":" + e.errorMessage);
        } catch (Exception error) {
            ;
        }
    }

    public void output() {
        try {
            for (String s : sort(outputs)) {
                out.write(s);
            }
            out.close();
        } catch (Exception e) {
//            /
        }
    }


    private ArrayList<String> sort(ArrayList<String> errors) {
        Comparator<String> cmp = new Comparator<String>() {
            public int compare(String a, String b) {
                int temp1 = Integer.valueOf(a.split(" ")[0]);
                int temp2 = Integer.valueOf(b.split(" ")[0]);
                return temp1 - temp2;
            }
        };
        errors.sort(cmp);
        return errors;
    }
}
