import java.util.ArrayList;
import java.util.HashMap;

public class AbstractSyntaxTree {

    HashMap<Integer, ArrayList<Integer>> ast = new HashMap<>();
    HashMap<Integer, SyntaxNode> map = new HashMap<>();
    HashMap<String, Func> funcs = new HashMap<>();


    public void addNode(int id, SyntaxNode node) {
        this.ast.put(id, new ArrayList<Integer>());
        this.map.put(id, node);
    }

//    public void addNode(int id, ArrayList<Integer> childs, SyntaxNode node) {
//        this.ast.put(id, childs);
//        this.map.put(id, node);
//    }

    public void addChild(int parent, int child) {
        this.ast.get(parent).add(child);
    }


    public ArrayList<Integer> getChild(int id) {
        return ast.get(id);
    }

    public class SyntaxNode {
        int pos;

        public SyntaxNode(int pos) {
            this.pos = pos;
        }
    }

    public class Program extends SyntaxNode {
        public Program(int pos) {
            super(pos);
        }
    }

    public class Decl extends SyntaxNode {
        boolean isConst;

        public Decl(boolean isConst, int pos) {
            super(pos);
            this.isConst = isConst;
        }
    }

    public class Def extends SyntaxNode {
        boolean isConst;
        String name = "";

        public Def(boolean isConst, String name, int pos) {
            super(pos);
            this.isConst = isConst;
            this.name = name;
        }


    }

    public class InitVal extends SyntaxNode {
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        int dimension;
        boolean isConst;

        public InitVal(boolean isConst, ArrayList<ArrayList<Integer>> exps, int dimension, int pos) {
            super(pos);
            this.exps = exps;
            this.isConst = isConst;
            this.dimension = dimension;
        }
    }

    public class Exp extends SyntaxNode {
        //<AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp>
        boolean isConst;
        String op;

        public Exp(String op, boolean isConst, int pos) {
            super(pos);
            this.op = op;
            this.isConst = isConst;
        }
    }

    public class MulExp extends SyntaxNode {
        //<MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp>
        public MulExp(int pos) {
            super(pos);
        }
    }

    public class UnaryExp extends SyntaxNode {
        //<UnaryExp> ::= <PrimaryExp> | 1
        //               <Ident> '(' [<FuncRParams>] ')' | 2/3
        //               <UnaryOp> <UnaryExp> 4
        int type;
        int exp;
        UnaryOp unaryOp;

        public UnaryExp(String op, int type, int exp, int pos) {
            super(pos);
            this.type = 3;
            this.exp = exp;
            switch (op) {
                case "+":
                    this.unaryOp = UnaryOp.PLUS;
                    break;
                case "-":
                    this.unaryOp = UnaryOp.MINUS;
                    break;
                case "!":
                    this.unaryOp = UnaryOp.NOT;
                    break;
            }
        }
    }


    public class FuncR extends SyntaxNode {
        String name;

        public FuncR(String name, int pos) {
            super(pos);
            this.name = name;
        }
    }

    public enum UnaryOp {
        PLUS, MINUS, NOT
    }

    public class PrimaryExp extends SyntaxNode {
        //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
        int type;
        int exp;
        int lVal;
        int number;

        public PrimaryExp(int type, int num, int pos) {
            super(pos);
            this.type = type;
            switch (type) {
                case 1:
                    this.exp = num;
                    break;
                case 2:
                    this.lVal = num;
                    break;
                case 3:
                    this.number = num;
                    break;
            }
        }
    }

    public class LVal extends SyntaxNode {
        int rangex;
        int rangey;
        int dimension = 0;
        String name = "";

        public LVal(boolean isConst, int rangex, int rangey, int dimension, String name, int pos) {
            super(pos);
            this.rangex = rangex;
            this.rangey = rangey;
            this.dimension = dimension;
            this.name = name;
        }

    }


    public class Func extends SyntaxNode {
        String type = "";
        String name = "";

        public Func(String type, String name, int pos) {
            super(pos);
            this.type = type;
            this.name = name;
        }

    }

    public class FuncFParams extends SyntaxNode {

        public FuncFParams(int pos) {
            super(pos);
        }
    }

    public class FuncFParam extends SyntaxNode {
        String name = "";
        int dimension;

        public FuncFParam(String name, int dimension, int pos) {
            super(pos);
            this.name = name;
            this.dimension = dimension;
        }

    }

    public class Stmt extends SyntaxNode {
        //   1     <Stmt> ::= <LVal> '=' <Exp> ';'
        //   2             | [Exp] ';' //有无⽆Exp两种情况
        //   3             | <Block>
        //   4             | 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
        //   5             | 'while' '(' <Cond> ')' <Stmt>
        //   6             | 'break;'
        //   7             | 'continue;'
        //   8             | 'return' [<Exp>] ';'
        //   9             | <LVal> = 'getint();'
        //   10            | 'printf('FormatString{,<Exp>}');'
        int type;
        int lVal;
        int exp;
        int block;
        int ifStmt;
        int whileStmt;
        int printfStmt;


        public Stmt(int type, int pos) {
            super(pos);
            this.type = type;
        }

        public Stmt(int type, int num, int pos) {
            super(pos);
            this.type = type;
            switch (type) {
                case 2:
                    this.exp = num;
                    break;
                case 8:
                    this.exp = num;
                    break;
                case 3:
                    this.block = num;
                    break;
                case 4:
                    this.ifStmt = num;
                    break;
                case 5:
                    this.whileStmt = num;
                    break;
                case 9:
                    this.lVal = num;
                    break;
                case 10:
                    this.printfStmt = num;
                    break;
            }
        }

        public Stmt(int type, int numa, int numb, int pos) {
            super(pos);
            this.type = type;
            this.lVal = numa;
            this.exp = numb;
        }

    }

    public class IfStmt extends SyntaxNode {
        int cond;
        int thenStmt;
        int elseStmt;

        public IfStmt(int cond, int thenStmt, int elseStmt, int pos) {
            super(pos);
            this.cond = cond;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

    }

    public class Block extends SyntaxNode {
        public Block(int pos) {
            super(pos);
        }
    }

    public class Cond extends SyntaxNode {

        public Cond(int pos) {
            super(pos);
        }
    }

    public class LAndExp extends SyntaxNode {

        public LAndExp(int pos) {
            super(pos);
        }
    }

    public class EqExp extends SyntaxNode {

        public EqExp(int pos) {
            super(pos);
        }
    }

    public class RelExp extends SyntaxNode {
        String rel;  //< | > | <= | >=

        public RelExp(String rel, int pos) {
            super(pos);
            this.rel = rel;
        }
    }

    public class WhileStmt extends SyntaxNode {

        public WhileStmt(int pos) {
            super(pos);
        }
    }

    public class PrintfStmt extends SyntaxNode {
        String formatString;

        public PrintfStmt(String formatString, int pos) {
            super(pos);
            this.formatString = formatString;
        }
    }

    public class Ident extends SyntaxNode {
        String name;

        public Ident(String name, int pos) {
            super(pos);
            this.name = name;
        }
    }

    public SyntaxNode getById(int id) {
        return map.get(id);
    }

    public void addFunc(String name, Func func) {
        this.funcs.put(name, func);
    }

    public Func getFuncByName(String name) {
        return funcs.get(name);
    }
}
