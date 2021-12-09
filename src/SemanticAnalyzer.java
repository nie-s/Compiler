import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class SemanticAnalyzer {
    ArrayList<Quadruple> quadruples = new ArrayList<>();
    ArrayList<Quadruple> tmp = new ArrayList<>();
    HashMap<Integer, String> numbers = new HashMap<>();

    boolean output = true;

    public void addQuadruple(String op, String dst, String src1, String src2) {
        if (output) {
            Quadruple quadruple = new Quadruple(op, dst, src1, src2);
            this.quadruples.add(quadruple);
//            System.out.println(quadruple);
        }
    }

    public void addQuadruple_tmp(String op, String dst, String src1, String src2) {
        if (output) {
            Quadruple quadruple = new Quadruple(op, dst, src1, src2);
            this.tmp.add(quadruple);
//            System.out.println(quadruple);
        }
    }

    public void funcDef(String type, String name) {
        addQuadruple("FUNC_" + name + ":", "", "", "");
    }

    public void funcEnd() {
        addQuadruple("F_END:", "", "", "");
    }

    public void mainDef() {
        addQuadruple("FUNC_main:", "", "", "");
    }

    public void assign(String name, int id) {
        addQuadruple_tmp("ASS", name, checkNumber(id), "");
    }

    public void assign_recover() {
        quadruples.addAll(tmp);
        tmp.clear();
    }

    public void conval(String dst, int value) {
        addQuadruple_tmp("ASS_CON", dst, String.valueOf(value), "");
    }

    public void conval_recover() {
        quadruples.addAll(tmp);
        tmp.clear();
    }

    public void add(int dst, int src1, int src2) {
        if (numbers.containsKey(src2) && numbers.containsKey(src1)) {
            numbers.put(dst, String.valueOf(Integer.parseInt(numbers.get(src1)) + Integer.parseInt(numbers.get(src2))));
        } else if (numbers.containsKey(src2)) {
            addQuadruple("ADDI", "tmp@" + dst, checkNumber(src1), String.valueOf(numbers.get(src2)));
        } else {
            addQuadruple("ADD", "tmp@" + dst, checkNumber(src1), "tmp@" + src2);
        }
    }

    public void sll(int dst, int src1, int src2) {
        addQuadruple("SLL", "tmp@" + dst, checkNumber(src1), String.valueOf(src2));
    }

    public void sub(int dst, int src1, int src2) {
        if (numbers.containsKey(src2) && numbers.containsKey(src1)) {
            numbers.put(dst, String.valueOf(Integer.parseInt(numbers.get(src1)) - Integer.parseInt(numbers.get(src2))));
        } else if (numbers.containsKey(src2)) {
            addQuadruple("SUBI", "tmp@" + dst, checkNumber(src1), String.valueOf(numbers.get(src2)));
        } else {
            addQuadruple("SUB", "tmp@" + dst, checkNumber(src1), "tmp@" + src2);

        }
    }

    public void sub(int dst, String src1, int src2) {
        if (numbers.containsKey(src2)) {
            addQuadruple("SUBI", "tmp@" + dst, src1, String.valueOf(numbers.get(src2)));
        } else {
            addQuadruple("SUB", "tmp@" + dst, src1, checkNumber(src2));
        }
    }

    public void mul(int dst, int src1, int src2) {
        if (numbers.containsKey(src2) && numbers.containsKey(src1)) {
            numbers.put(dst, String.valueOf(Integer.parseInt(numbers.get(src1)) * Integer.parseInt(numbers.get(src2))));
        } else {
            addQuadruple("MUL", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
        }
    }

    public void div(int dst, int src1, int src2) {
        if (numbers.containsKey(src2) && numbers.containsKey(src1)) {
            numbers.put(dst, String.valueOf(Integer.parseInt(numbers.get(src1)) / Integer.parseInt(numbers.get(src2))));
        } else {
            addQuadruple("DIV", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
        }
    }

    public void mod(int dst, int src1, int src2) {
        if (numbers.containsKey(src2) && numbers.containsKey(src1)) {
            numbers.put(dst, String.valueOf(Integer.parseInt(numbers.get(src1)) % Integer.parseInt(numbers.get(src2))));
        } else {
            addQuadruple("MOD", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
        }
    }

    public void mul(int dst, int src1, String src2) {
        addQuadruple("MUL", "tmp@" + dst, checkNumber(src1), src2);
    }

    public void not(int dst, int src) {
        addQuadruple("NOT", "tmp@" + dst, checkNumber(src), "");
    }

    public void jr() {
        addQuadruple("JR", "", "", "");
    }

    public void ret(int src) {
        addQuadruple("RET", checkNumber(src), "", "");
    }

    public void printChar(String c) {
        addQuadruple("WC", c, "", "");
    }

    public void printString(String c) {
        addQuadruple("WS", c, "", "");
    }

    public void printInt(int num) {
        addQuadruple("WI", checkNumber(num), "", "");
    }


    public void jump(String label) {
        addQuadruple("J", label, "", "");
    }

    public void output() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("quadruple.txt"));
            for (Quadruple quadruple : quadruples) {
                out.write(quadruple.toString() + "\n");
            }
            out.close();
        } catch (Exception e) {
            //
        }
    }

    public void label(String label) {
        addQuadruple("LABEL", label, "", "");
    }

    public void para(int src, int dim, int rangey) {
        addQuadruple("PARA", checkNumber(src), String.valueOf(dim), String.valueOf(rangey));
    }

    public void para(String src, int shift, int dim, int rangey) {
        addQuadruple("PARA", src + "$" + checkNumber(shift), String.valueOf(dim), String.valueOf(rangey));
    }

    public void para(String src, int dim, int rangey) {
        addQuadruple("PARA", src, String.valueOf(dim), String.valueOf(rangey));
    }

    public void call(String label, int dim) {
        addQuadruple("CALL", label, String.valueOf(dim), "");
    }

    public void funcRet(int dst) {
        addQuadruple("FUNCRET", "tmp@" + dst, "", "");
    }

    public void exit() {
        addQuadruple("EXIT", "", "", "");
    }

    public void beq(int src1, String src2, String target) {
        addQuadruple("BEQ", "tmp@" + src1, src2, target);
    }

    public void beqz(int src1, String target) {
        addQuadruple("BEQZ", "tmp@" + src1, target, "");
    }

    public void eq(int dst, int src1, int src2) {
        addQuadruple("EQ", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
    }

    public void neq(int dst, int src1, int src2) {
        addQuadruple("NEQ", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
    }

    public void neqz(int dst, int src1) {
        addQuadruple("NEQZ", "tmp@" + dst, checkNumber(src1), "");
    }

    public void lss(int dst, int src1, int src2) {
        addQuadruple("LSS", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
    }

    public void leq(int dst, int src1, int src2) {
        addQuadruple("LEQ", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
    }

    public void grt(int dst, int src1, int src2) {
        addQuadruple("GRT", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
    }

    public void geq(int dst, int src1, int src2) {
        addQuadruple("GEQ", "tmp@" + dst, checkNumber(src1), checkNumber(src2));
    }

    public void number(int dst, String src) {
        numbers.put(dst, src);
//        addQuadruple("CONST", "tmp@" + dst, src, "");
    }

    public void lw(int dst, String src, int shift) {
        addQuadruple("LW", "tmp@" + dst, src, "tmp@" + shift);
    }

    public void lval(int dst, String src) {
        addQuadruple("LVAL", "tmp@" + dst, src, "");
    }

    public void sw(String dst, String shift, String src) {
        addQuadruple("SW", dst, shift, src);
    }

    public void sw(String dst, String shift, int src) {
        addQuadruple("SW", dst, shift, checkNumber(src));
    }

    public void define(String name, int layer, int rangex, int rangey) {
        addQuadruple("DEFINE", name + "." + layer, String.valueOf(rangex), String.valueOf(rangey));
    }

    public void defineEnd() {
        addQuadruple("D_END", "", "", "");
    }

    public String checkNumber(int src) {
        if (numbers.containsKey(src)) {
            return numbers.get(src);
        } else {
            return "tmp@" + src;
        }
    }
}

// dst = "@RET";
//    dst.append(to_string(functionCallCnt++));