import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Pattern;

public class MipsGenerator {
    ArrayList<Quadruple> quadruples;
    ArrayList<Mips> mips = new ArrayList<>();
    ArrayList<Mips> data = new ArrayList<>();

    ArrayList<String> global = new ArrayList<>();
    HashMap<String, Integer> globalDim = new HashMap<>();

    HashMap<String, Integer> varTable = new HashMap<>();
    HashMap<String, Integer> varType = new HashMap<>();
    HashMap<String, Integer> varDim = new HashMap<>();
    HashMap<String, Integer> varReal = new HashMap<>();

    Stack<Quadruple> funcStack = new Stack<>();

    int index = 0;
    int strCnt = 0;
    int currentSP = 0;
    boolean debug = true;

    public static class Mips {
        public String op;
        public String dst = "";
        public String src1 = "";
        public String src2 = "";

        public Mips(String op, String dst, String src1, String src2) {
            this.op = op;
            this.dst = dst;
            this.src1 = src1;
            this.src2 = src2;
        }

        public Mips(String op) {
            this.op = op;
        }

        public String toString() {
            return String.format("%-10s", op) +
                    String.format("%-12s", dst) +
                    String.format("%-12s", src1) +
                    src2;
        }
    }

    public MipsGenerator(ArrayList<Quadruple> quadruples) {
        this.quadruples = quadruples;
    }

    public void analyse() {
        globalDefine();
        print_mips(new Mips(".text"));
        print_mips(new Mips("li", "$sp", "0x10040004", ""));
        print_mips(new Mips("J", "main", "", ""));
        while (index < quadruples.size()) {
            print_mips(new Mips(""));
            funcDefine();
        }
    }

    public void globalDefine() {
        if (quadruples.get(index).isLabel() && quadruples.get(index).dst.equals("GLOBAL:")) {
            index++;
            print_data(new Mips(".data", "", "", ""));
            while (!quadruples.get(index).dst.equals("GLOBAL_END:")) {
                getGlobalDefine();
                index++;
            }
            index++;
        }
    }

    public void getGlobalDefine() {
        if (debug) print_data(new Mips("#define", quadruples.get(index).dst, "", ""));
        Quadruple quadruple = quadruples.get(index);
        String name = quadruple.dst;
        String src1 = quadruple.src1;
        String src2 = quadruple.src2;

        global.add(name);
        globalDim.put(name, Integer.parseInt(src1));

        if (quadruples.get(index + 1).isDefineEnd()) {
            int len = getLen(src1, src2);
            print_data(new Mips("", name + ":", ".word", "0:" + len));
            index++;
        } else {
            index++;
            StringBuilder sb = new StringBuilder();
            while (!quadruples.get(index).isDefineEnd()) {
                sb.append(",").append(quadruples.get(index).src1);
                index++;
            }
            print_data(new Mips("", name + ":", ".word", sb.substring(1)));
        }

    }

    public int getLen(String src1, String src2) {
        int len;
        int rangex = Integer.parseInt(src1);
        int rangey = Integer.parseInt(src2);
        if (rangex == 0 && rangey == 0) {
            len = 1;
        } else if (rangey == 0) {
            len = rangex;
        } else {
            len = rangex * rangey;
        }
        return len;
    }

    public void funcDefine() {
        String funcName = quadruples.get(index++).op.substring(5);
        print_mips(new Mips(funcName));
        ArrayList<Quadruple> list = new ArrayList<>();
        while (quadruples.get(index).isRecover()) {
            list.add(quadruples.get(index++));
        }

        int cnt = list.size();
        for (int i = 0; i < cnt; i++) {
            Quadruple quadruple = list.get(i);
            recover(4 * (2 + i), quadruple.dst,
                    quadruple.src1, quadruple.src2);
        }
        //TODO
        while (!quadruples.get(index).op.equals("F_END:")) {
            String op = quadruples.get(index).op;
            String dst = quadruples.get(index).dst;
            String src1 = quadruples.get(index).src1;
            String src2 = quadruples.get(index).src2;

            switch (op) {
                case "WS":
                    printString();
                    break;
                case "WC":
                    printChar();
                    break;
                case "WI":
                    printInt(dst);
                    break;
                case "DEFINE":
                    localDefine();
                    break;
                case "LI":
                    li();
                    break;
                case "LW":
                    lw();
                    break;
                case "ADD":
                case "SUB":
                case "MUL":
                case "DIV":
                case "MOD":
                case "EQ":
                case "LSS":
                case "LEQ":
                case "GRT":
                case "GEQ":
                case "NEQ":
                    cal_two(op, dst, src1, src2);
                    break;
                case "NEQZ":
                case "NOT":
                case "SLL":
                    cal_one(op, dst, src1, src2);
                    break;
                case "LABEL":
                    print_mips(new Mips(""));
                    print_mips(new Mips(quadruples.get(index).dst));
                    break;
                case "J":
                    print_mips(new Mips("j", quadruples.get(index).dst, "", ""));
                    break;
                case "CALL":
                    callFunc(Integer.parseInt(src1));
                    break;
                case "JR":
                    print_mips(new Mips("jr", "$ra", "", ""));
                    break;
                case "EXIT":
                    print_mips(new Mips("li", "$v0", "10", ""));
                    print_mips(new Mips("syscall"));
                    break;
                case "SW":
                    sw(dst, src1, src2);
                    break;
                case "BEQZ":
                    load("$t0", dst);
                    print_mips(new Mips("beq", "$t0", "$0", src1));
                    break;
                case "BEQ":
                    load("$t0", dst);
                    load("$t1", src1);
                    print_mips(new Mips("beq", "$t0", "$t1", src2));
                    break;
                case "PARA":
                    funcStack.push(quadruples.get(index));
                    break;
                case "FUNCRET":
                    save("$v0", dst);
                    break;
                case "RET":
                    load("$v0", dst);
                    break;
                default:
                    break;

            }
            index++;
        }
        funcEnd();
        index++;
    }

    public void callFunc(int dim) {
        print_mips(new Mips("#call", quadruples.get(index).dst, "", ""));
        int size = funcStack.size();
        int lastSP = currentSP;

        for (int i = 0; i < dim; i++) {

            Quadruple quadruple = funcStack.get(size - dim + i);
            String dst = quadruple.dst;
            String real = quadruple.src1;
            int rangey = Integer.parseInt(quadruple.src2);
            print_mips(new Mips("#getpara", dst, real, ""));

            if (real.equals("0")) {
                load("$t0", dst);
                print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
            } else if (real.equals("1") && dst.contains("$")) {
                dst = dst.split("\\$")[0];
                String shift = quadruple.dst.split("\\$")[1];
//                if (varType.get(dst) == 0) {
                load_address("$t0", dst);
                load("$t1", shift);
                if (rangey == 0) {
                    print_mips(new Mips("sll", "$t1", "$t1", "2"));
                    print_mips(new Mips("addu", "$t0", "$sp", "$t0"));
                    print_mips(new Mips("addu", "$t0", "$t0", "$t1"));
                } else {
                    print_mips(new Mips("li", "$t2", String.valueOf(rangey), ""));
                    print_mips(new Mips("mul", "$t2", "$t2", "$t1"));
                    print_mips(new Mips("sll", "$t2", "$t2", "2"));
                    print_mips(new Mips("addu", "$t0", "$t0", "$t2"));
                }
                print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
//                }
            } else {
                load_address("$t0", dst);
                print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
            }

            currentSP += 4;
        }

        for (int i = 0; i < dim; i++) {
            funcStack.pop();
        }

        print_mips(new Mips("sw", "$ra", currentSP + "($sp)", ""));
        currentSP += 4;

        print_mips(new Mips("addu", "$sp", "$sp", String.valueOf(currentSP)));
        print_mips(new Mips("jal", quadruples.get(index).dst, "", ""));

        print_mips(new Mips("lw", "$ra", "-4($sp)", ""));
        print_mips(new Mips("sub", "$sp", "$sp", String.valueOf(currentSP)));
        currentSP = lastSP;
    }

    public void recover(int shift, String dst, String src1, String src2) {
//        int shift = 0;

        varTable.put(dst, -shift);
        varDim.put(dst, Integer.valueOf(src1));
        varReal.put(dst, Integer.valueOf(src2));
        if (src1.equals("0")) {
            varType.put(dst, 0);
        } else {
            varType.put(dst, 1);
        }

    }

    public void funcEnd() {
        currentSP = 0;
        varType = new HashMap<>();
        varTable = new HashMap<>();
        varDim = new HashMap<>();
    }

    public void printString() {
        String s = quadruples.get(index).dst;
        if (debug) print_mips(new Mips("#print", s, "", ""));

        print_data(new Mips("", ".str" + strCnt + ":", ".asciiz", "\"" + s + "\""));
        print_mips(new Mips("addiu", "$v0", "$0", "4"));
        print_mips(new Mips("la", "$a0", ".str" + strCnt++, ""));
        print_mips(new Mips("syscall"));
    }

    public void printChar() {
        if (debug) print_mips(new Mips("#print", quadruples.get(index).dst, "", ""));
        print_mips(new Mips("addiu", "$v0", "$0", "11"));
        print_mips(new Mips("li", "$a0", "10", ""));
        print_mips(new Mips("syscall"));
    }

    public void printInt(String dst) {
        if (debug) print_mips(new Mips("#print", quadruples.get(index).dst, "", ""));
        load("$a0", dst);
        print_mips(new Mips("addiu", "$v0", "$0", "1"));
        print_mips(new Mips("syscall"));
    }

    public void localDefine() {
        if (debug) print_mips(new Mips("#define", quadruples.get(index).dst, "", ""));
        Quadruple quadruple = quadruples.get(index);
        String name = quadruple.dst;
        String src1 = quadruple.src1;
        String src2 = quadruple.src2;
        int dim = (src1.equals("0") && src2.equals("0")) ? 0 : src2.equals("0") ? 1 : 2;

        varTable.put(name, currentSP);
        varType.put(name, 0);
        varDim.put(name, dim);
        varReal.put(name, Integer.parseInt(src2));

        //TODO 把维数加入符号表
        if (quadruples.get(index + 1).isDefineEnd()) {
            int len = getLen(src1, src2);
            currentSP = currentSP + len * 4;
            index++;
        } else {
            index++;
            if (quadruples.get(index).isConstDefine()) {
                while (!quadruples.get(index).isDefineEnd()) {
                    print_mips(new Mips("li", "$t0", quadruples.get(index).src1, ""));
                    print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
                    currentSP += 4;
                    index++;
                }
            } else {
                while (!quadruples.get(index).isDefineEnd()) {
                    int pos = varTable.get(quadruples.get(index).src1);
                    print_mips(new Mips("lw", "$t0", pos + "($sp)", ""));
                    print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
                    currentSP += 4;
                    index++;
                }
            }
        }
    }

    public void li() {
        if (debug) print_mips(new Mips("#li", quadruples.get(index).dst, quadruples.get(index).src1, ""));
        String name = quadruples.get(index).dst;

        varTable.put(name, currentSP);
        varType.put(name, 0);
        varDim.put(name, 0);
        varReal.put(name, 0);

        print_mips(new Mips("li", "$t0", quadruples.get(index).src1, ""));
        print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
        currentSP += 4;
    }

    public void lw() {
        if (debug) print_mips(new Mips("#lw", quadruples.get(index).dst,
                quadruples.get(index).src1, quadruples.get(index).src2));

        String name = quadruples.get(index).dst;
        varTable.put(name, currentSP);
        varType.put(name, 0);
        varDim.put(name, 0);
        varReal.put(name, 0);
        if (quadruples.get(index).src2.equals("")) {
            load("$t0", quadruples.get(index).src1);
        } else {
            load("$t0", quadruples.get(index).src1, quadruples.get(index).src2);
        }
        print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));

        currentSP += 4;
    }

    public void load(String dst, String src) {
        print_mips(new Mips("#load", dst, src, ""));
        if (global.contains(src)) {
            print_mips(new Mips("lw", dst, src, ""));
        } else if (isNumber(src)) {
            print_mips(new Mips("li", dst, src, ""));
        } else if (src.startsWith("$")) {
            if (!src.equals(dst)) {
                print_mips(new Mips("move", dst, src, ""));
            }
        } else if (varType.get(src) == 1) {
            load_address("$t4", src);
            print_mips(new Mips("lw", "$t4", "($t4)", ""));
            print_mips(new Mips("lw", dst, "($t4)", ""));
        } else {
            int pos_shift = varTable.get(src);
            print_mips(new Mips("lw", dst, pos_shift + "($sp)", ""));
        }
    }

    public void load(String dst, String src1, String src2) {
        print_mips(new Mips("#load", dst, src1, src2));
        load("$t3", src2);

        if (global.contains(src1)) {
            print_mips(new Mips("lw", dst, src1 + "($t3)", ""));
        } else if (varType.get(src1) == 1 && varDim.get(src1) > 0) {
            load_address("$t4", src1);
            print_mips(new Mips("addu", "$t3", "$t4", "$t3"));
            print_mips(new Mips("lw", dst, "($t3)", ""));
        } else {
            load_address("$t4", src1);
            print_mips(new Mips("addu", "$t3", "$t4", "$t3"));
            print_mips(new Mips("lw", dst, "($t3)", ""));
        }
    }

    public void load_address(String dst, String src) {
        print_mips(new Mips("#load adrress", dst, src, ""));

        if (global.contains(src)) {
            print_mips(new Mips("la", dst, src, ""));
        } else if (varType.get(src) == 1) {
            int pos_shift = -varTable.get(src);
            print_mips(new Mips("subiu", dst, "$sp", String.valueOf(pos_shift)));
            print_mips(new Mips("lw", dst, "(" + dst + ")", ""));
        } else {
            int pos_shift = varTable.get(src);
            if (pos_shift < 0) {
                pos_shift = -pos_shift;
                print_mips(new Mips("li", dst, String.valueOf(pos_shift), ""));
                print_mips(new Mips("subu", dst, "$sp", dst));
            } else {
                print_mips(new Mips("li", dst, String.valueOf(pos_shift), ""));
                print_mips(new Mips("addu", dst, dst, "$sp"));
            }
        }
    }

    public void sw(String dst, String src1, String src2) {
        if (debug) print_mips(new Mips("#sw", dst, src1, src2));

        load("$t0", src1);

        if (src2.equals("@getInt")) {   //输入
            print_mips(new Mips("li", "$v0", "5", ""));
            print_mips(new Mips("syscall"));
            print_mips(new Mips("move", "$t1", "$v0", ""));
        } else {
            load("$t1", src2);
        }

        if (global.contains(dst)) {
            print_mips(new Mips("sw", "$t1", dst + "($t0)", ""));
        } else {
            int pos = varTable.get(dst);
            if (pos < 0) {
                pos = -pos;
                print_mips(new Mips("subiu", "$t2", "$sp", String.valueOf(pos)));
                if (varDim.get(dst) != 0) {
                    print_mips(new Mips("lw", "$t2", "($t2)", ""));
                }
                print_mips(new Mips("addu", "$t2", "$t0", "$t2"));
            } else {
                print_mips(new Mips("addiu", "$t2", "$sp", String.valueOf(pos)));
                print_mips(new Mips("addu", "$t2", "$t0", "$t2"));
            }
            print_mips(new Mips("sw", "$t1", "($t2)", ""));
        }

    }


    public void cal_two(String op, String dst, String src1, String src2) {
        if (debug) print_mips(new Mips("#" + op.toLowerCase(), dst, src1, src2));
        load("$t0", src1);
        load("$t1", src2);
        switch (op) {
            case "ADD":
                print_mips(new Mips("addu", "$t2", "$t0", "$t1"));
                break;
            case "SUB":
                print_mips(new Mips("subu", "$t2", "$t0", "$t1"));
                break;
            case "MUL":
                print_mips(new Mips("mul", "$t2", "$t0", "$t1"));
                break;
            case "DIV":
                print_mips(new Mips("div", "$t0", "$t1", ""));
                print_mips(new Mips("mflo", "$t2", "", ""));
                break;
            case "MOD":
                print_mips(new Mips("div", "$t0", "$t1", ""));
                print_mips(new Mips("mfhi", "$t2", "", ""));
                break;
            case "EQ":
                equal();
                break;
            case "LSS":
                print_mips(new Mips("slt", "$t2", "$t0", "$t1"));
                break;
            case "LEQ":
                print_mips(new Mips("sle", "$t2", "$t0", "$t1"));
                break;
            case "GRT":
                print_mips(new Mips("slt", "$t2", "$t1", "$t0"));
                break;
            case "GEQ":
                print_mips(new Mips("sle", "$t2", "$t1", "$t0"));
                break;
            case "NEQ":
                neq();
                break;
        }

        save("$t2", dst);
    }

    public void cal_one(String op, String dst, String src1, String src2) {
        if (debug) print_mips(new Mips("#" + op.toLowerCase(), dst, src1, src2));
        load("$t0", src1);

        switch (op) {
            case "SLL":
                print_mips(new Mips("sll", "$t2", "$t0", src2));
                break;
            case "NOT":
                load("$t1", "0");
                equal();
                break;
            case "BEQZ":
                print_mips(new Mips("beq", "$t0", "$0", src2));
                break;
            case "NEQZ":
                neqz();
                break;
        }

        save("$t2", dst);
    }

    public void save(String d, String dst) {
        if (varTable.containsKey(dst)) {
            int pos = varTable.get(dst);
            print_mips(new Mips("sw", d, pos + "($sp)", ""));
        } else {
            varTable.put(dst, currentSP);
            varType.put(dst, 0);
            varDim.put(dst, 0);
            varReal.put(dst, 0);
            print_mips(new Mips("sw", d, currentSP + "($sp)", ""));
        }
        currentSP += 4;
    }

    public void equal() {
        print_mips(new Mips("slt", "$t2", "$t0", "$t1"));    //
        print_mips(new Mips("slt", "$t3", "$t1", "$t0"));
        print_mips(new Mips("or", "$t2", "$t2", "$t3"));
        print_mips(new Mips("li", "$t3", "1", ""));
        print_mips(new Mips("sub", "$t2", "$t3", "$t2"));
    }

    public void neq() {
        print_mips(new Mips("sltu", "$t2", "$t0", "$t1"));    //
        print_mips(new Mips("sltu", "$t3", "$t1", "$t0"));
        print_mips(new Mips("or", "$t2", "$t2", "$t3"));
    }

    public void neqz() {
        print_mips(new Mips("seq", "$t2", "$0", "$t0"));    // t2 = 1 :
        print_mips(new Mips("li", "$t3", "1", ""));
        print_mips(new Mips("sub", "$t2", "$t3", "$t2"));
    }

    public boolean isNumber(String s) {
        Pattern pattern = Pattern.compile("[-[0-9]]*");
        return pattern.matcher(s).matches();
    }

    public void output() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("mips.txt"));
            for (Mips mip : data) {
                out.write(mip.toString() + "\n");
            }

            out.write("\n");

            for (Mips mip : mips) {
                out.write(mip.toString() + "\n");
            }
            out.close();
        } catch (Exception e) {
            //
        }
    }

    public void print_mips(Mips mips) {
//        System.out.println(mips);
        this.mips.add(mips);
    }

    public void print_data(Mips mips) {
//        System.out.println(mips);
        this.data.add(mips);
    }

}
