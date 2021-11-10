import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class SemanticAnalyzer {
    ArrayList<Quadruple> quadruples = new ArrayList<>();
    ArrayList<Quadruple> tmp = new ArrayList<>();
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
        addQuadruple_tmp("ASS", name, "tmp@" + id, "");
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
        addQuadruple("ADD", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void sll(int dst, int src1, int src2) {
        addQuadruple("SLL", "tmp@" + dst, "tmp@" + src1, String.valueOf(src2));
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

    public void printChar(String c) {
        addQuadruple("WC", c, "", "");
    }

    public void printString(String c) {
        addQuadruple("WS", c, "", "");
    }

    public void printInt(int num) {
        addQuadruple("WI", "tmp@" + String.valueOf(num), "", "");
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
        addQuadruple("PARA", "tmp@" + src, String.valueOf(dim), String.valueOf(rangey));
    }

    public void para(String src, int dim, int rangey) {
        addQuadruple("PARA", src, String.valueOf(dim), String.valueOf(rangey));
    }

    public void call(String label, int dim) {
        addQuadruple("CALL", label, String.valueOf(dim), "");
    }

    public void funcRet(String dst) {
        addQuadruple("FUNCRET", dst, "", "");
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
        addQuadruple("EQ", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }

    public void neq(int dst, int src1, int src2) {
        addQuadruple("NEQ", "tmp@" + dst, "tmp@" + src1, "tmp@" + src2);
    }
    public void neqz(int dst, int src1) {
        addQuadruple("NEQZ", "tmp@" + dst, "tmp@" + src1,"");
    }

    public void seqz(int dst, int src1) {
        addQuadruple("SEQZ", "tmp@" + dst, "tmp@" + src1, "");
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

    public void lw(int dst, String src, int shift) {
        addQuadruple("LW", "tmp@" + dst, src, "tmp@" + shift);
    }

    public void sw(String dst, String shift, String src) {
        addQuadruple("SW", dst, shift, src);
    }

    public void define(String name, int layer, int rangex, int rangey) {
        addQuadruple("DEFINE", name + "." + layer, String.valueOf(rangex), String.valueOf(rangey));
    }

    public void defineEnd() {
        addQuadruple("D_END", "", "", "");
    }
}

// dst = "@RET";
//    dst.append(to_string(functionCallCnt++));