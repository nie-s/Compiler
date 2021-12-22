import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class Optimizer {
    long[] pow = new long[100];
    ArrayList<Quadruple> quadruples;

    HashMap<String, String> assignMap = new HashMap<>();
    HashMap<String, Integer> dimensions = new HashMap<>();

    public Optimizer(ArrayList<Quadruple> quadruples) {
        this.quadruples = quadruples;
        pow[0] = 1;
        for (int i = 1; i <= 70; i++) {
            pow[i] = pow[i - 1] * 2;
        }

    }

    public void optimize() {
        deleteLval();
        constValue();
        deadCode();
        tempAllocate();
    }


    public void deleteLval() {
        HashMap<String, String> lvals = new HashMap<>();
        Iterator<Quadruple> it = quadruples.iterator();
        while (it.hasNext()) {
            Quadruple quadruple = it.next();
            if (quadruple.op.equals("LVAL")) {
                lvals.put(quadruple.dst, quadruple.src1);
                it.remove();
            }

            if (quadruple.dst.contains("$tmp@")) {
                String split = quadruple.dst.split("[/$]")[1];
                if (lvals.containsKey(split)) {
                    quadruple.dst = quadruple.dst.split("[/$]")[0] + "$" + lvals.get(split);
                }

            } else if (lvals.containsKey(quadruple.dst)) {
                quadruple.dst = lvals.get(quadruple.dst);
            }

            if (lvals.containsKey(quadruple.src1)) {
                quadruple.src1 = lvals.get(quadruple.src1);
            }

            if (lvals.containsKey(quadruple.src2)) {
                quadruple.src2 = lvals.get(quadruple.src2);
            }
        }
    }

    public void constValue() {
        HashMap<String, String> values = new HashMap<>();
        HashMap<String, String> idents = new HashMap<>();
        ArrayList<String> banned = new ArrayList<>();
        HashMap<String, HashMap<String, String>> funcs = new HashMap<>();
        String funcName = "";
        Iterator<Quadruple> it = quadruples.iterator();
        boolean global = true;

        while (it.hasNext()) {
            Quadruple quadruple = it.next();
            String op = quadruple.op;
            String dst = quadruple.dst;
            String src1 = quadruple.src1;
            String src2 = quadruple.src2;
            if (op.equals("LABEL") && dst.equals("GLOBAL_END:")) {
                global = false;
                continue;
            } else if (global) {
                continue;
            } else if (op.startsWith("FUNC_")) {
                values = new HashMap<>();
                idents = new HashMap<>();
                banned = new ArrayList<>();
                funcName = op.substring(5, op.length() - 1);
                continue;
            } else if (op.equals("F_END:")) {
                funcs.put(funcName, idents);
            }

            if ((op.equals("RET") || op.equals("WI") || op.equals("PARA")) && values.containsKey(dst)) {
                quadruple.dst = values.get(dst);
                dst = quadruple.dst;
            }

            if (values.containsKey(src1)) {
                quadruple.src1 = values.get(src1);
                src1 = quadruple.src1;
            }
            if (values.containsKey(src2)) {
                quadruple.src2 = values.get(src2);
                src2 = quadruple.src2;
            }


            if ((op.equals("SUBI") || op.equals("SUB")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) - Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("ADDI") || op.equals("ADD")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) + Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("MUL")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) * Integer.parseInt(src2)));
                it.remove();
            } else if (op.equals("ASS")) {
                if (isNumber(src1) && !banned.contains(dst)) {
                    idents.put(dst, src1);
                } else {
                    idents.remove(dst);
                    banned.add(dst);
                }
            } else if (op.equals("SW")) {
                idents.remove(dst);
                banned.add(dst);
            } else if (op.equals("DEFINE") && !(src1.equals("0") && src2.equals("0"))) {
                banned.add(dst);
            } else if ((op.equals("DIV")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) / Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("MOD")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) % Integer.parseInt(src2)));
                it.remove();
            }
        }

        it = quadruples.iterator();
        while (it.hasNext()) {
            Quadruple quadruple = it.next();
            String op = quadruple.op;
            String dst = quadruple.dst;
            String src1 = quadruple.src1;
            String src2 = quadruple.src2;
            if (op.equals("LABEL") && dst.equals("GLOBAL_END:")) {
                global = false;
                continue;
            } else if (global) {
                continue;
            } else if (op.startsWith("FUNC_")) {
                values = new HashMap<>();
                funcName = op.substring(5, op.length() - 1);
                idents = funcs.get(funcName);
                continue;
            }

            if ((op.equals("RET") || op.equals("WI") || op.equals("PARA")) && values.containsKey(dst)) {
                quadruple.dst = values.get(dst);
                dst = quadruple.dst;
            }

            if (values.containsKey(src1)) {
                quadruple.src1 = values.get(src1);
                src1 = quadruple.src1;
            }
            if (values.containsKey(src2)) {
                quadruple.src2 = values.get(src2);
                src2 = quadruple.src2;
            }

//            if ((op.equals("RET") || op.equals("WI") || op.equals("PARA")) && idents.containsKey(dst)) {
//                System.out.println(dst);
//                quadruple.dst = idents.get(dst);
//                dst = quadruple.dst;
//            }

            if (idents.containsKey(src1)) {
                quadruple.src1 = idents.get(src1);
                src1 = quadruple.src1;
            }
            if (idents.containsKey(src2)) {
                quadruple.src2 = idents.get(src2);
                src2 = quadruple.src2;
            }


            if ((op.equals("SUBI") || op.equals("SUB")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) - Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("ADDI") || op.equals("ADD")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) + Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("MUL")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) * Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("DIV")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) / Integer.parseInt(src2)));
                it.remove();
            } else if ((op.equals("MOD")) && dst.startsWith("tmp@")
                    && isNumber(src1) && isNumber(src2)) {
                values.put(dst, String.valueOf(Integer.parseInt(src1) % Integer.parseInt(src2)));
                it.remove();
            }
        }

    }

    public void tempAllocate() {
        for (int i = 0; i < quadruples.size() - 2; i++) {
            if (quadruples.get(i).dst.contains("tmp@")) {
                String temp = quadruples.get(i).dst;
                if (quadruples.get(i + 1).src1.equals(temp)) {
                    quadruples.get(i + 1).src1 = "$t5";
                    quadruples.get(i).dst = "$t5";
                }
                if (quadruples.get(i + 1).src2.equals(temp)) {
                    quadruples.get(i + 1).src2 = "$t5";
                    quadruples.get(i).dst = "$t5";
                }

                if (quadruples.get(i + 1).dst.equals(temp)
                        && (quadruples.get(i + 1).op.equals("BEQ")
                        || quadruples.get(i + 1).op.equals("BEQZ")
                )) {
                    quadruples.get(i + 1).dst = "$t6";
                    quadruples.get(i).dst = "$t6";
                }

                if (quadruples.get(i + 2).src1.equals(temp)
                        && quadruples.get(i + 1).op.equals("DEFINE")
                        && quadruples.get(i + 2).op.equals("ASS")) {
                    quadruples.get(i + 2).src1 = "$t6";
                    quadruples.get(i).dst = "$t6";
                }
            }
        }
    }

    public void deadCode() {
        int i;
        for (i = 0; i < quadruples.size() - 2; i++) {
            Quadruple quadruple = quadruples.get(i);
            if (quadruple.op.equals("LABEL") && quadruple.dst.equals("GLOBAL_END:")) {
                break;
            }
            if (quadruple.op.equals("DEFINE")) {
                int dim = quadruple.src1.equals("0") && quadruple.src2.equals("0") ? 0 :
                        quadruple.src2.equals("0") ? 1 : 2;
                dimensions.put(quadruple.dst, dim);
            }

        }

        assignMap = new HashMap<>();
        assignMap.put("-1", "-1");
        for (; i < quadruples.size(); i++) {
            Quadruple quadruple = quadruples.get(i);
            setGroup(quadruple.op, quadruple.dst, quadruple.src1, quadruple.src2);
        }

        judgeAssign();
    }

    public void judgeAssign() {
        HashMap<String, Integer> vis = new HashMap<>();
        for (String name : assignMap.keySet()) {
            String fa = getAssignMap(name);
            if (name.startsWith("tmp@") || name.equals("@getInt"))
                continue;

            vis.putIfAbsent(fa, 0);
            vis.put(fa, vis.get(fa) + 1);
        }

        Iterator<Quadruple> it = quadruples.iterator();
        boolean global = true;
        ArrayList<String> toDelete = new ArrayList<>();

        while (it.hasNext()) {
            Quadruple quadruple = it.next();
            if (quadruple.op.equals("LABEL") && quadruple.dst.equals("GLOBAL_END:")) {
                global = false;
                continue;
            }
            if (!global && (quadruple.op.equals("ASS") || quadruple.op.equals("SW"))) {
                String dst = quadruple.dst;
                if (dimensions.containsKey(dst) && dimensions.get(dst) == 0) {
                    if (!assignMap.containsKey(dst) || getAssignMap(dst).equals(getAssignMap("-1"))
                            || quadruple.src2.equals("@getInt")) {
                        continue;
                    }
                    if (vis.get(getAssignMap(dst)) == 1) {

                        toDelete.add(dst);
                    }
                }
            }
        }
        boolean hasChange = true;
        while (hasChange) {
            hasChange = false;
            it = quadruples.iterator();
            global = true;
            while (it.hasNext()) {
                Quadruple quadruple = it.next();
                if (quadruple.op.equals("LABEL") && quadruple.dst.equals("GLOBAL_END:")) {
                    global = false;
                    continue;
                }
                if (!global && (toDelete.contains(quadruple.src1) ||
                        toDelete.contains(quadruple.src2) || toDelete.contains(quadruple.dst))) {
                    it.remove();

                    if (quadruple.dst.startsWith("tmp@"))
                        toDelete.add(quadruple.dst);

                    hasChange = true;
                }
            }
        }


    }

    public void setGroup(String op, String dst, String src1, String src2) {
        if (src2.equals("@getInt")) {
            insertDead(dst);
        }
        switch (op) {
            case "DEFINE":
            case "RECOVER":
                int dim = src1.equals("0") && src2.equals("0") ? 0 : src2.equals("0") ? 1 : 2;
                dimensions.put(dst, dim);
                break;
            case "WI":
            case "BEQZ":
            case "FUNCRET":
            case "RET":
            case "PARA":
                insertDead(dst);
                break;
            case "EQ":
            case "NEQ":
            case "LSS":
            case "LEQ":
            case "GRT":
            case "GEQ":
            case "ADD":
            case "SUB":
            case "MUL":
            case "DIV":
            case "MOD":
            case "SW":
                combine(dst, src1);
                combine(src1, src2);
                combine(dst, src2);
                break;
            case "NOT":
            case "NEQZ":
            case "SLL":
            case "ADDI":
            case "SUBI":
            case "ASS":
                combine(dst, src1);
                break;
            case "LW":
                combine(src1, src2);
                break;
            case "BEQ":
                insertDead(dst);
                insertDead(src1);
                break;
        }


    }

    public void combine(String a, String b) {
        if (isNumber(a) || isNumber(b)
                || a.startsWith("@") || b.startsWith("@")
                || a.equals("@getInt") || b.equals("@getInt")) {
            return;
        }
        assignMap.putIfAbsent(a, a);
        assignMap.putIfAbsent(b, b);

        String temp1 = getAssignMap(a);
        String temp2 = getAssignMap(b);

        assignMap.put(temp1, temp2);
    }

    public void insertDead(String dst) {
        if (isNumber(dst)) {
            return;
        }
        assignMap.putIfAbsent(dst, dst);
        String temp = getAssignMap(dst);
        assignMap.put(temp, getAssignMap("-1"));
    }

    public String getAssignMap(String dst) {
        if (assignMap.get(dst).equals(dst)) {
            return dst;
        }
        String temp = assignMap.get(dst);
        String real = getAssignMap(temp);
        assignMap.put(dst, real);
        return real;
    }

    public int log(int num) {
        int cnt = 0;
        while (num != 0) {
            cnt += 1;
            num >>= 1;
        }
        return cnt;
    }

    public int getSign(int num) {
        return num >= 0 ? 0 : -1;
    }

    public void div_mod(String reg, String s, ArrayList<MipsGenerator.Mips> mips, boolean isMod) {
        int num = Integer.parseInt(s);
        int l = Math.max(1, log(Math.abs(num)));
        long m = (long) 1 + (pow[32 + l - 1] / ((long) Math.abs(num)));
        int m_ = (int) (m - pow[32]);
        int d_sign = getSign(num);
        int sh_post = l - 1;

        mips.add(new MipsGenerator.Mips("mul", "$t2", reg, String.valueOf(m_)));
        mips.add(new MipsGenerator.Mips("mfhi", "$t2", "", ""));
        mips.add(new MipsGenerator.Mips("add", "$t2", reg, "$t2"));
        mips.add(new MipsGenerator.Mips("sra", "$t2", "$t2", String.valueOf(sh_post)));
        mips.add(new MipsGenerator.Mips("slt", "$t3", reg, "$0"));
        mips.add(new MipsGenerator.Mips("neg", "$t3", "$t3", ""));
        mips.add(new MipsGenerator.Mips("subu", "$t3", "$t2", "$t3"));
        if (d_sign != 0) {
            mips.add(new MipsGenerator.Mips("xori", "$t3", "$t3", String.valueOf(d_sign)));
        }
        if (d_sign != 0) {
            mips.add(new MipsGenerator.Mips("subiu", "$t3", "$t3", String.valueOf(d_sign)));
        }

        if (isMod) {
            mips.add(new MipsGenerator.Mips("mul", "$t3", "$t3", String.valueOf(num)));
            mips.add(new MipsGenerator.Mips("subu", "$t3", reg, "$t3"));
        }
    }

    public void output() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("optimized.txt"));
            for (Quadruple quadruple : quadruples) {
                out.write(quadruple.toString() + "\n");
            }
            out.close();

        } catch (Exception e) {
            //
        }
    }

    public void opMips(ArrayList<MipsGenerator.Mips> mips) {
        Iterator<MipsGenerator.Mips> it = mips.iterator();
        for (int i = 2; i < mips.size() - 1; i++) {
            MipsGenerator.Mips code = mips.get(i);
            MipsGenerator.Mips next = mips.get(i + 1);
            if ((code.op.equals("addi") || code.op.equals("subi") || code.op.equals("addu"))
                    && next.op.equals("move") &&
                    code.dst.equals(next.src1)) {
                code.dst = next.dst;
                next.src1 = next.dst;
            }
        }
        while (it.hasNext()) {
            MipsGenerator.Mips code = it.next();
            if (code.op.equals("move") && code.dst.equals(code.src1)) {
                it.remove();
            }
        }
    }

    public boolean isNumber(String s) {
        Pattern pattern = Pattern.compile("[-[0-9]]*");
        return pattern.matcher(s).matches();
    }

}
