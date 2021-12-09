import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class Optimizer {
    long[] pow = new long[100];

    public Optimizer() {
        pow[0] = 1;
        for (int i = 1; i <= 70; i++) {
            pow[i] = pow[i - 1] * 2;
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

    public void div_mod(String s, ArrayList<MipsGenerator.Mips> mips, boolean isMod) {
        int num = Integer.parseInt(s);
        int l = Math.max(1, log(Math.abs(num)));
        long m = (long) 1 + (pow[32 + l - 1] / ((long) Math.abs(num)));
        int m_ = (int) (m - pow[32]);
        int d_sign = getSign(num);
        int sh_post = l - 1;

        mips.add(new MipsGenerator.Mips("mul", "$t2", "$t1", String.valueOf(m_)));
        mips.add(new MipsGenerator.Mips("mfhi", "$t2", "", ""));
        mips.add(new MipsGenerator.Mips("add", "$t2", "$t1", "$t2"));
        mips.add(new MipsGenerator.Mips("sra", "$t2", "$t2", String.valueOf(sh_post)));
        mips.add(new MipsGenerator.Mips("slt", "$t3", "$t1", "$0"));
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
            mips.add(new MipsGenerator.Mips("subu", "$t3", "$t1", "$t3"));
        }
    }

}
