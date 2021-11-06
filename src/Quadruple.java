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
        sb.append(String.format("%-8s", op));
        sb.append(String.format("%-15s", dst));
        sb.append(String.format("%-15s", src1));
        sb.append(src2);
        return sb.toString();
    }

    public enum Operator {
        ADD,    // 加法
        SUB,    // 减法
        MUL,    // 乘法
        DIV,    // 除法
        NOT,
        ASS,    // 赋值
        FAS,    // 给函数赋返回值
        LAB,    // 标签
        CMP,    // 比较
        LSS,
        LEQ,
        GRT,
        GEQ,
        NEQ,
        EQ,
        BG,     // 大于跳转
        BGE,    // 大于或等于跳转
        BL,     // 小于跳转
        BLE,    // 小于或等于跳转
        BEQ,    // 等于跳转
        BNE,    // 不等于跳转
        GOTO,
        J,      // 无条件跳转
        JR,
        JAL,
        SAVE, //保存现场
        PARA,   // 函数参数
        RET,    // 函数返回
        PUSH,   // 压栈
        RI,     // 读整数
        WI,     // 写整数
        WC,
        MAIN,    //main函数定义
        FUNC,    //函数定义
        EXIT,
    }
}
