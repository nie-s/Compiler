import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class SemanticAnalyzer {
    ArrayList<Quadruple> quadruples = new ArrayList<>();
    int labelCnt = 0;
    boolean output = true;

    public void addQuadruple(String op, String dst, String src1, String src2) {
        if (output) {
            Quadruple quadruple = new Quadruple(op, dst, src1, src2);
            this.quadruples.add(quadruple);
        }
    }

    public void addQuadruple(String op, String dst, String src1, String src2, boolean global) {
        if (output) {
            Quadruple quadruple = new Quadruple(op, dst, src1, src2, global);
            this.quadruples.add(quadruple);
        }
    }

    public void funcDef(String type, String name) {
        addQuadruple("FUNC_" + name + ":", "", "", "");
    }

    public void mainDef() {
        addQuadruple("FUNC_MAIN:", "", "", "");
    }

    public void assign(String name, int shift, int id) {
        addQuadruple("ASS", name + "$" + shift, "tmp@" + id, "");
    }

    public void conval(String dst, int value) {
        addQuadruple("ASS", dst, String.valueOf(value), "");
    }

    public void add(int dst, int src1, int src2) {
        addQuadruple("ADD", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void add(int dst, int src1, String src2) {
        addQuadruple("ADD", "tmp@" + dst, "tmp@" + src1, src2);
    }

    public void add(int dst, String src1, String src2) {
        addQuadruple("ADD", "tmp@" + dst, src1, src2);
    }

    public void sub(int dst, int src1, int src2) {
        addQuadruple("SUB", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void sub(int dst, String src1, int src2) {
        addQuadruple("SUB", "tmp@" + dst, src1, "tmp@" + src2);
    }

    public void mul(int dst, int src1, int src2) {
        addQuadruple("MUL", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void div(int dst, int src1, int src2) {
        addQuadruple("DIV", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void mod(int dst, int src1, int src2) {
        addQuadruple("MOD", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void mul(int dst, int src1, String src2) {
        addQuadruple("MUL", "tmp@" + dst, "tmp@" + src1, src2);
    }

    public void not(int dst, int src) {
        addQuadruple("NOT", "tmp@" + dst, "tmp@" + src, "");
    }

    public void jr() {
        addQuadruple("JR", "", "", "");
    }

    public void ret(int src) {
        addQuadruple("RET", "tmp@" + src, "", "");
    }

    public void getInt() {
        addQuadruple("RI", "", "", "");
    }

    public void printChar(char c) {
        addQuadruple("WC", String.valueOf(c), "", "");
    }

    public void printChar(String c) {
        addQuadruple("WC", c, "", "");
    }

    public void printInt(int num) {
        addQuadruple("WI", String.valueOf(num), "", "");
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

    public void br(int cond, String label1, String label2) {
        addQuadruple("BR", "tmp@" + cond, label1, label2);
    }

    public void label(String label) {
        addQuadruple(label, "", "", "");
    }

    public void para(int src) {
        addQuadruple("PARA", "tmp@" + src, "", "");
    }

    public void call(String label) {
        addQuadruple("JAL", label, "", "");
    }

    public void exit() {
        addQuadruple("EXIT", "", "", "");
    }

    public void beq(int src1, String src2, String target) {
        addQuadruple("BEQ", "tmp@" + src1, src2, target);
    }

    public void eq(int dst, int src1, int src2) {
        addQuadruple("EQ", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void neq(int dst, int src1, int src2) {
        addQuadruple("NEQ", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void lss(int dst, int src1, int src2) {
        addQuadruple("LSS", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void leq(int dst, int src1, int src2) {
        addQuadruple("LEQ", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void grt(int dst, int src1, int src2) {
        addQuadruple("GRT", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void geq(int dst, int src1, int src2) {
        addQuadruple("GEQ", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void li(int dst, String src) {
        addQuadruple("LI", "tmp@" + dst, src, "");
    }

    public void lw(int dst, String src) {
        addQuadruple("LW", "tmp@" + dst, src, "");
    }

    public void sw(String dst, String src) {
        addQuadruple("SW", dst, src, "");
    }

}

// dst = "@RET";
//    dst.append(to_string(functionCallCnt++));