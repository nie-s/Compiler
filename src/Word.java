import java.util.regex.Pattern;

public class Word {
    String value;
    String type;
    int lineCnt;

    public Word() {

    }

    public Word(String value, String type, int lineCnt) {
        this.value = value;
        this.type = type;
        this.lineCnt = lineCnt;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public boolean isConst() {
        return type.equals("CONSTTK");
    }

    public boolean isInt() {
        return type.equals("INTTK");
    }

    public boolean isVoid() {
        return type.equals("VOIDTK");
    }

    public boolean isMain() {
        return type.equals("MAINTK");
    }

    public boolean isBreak() {
        return type.equals("BREAKTK");
    }

    public boolean isContinue() {
        return type.equals("CONTINUETK");
    }

    public boolean isIf() {
        return type.equals("IFTK");
    }

    public boolean isElse() {
        return type.equals("ELSETK");
    }

    public boolean isWhile() {
        return type.equals("WHILETK");
    }

    public boolean isGetInt() {
        return type.equals("GETINTTK");
    }

    public boolean isReturn() {
        return type.equals("RETURNTK");
    }

    public boolean isPrintf() {
        return type.equals("PRINTFTK");
    }

    public boolean isReserve() {
        return isReturn() || isBreak() || isInt() || isConst() || isContinue() || isVoid() || isIf()
                || isElse() || isWhile() || isMain() || isGetInt() || isPrintf();
    }

    public boolean isIdent() {
        return type.equals("IDENFR");
    }

    public boolean isSemiColon() {
        return type.equals("SEMICN");
    }

    public boolean isLparent() {
        return type.equals("LPARENT");
    }

    public boolean isRparent() {
        return type.equals("RPARENT");
    }

    public boolean isComma() {
        return type.equals("COMMA");
    }

    public boolean isNot() {
        return type.equals("NOT");
    }

    public boolean isAnd() {
        return type.equals("AND");
    }

    public boolean isOr() {
        return type.equals("OR");
    }

    public boolean isPlus() {
        return type.equals("PLUS");
    }

    public boolean isMinus() {
        return type.equals("MINU");
    }

    public boolean isMult() {
        return type.equals("MULT");
    }

    public boolean isDiv() {
        return type.equals("DIV");
    }

    public boolean isMod() {
        return type.equals("MOD");
    }

    public boolean isLss() {
        return type.equals("LSS");
    }

    public boolean isLeq() {
        return type.equals("LEQ");
    }

    public boolean isGre() {
        return type.equals("GRE");
    }

    public boolean isGeq() {
        return type.equals("GEQ");
    }

    public boolean isEql() {
        return type.equals("EQL");
    }

    public boolean isNeq() {
        return type.equals("NEQ");
    }

    public boolean isAssign() {
        return type.equals("ASSIGN");
    }

    public boolean isLbrack() {
        return type.equals("LBRACK");
    }

    public boolean isRbrack() {
        return type.equals("RBRACK");
    }

    public boolean isLbrace() {
        return type.equals("LBRACE");
    }

    public boolean isRbrace() {
        return type.equals("RBRACE");
    }

    public boolean isNumber() {
        return type.equals("INTCON");
    }

    public boolean isUnaryOp() {
        return this.isMinus() || this.isPlus() || this.isNot();
    }

    public boolean isUnaryCal() {
        return this.isMult() || this.isDiv() || this.isMod();
    }

    public boolean isUnaryAdd() {
        return this.isPlus() || this.isMinus();

    }

    public boolean isStrcon() {
        return type.equals("STRCON");
    }

    public boolean checkStrcon() {
//        <FormatChar> → %d
//        <NormalChar> → ⼗进制编码为32,33,40-126的ASCII字符
        for (int i = 0; i < value.length(); i++) {
            if (!(value.charAt(i) == 32 || value.charAt(i) == 33 ||
                    (value.charAt(i) >= 40 && value.charAt(i) <= 126))) {
                return false;
            }
            if (value.charAt(i) == '%') {
                if (i == value.length() - 1) {
                    return false;
                } else if (value.charAt(i + 1) != 'd') {
                    return false;
                }
                i++;
            }
        }

        return true;
    }

    public String toString() {
        return this.type + ":" + this.value;
    }
}