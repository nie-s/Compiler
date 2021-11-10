public class Quadruple {
    public String op;
    public String dst;
    public String src1;
    public String src2;
    public boolean global = false;

    public Quadruple(String op, String dst, String src1, String src2) {
        this.op = op;
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }

    public Quadruple(String op, String dst, String src1, String src2, boolean global) {
        this.op = op;
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
        this.global = global;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s", op));
        sb.append(String.format("%-20s", dst));
        sb.append(String.format("%-20s", src1));
        sb.append(src2);
        return sb.toString();
    }

    public enum Operator {
        ADD,    // 加法
        SUB,    // 减法
        MUL,    // 乘法
        DIV,    // 除法
        SLL,
        MOD,

        WI,     // 写整数
        WC,
        WS,

        ASS,    // 赋值
        ASS_CON,

        DEFINE,
        D_END,

        LI,
        LW,
        SW,

        FUNC,    //函数定义
        LABEL,

        J,      // 无条件跳转
        JR,
        JAL,
        EXIT,

        NOT,
        LSS,
        LEQ,
        GRT,
        GEQ,
        NEQ,
        EQ,

        BEQ,    // 等于跳转


        SAVE, //保存现场
        PARA,   // 函数参数

        RET,    // 函数返回
        PUSH,   // 压栈


    }

    public boolean isLabel() {
        return this.op.equals("LABEL");
    }

    public boolean isDefine() {
        return this.op.equals("DEFINE");
    }

    public boolean isDefineEnd() {
        return this.op.equals("D_END");
    }

    public boolean isConstDefine() {
        return this.op.equals("ASS_CON");
    }

    public boolean isRecover() {
        return this.op.equals("RECOVER");
    }
}
