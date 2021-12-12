import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Optimizer {
    long[] pow = new long[100];
    ArrayList<Quadruple> quadruples;

    public Optimizer(ArrayList<Quadruple> quadruples) {
        this.quadruples = quadruples;
        pow[0] = 1;
        for (int i = 1; i <= 70; i++) {
            pow[i] = pow[i - 1] * 2;
        }
    }

    public void optimize() {
        deleteLval();
        tempAllocate();
        simplifyAssign();
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

    public void simplifyAssign() {

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
            }
        }
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
}
