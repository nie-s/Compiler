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

    HashMap<String, String> lvalMap = new HashMap<>();

    Stack<Quadruple> funcStack = new Stack<>();

    Optimizer optimizer;

    int index = 0;
    int strCnt = 0;
    int currentSP = 0;
    boolean debug = false;

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

    public MipsGenerator(ArrayList<Quadruple> quadruples, Optimizer optimizer) {
        this.quadruples = quadruples;
        this.optimizer = optimizer;
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
                case "EQ":
                case "LSS":
                case "LEQ":
                case "GRT":
                case "GEQ":
                case "NEQ":
                    cal_two(op, dst, src1, src2);
                    break;
                case "DIV":
                case "MOD":
                    div_mod(op, dst, src1, src2);
                    break;
                case "NEQZ":
                case "NOT":
                case "SLL":
                case "ADDI":
                case "SUBI":
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
                    String reg = load("$t0", dst);
                    print_mips(new Mips("beq", reg, "$0", src1));
                    break;
                case "BEQ":
                    String reg1 = load("$t0", dst);
                    String reg2 = load("$t1", src1);
                    print_mips(new Mips("beq", reg1, reg2, src2));
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
                case "CONST":
                    number(dst, src1);
                case "LVAL":
                    lval(dst, src1);
                default:
                    break;

            }
            index++;
        }
        funcEnd();
        index++;
    }

    public void callFunc(int dim) {
        if (debug) print_mips(new Mips("#call", quadruples.get(index).dst, "", ""));
        int size = funcStack.size();
        int lastSP = currentSP;

        for (int i = 0; i < dim; i++) {

            Quadruple quadruple = funcStack.get(size - dim + i);
            String dst = quadruple.dst;
            String real = quadruple.src1;
            int rangey = Integer.parseInt(quadruple.src2);
            if (debug) print_mips(new Mips("#getpara", dst, real, ""));

            if (real.equals("0")) {
                String reg = load("$t0", dst);
                print_mips(new Mips("sw", reg, currentSP + "($sp)", ""));
            } else if (real.equals("1") && dst.contains("$")) {
                dst = dst.split("\\$")[0];
                String shift = quadruple.dst.split("\\$")[1];
//                if (varType.get(dst) == 0) {
                load_address("$t0", dst);
                String reg = load("$t1", shift);
                if (rangey == 0) {
                    print_mips(new Mips("sll", reg, reg, "2"));
                    print_mips(new Mips("addu", "$t0", "$sp", "$t0"));
                    print_mips(new Mips("addu", "$t0", "$t0", reg));
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
                    //TODO 0没有赋值
                    if (!quadruples.get(index).src1.equals("0")) {
                        print_mips(new Mips("li", "$t0", quadruples.get(index).src1, ""));
                        print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
                    } else {
                        print_mips(new Mips("sw", "$0", currentSP + "($sp)", ""));
                    }
                    currentSP += 4;
                    index++;
                }
            } else {
                while (!quadruples.get(index).isDefineEnd()) {
                    if (!quadruples.get(index).src1.equals("0")) {
                        if (isNumber(quadruples.get(index).src1)) {
                            print_mips(new Mips("li", "$t0", quadruples.get(index).src1, ""));
                        } else {
                            String src = quadruples.get(index).src1;
                            if (lvalMap.containsKey(src)) {
                                src = lvalMap.get(src);
                            }
                            if (global.contains(src)) {
                                print_mips(new Mips("lw", "$t0", src, ""));
                            } else {
                                int pos = varTable.get(quadruples.get(index).src1);
                                print_mips(new Mips("lw", "$t0", pos + "($sp)", ""));
                            }
                        }

                        print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
                    } else {
                        print_mips(new Mips("sw", "$0", currentSP + "($sp)", ""));
                    }
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

        String reg_dst = quadruples.get(index).dst.startsWith("$t") ? quadruples.get(index).dst : "$t0";

        if (quadruples.get(index).src2.equals("")) {
            load(reg_dst, quadruples.get(index).src1);
        } else {
            load(reg_dst, quadruples.get(index).src1, quadruples.get(index).src2);
        }

        if (!quadruples.get(index).dst.startsWith("$t")) {
            String name = quadruples.get(index).dst;
            varTable.put(name, currentSP);
            varType.put(name, 0);
            varDim.put(name, 0);
            varReal.put(name, 0);
            print_mips(new Mips("sw", "$t0", currentSP + "($sp)", ""));
        }

        currentSP += 4;
    }

    public void lval(String dst, String src) {
        this.lvalMap.put(dst, src);
        varTable.put(dst, varTable.get(src));
        varType.put(dst, 0);
        varDim.put(dst, 0);
        varReal.put(dst, 0);
    }

    public String load(String dst, String src) {
        if (debug) print_mips(new Mips("#load", dst, src, ""));
        if (src.startsWith("$")) {
            if (src.equals("$v0") || src.equals("$a0")) {
                print_mips(new Mips("move", dst, src, ""));
            } else {
                dst = src;
            }
        } else if (global.contains(src)) {
            print_mips(new Mips("lw", dst, src, ""));
        } else if (isNumber(src)) {
            print_mips(new Mips("li", dst, src, ""));
        } else if (src.startsWith("$")) {
            if (!src.equals(dst) || src.equals("$v0")) {
                print_mips(new Mips("move", dst, src, ""));
            }
        } else if (lvalMap.containsKey(src)) {
            dst = load(dst, lvalMap.get(src));
        } else if (varType.get(src) == 1) {
            load_address("$t4", src);
            print_mips(new Mips("lw", "$t4", "($t4)", ""));
            print_mips(new Mips("lw", dst, "($t4)", ""));
        } else {
            int pos_shift = varTable.get(src);
            print_mips(new Mips("lw", dst, pos_shift + "($sp)", ""));
        }
        return dst;
    }

    public void load(String dst, String src1, String src2) {
        if (debug) print_mips(new Mips("#load", dst, src1, src2));
        String reg3 = load("$t3", src2);

        if (global.contains(src1)) {
            print_mips(new Mips("lw", dst, src1 + "(" + reg3 + ")", ""));
        } else if (varType.get(src1) == 1 && varDim.get(src1) > 0) {
            load_address("$t4", src1);
            print_mips(new Mips("addu", reg3, "$t4", reg3));
            print_mips(new Mips("lw", dst, "(" + reg3 + ")", ""));
        } else {
            load_address("$t4", src1);
            print_mips(new Mips("addu", reg3, "$t4", reg3));
            print_mips(new Mips("lw", dst, "(" + reg3 + ")", ""));
        }
    }

    public void load_address(String dst, String src) {
        if (debug) print_mips(new Mips("#load adrress", dst, src, ""));

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
        String reg = "$t1";
        if (src2.equals("@getInt")) {   //输入
            print_mips(new Mips("li", "$v0", "5", ""));
            print_mips(new Mips("syscall"));
            print_mips(new Mips("move", "$t1", "$v0", ""));
        } else {
            reg = load("$t1", src2);
        }


        if (src1.equals("0")) {
            if (global.contains(dst)) {
                print_mips(new Mips("sw", reg, dst + "($0)", ""));
            } else {
                int pos = varTable.get(dst);
                if (pos < 0) {
                    pos = -pos;
                    print_mips(new Mips("subiu", "$t2", "$sp", String.valueOf(pos)));
                    if (varDim.get(dst) != 0) {
                        print_mips(new Mips("lw", "$t2", "($t2)", ""));
                    }
                    print_mips(new Mips("sw", reg, "($t2)", ""));
                } else {
                    print_mips(new Mips("sw", reg, String.valueOf(pos) + "($sp)", ""));
                }
            }
        } else {
            String reg1 = load("$t0", src1);

            if (global.contains(dst)) {
                print_mips(new Mips("sw", reg, dst + "(" + reg1 + ")", ""));
            } else {
                int pos = varTable.get(dst);
                if (pos < 0) {
                    pos = -pos;
                    print_mips(new Mips("subiu", "$t2", "$sp", String.valueOf(pos)));
                    if (varDim.get(dst) != 0) {
                        print_mips(new Mips("lw", "$t2", "($t2)", ""));
                    }
                    print_mips(new Mips("addu", "$t2", reg1, "$t2"));
                } else {
                    print_mips(new Mips("addiu", "$t2", "$sp", String.valueOf(pos)));
                    print_mips(new Mips("addu", "$t2", reg1, "$t2"));
                }
                print_mips(new Mips("sw", reg, "($t2)", ""));
            }
        }
    }

    public void number(String dst, String src) {

    }

    public void cal_two(String op, String dst, String src1, String src2) {
        if (debug) print_mips(new Mips("#" + op.toLowerCase(), dst, src1, src2));
        String reg1 = load("$t0", src1);
        String reg2 = load("$t1", src2);
        String reg_dst = dst.startsWith("$t") ? dst : "$t2";

        switch (op) {
            case "ADD":
                print_mips(new Mips("addu", reg_dst, reg1, reg2));
                break;
            case "SUB":
                print_mips(new Mips("subu", reg_dst, reg1, reg2));
                break;
            case "MUL":
                print_mips(new Mips("mul", reg_dst, reg1, reg2));
                break;
            case "DIV":
                print_mips(new Mips("div", reg1, reg2, ""));
                print_mips(new Mips("mflo", reg_dst, "", ""));
                break;
            case "MOD":
                print_mips(new Mips("div", reg1, reg2, ""));
                print_mips(new Mips("mfhi", reg_dst, "", ""));
                break;
            case "EQ":
                equal(reg1, reg2, reg_dst);
                break;
            case "LSS":
                print_mips(new Mips("slt", reg_dst, reg1, reg2));
                break;
            case "LEQ":
                print_mips(new Mips("sle", reg_dst, reg1, reg2));
                break;
            case "GRT":
                print_mips(new Mips("slt", reg_dst, reg2, reg1));
                break;
            case "GEQ":
                print_mips(new Mips("sle", reg_dst, reg2, reg1));
                break;
            case "NEQ":
                neq(reg1, reg2, reg_dst);
                break;
        }

        save(reg_dst, dst);
    }

    public void cal_one(String op, String dst, String src1, String src2) {
        if (debug) print_mips(new Mips("#" + op.toLowerCase(), dst, src1, src2));
        String reg = load("$t0", src1);
        String reg_dst = dst.startsWith("$t") ? dst : "$t2";
        switch (op) {
            case "SLL":
                print_mips(new Mips("sll", reg_dst, reg, src2));
                break;
            case "NOT":
                equal(reg, "$0", reg_dst);
                break;
            case "BEQZ":
                print_mips(new Mips("beq", reg, "$0", src2));
                break;
            case "NEQZ":
                neqz(reg, reg_dst);
                break;
            case "ADDI":
                print_mips(new Mips("addi", reg_dst, reg, src2));
                break;
            case "SUBI":
                print_mips(new Mips("subi", reg_dst, reg, src2));
                break;
        }

        save(reg_dst, dst);
    }

    public void div_mod(String op, String dst, String src1, String src2) {
        print_mips(new Mips("##mod_div", dst, src1, src2));
        if (isNumber(src2)) {
            String reg = load("$t1", src1);
            optimizer.div_mod(reg, src2, mips, op.equals("MOD"));
            save("$t3", dst);
            if (dst.contains("$t")) {
                print_mips(new Mips("move", dst, "$t3", ""));
            }
        } else {
            cal_two(op, dst, src1, src2);
        }
    }

    public void save(String d, String dst) {

        if (dst.contains("$t")) {
            if (d.equals("$v0")) {
                print_mips(new Mips("move", dst, "$v0", ""));
            }
            return;
        }

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

    public void equal(String reg1, String reg2, String reg_dst) {
        print_mips(new Mips("slt", "$t4", reg1, reg2));    //
        print_mips(new Mips("slt", "$t3", reg2, reg1));
        print_mips(new Mips("or", "$t4", "$t4", "$t3"));
        print_mips(new Mips("li", "$t3", "1", ""));
        print_mips(new Mips("sub", reg_dst, "$t3", "$t4"));
    }

    public void neq(String reg1, String reg2, String reg_dst) {
        print_mips(new Mips("sltu", reg_dst, reg1, reg2));    //
        print_mips(new Mips("sltu", "$t3", reg2, reg1));
        print_mips(new Mips("or", reg_dst, reg_dst, "$t3"));
    }

    public void neqz(String reg1, String reg_dst) {
        print_mips(new Mips("seq", reg_dst, "$0", reg1));
        print_mips(new Mips("li", "$t3", "1", ""));
        print_mips(new Mips("sub", reg_dst, "$t3", reg_dst));
    }

    public boolean isNumber(String s) {
        Pattern pattern = Pattern.compile("[-[0-9]]*");
        return pattern.matcher(s).matches();
    }

    public void output() {
        try {
            saveAndLoad();

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

    public void saveAndLoad() {
        for (int i = 0; i < mips.size() - 2; i++) {
            Mips mip = mips.get(i);
//            sw        $t2         0($sp)
//            lw        $t0         0($sp)
            if (mips.get(i).op.equals("sw") && mips.get(i + 1).op.equals("lw")
                    && mips.get(i).src1.equals(mips.get(i + 1).src1)
                    && mips.get(i + 1).dst.startsWith("$t") && mips.get(i).dst.startsWith("$t")) {
                mips.get(i + 1).op = "move";
                mips.get(i + 1).src1 = mips.get(i).dst;
                i++;
            }
        }
    }

}
