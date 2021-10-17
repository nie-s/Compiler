import java.util.ArrayList;
import java.util.HashMap;

public class AbstractSyntaxTree {

    public class CompUnit {
        HashMap<String, Decl> dels = new HashMap<>();
        HashMap<String, Func> func = new HashMap<>();
    }

    public class Decl {
        boolean isConst;
        Exp rangex;
        Exp rangey;
        int dimension = 0;
        String name = "";
        InitVal initVal;
        int pos;

        public Decl(boolean isConst, Exp rangex, Exp rangey, String name,
                    InitVal initVal, int pos) {
            this.initVal = initVal;
            this.isConst = isConst;
            this.rangex = rangex;
            this.rangey = rangey;
            this.dimension = 2;
            this.name = name;
            this.pos = pos;
        }

        public Decl(boolean isConst, Exp rangex, String name,
                    InitVal initVal, int pos) {
            this.initVal = initVal;
            this.isConst = isConst;
            this.rangex = rangex;
            this.dimension = 1;
            this.name = name;
            this.pos = pos;
        }

        public Decl(boolean isConst, String name,
                    InitVal initVal, int pos) {
            this.initVal = initVal;
            this.isConst = isConst;
            this.dimension = 1;
            this.name = name;
            this.pos = pos;
        }
    }

    public class InitVal {
        ArrayList<ArrayList<Exp>> exps = new ArrayList<>();
        int pos;

        public InitVal(ArrayList<ArrayList<Exp>> exps, int pos) {
            this.exps = exps;
            this.pos = pos;
        }
    }

    public class Exp {
        ArrayList<MulExp> mulExps = new ArrayList<>();
        int pos;

        public Exp(ArrayList<MulExp> mulExps, int pos) {
            this.mulExps = mulExps;
            this.pos = pos;
        }
    }

    public class MulExp {
        ArrayList<UnaryExp> unaryExps = new ArrayList<>();
        int pos;

        public MulExp(ArrayList<UnaryExp> unaryExps, int pos) {
            this.unaryExps = unaryExps;
            this.pos = pos;
        }
    }

    public class UnaryExp {
        //<UnaryExp> ::= <PrimaryExp> | 1
        //               <Ident> '(' [<FuncRParams>] ')' | 2
        //               <UnaryOp> <UnaryExp> 3
        int type;
        PrimaryExp primaryExp;
        String ident;
        ArrayList<Exp> funcRParams;
        UnaryOp unaryOp;
        UnaryExp unaryExp;
        int pos;

        public UnaryExp(PrimaryExp primaryExp) {
            this.type = 1;
            this.primaryExp = primaryExp;
        }

        public UnaryExp(String ident, ArrayList<Exp> funcRParams) {
            this.type = 2;
            this.funcRParams = funcRParams;
        }

        public UnaryExp(String op, UnaryExp unaryExp) {
            this.type = 3;
            switch (op) {
                case "+" -> this.unaryOp = UnaryOp.PLUS;
                case "-" -> this.unaryOp = UnaryOp.MINUS;
                case "!" -> this.unaryOp = UnaryOp.NOT;
            }
        }
    }

    public enum UnaryOp {
        PLUS, MINUS, NOT
    }

    public class PrimaryExp {
        //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
        int type;
        Exp exp;
        LVal lVal;
        int number;
        int pos;

        public PrimaryExp(Exp exp, int pos) {
            this.type = 1;
            this.exp = exp;
            this.pos = pos;

        }

        public PrimaryExp(LVal lVal, int pos) {
            this.type = 2;
            this.lVal = lVal;
            this.pos = pos;

        }

        public PrimaryExp(int number, int pos) {
            this.type = 3;
            this.number = number;
            this.pos = pos;

        }
    }

    public class LVal {
        Exp rangex;
        Exp rangey;
        int dimension = 0;
        String name = "";
        int pos;

        public LVal(boolean isConst, Exp rangex, Exp rangey, String name,
                    InitVal initVal, int pos) {
            this.rangex = rangex;
            this.rangey = rangey;
            this.dimension = 2;
            this.name = name;
            this.pos = pos;
        }

        public LVal(boolean isConst, Exp rangex, String name,
                    InitVal initVal, int pos) {
            this.rangex = rangex;
            this.dimension = 1;
            this.name = name;
            this.pos = pos;
        }

        public LVal(boolean isConst, String name,
                    InitVal initVal, int pos) {
            this.dimension = 1;
            this.name = name;
            this.pos = pos;
        }

    }


    public class Func {
        String type = "";
        String name = "";
        HashMap<String, FuncFParam> funcFParams = new HashMap<>();
        HashMap<String, Decl> dels = new HashMap<>();
        HashMap<String, Stmt> stmts = new HashMap<>();
        int pos;

        public Func(String type, String name, HashMap<String, FuncFParam> funcFParams,
                    HashMap<String, Decl> dels, HashMap<String, Stmt> stmts, int pos) {
            this.type = type;
            this.name = name;
            this.funcFParams = funcFParams;
            this.dels = dels;
            this.stmts = stmts;
            this.pos = pos;
        }

    }

    public class FuncFParam {
        String name = "";
        Exp constExp;
        int dimension;
        int pos;

        public FuncFParam(String name, Exp constExp, int pos) {
            this.name = name;
            this.constExp = constExp;
            this.dimension = 2;
            this.pos = pos;
        }

        public FuncFParam(String name, int dimension, int pos) {
            this.name = name;
            this.dimension = dimension;
            this.pos = pos;
        }
    }

    public class Stmt {
        //        <Stmt> ::= <LVal> '=' <Exp> ';'
        //                | [Exp] ';' //有无⽆Exp两种情况
        //                | <Block>
        //                | 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
        //                | 'while' '(' <Cond> ')' <Stmt>
        //                | 'break;'
        //                | 'continue;'
        //                | 'return' [<Exp>] ';'
        //                | <LVal> = 'getint();'
        //                | 'printf('FormatString{,<Exp>}');'
        StmtType type;
        LVal lVal;
        Exp exp;
        Block block;
        IfStmt ifStmt;
        WhileStmt whileStmt;
        PrintfStmt printfStmt;
        Exp returnExp;
        int pos;

        public Stmt(LVal lVal, Exp exp, int pos) {
            this.lVal = lVal;
            this.exp = exp;
            this.pos = pos;
            this.type = StmtType.LVALEXP;
        }

        public Stmt(String stmt, int pos) {
            switch (stmt) {
                case "break" -> this.type = StmtType.BREAK;
                case "continue" -> this.type = StmtType.CONTINUE;
                case ";" -> this.type = StmtType.SEMI;
            }
            this.pos = pos;

        }

        public Stmt(Exp exp, int pos) {
            this.type = StmtType.EXP;
            this.exp = exp;
            this.pos = pos;
        }

        public Stmt(Block block, int pos) {
            this.type = StmtType.BLOCK;
            this.block = block;
            this.pos = pos;
        }

        public Stmt(String re, Exp exp, int pos) {
            this.type = StmtType.RETURN;
            this.returnExp = exp;
            this.pos = pos;
        }

        public Stmt(LVal lVal, int pos) {
            this.type = StmtType.GETINT;
            this.lVal = lVal;
            this.pos = pos;
        }

        public Stmt(PrintfStmt printfStmt, int pos) {
            this.printfStmt = printfStmt;
            this.type = StmtType.PRINTF;
            this.pos = pos;
        }

    }

    public enum StmtType {
        LVALEXP, EXP, BLOCK, IF, WHILE, BREAK, RETURN, GETINT, PRINTF, SEMI, CONTINUE
    }

    public class IfStmt {
        Cond cond;
        Stmt thenStmt;
        Stmt elseStmt;
        int pos;

        public IfStmt(Cond cond, Stmt thenStmt, Stmt elseStmt, int pos) {
            this.cond = cond;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
            this.pos = pos;
        }

    }

    public class Block {
        HashMap<String, Decl> dels = new HashMap<>();
        Stmt stmt;
        int pos;

        public Block(HashMap<String, Decl> dels, Stmt stmt, int pos) {
            this.dels = dels;
            this.stmt = stmt;
            this.pos = pos;
        }
    }

    public class Cond {
        ArrayList<LAndExp> lOrExps;
        int pos;

        public Cond(ArrayList<LAndExp> lOrExps, int pos) {
            this.lOrExps = lOrExps;
            this.pos = pos;
        }
    }

    public class LAndExp {
        ArrayList<EqExp> eqExps;
        int pos;

        public LAndExp(ArrayList<EqExp> eqExps, int pos) {
            this.eqExps = eqExps;
            this.pos = pos;
        }
    }

    public class EqExp {
        String rel;  //== | !=
        ArrayList<RelExp> relExps;
        int pos;

        public EqExp(String rel, ArrayList<RelExp> relExps, int pos) {
            this.rel = rel;
            this.relExps = relExps;
            this.pos = pos;
        }
    }

    public class RelExp {
        String rel;  //< | > | <= | >=
        ArrayList<Exp> addExps;
        int pos;

        public RelExp(String rel, ArrayList<Exp> addExps, int pos) {
            this.rel = rel;
            this.addExps = addExps;
            this.pos = pos;
        }
    }

    public class WhileStmt {
        Cond cond;
        Stmt stmt;
        int pos;

        public WhileStmt(Cond cond, Stmt stmt, int pos) {
            this.cond = cond;
            this.stmt = stmt;
            this.pos = pos;
        }
    }

    public class PrintfStmt {
        String formatString;
        ArrayList<Exp> exps;
        int pos;

        public PrintfStmt(String formatString, ArrayList<Exp> exps, int pos) {
            this.formatString = formatString;
            this.exps = exps;
            this.pos = pos;
        }
    }
}
