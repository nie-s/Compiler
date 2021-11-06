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
//        this.ast.get(parent).add(child);
    }


    public ArrayList<Integer> getChild(int id) {
        return ast.get(id);
    }

    public class SyntaxNode {
        int pos;
        int value;

        public SyntaxNode(int pos, int value) {
            this.pos = pos;
            this.value = value;
        }

        public String toString() {
            return this.getClass().toString() + "---" + this.pos;
        }
    }

    public class Program extends SyntaxNode {
        public Program(int pos) {
            super(pos, 0);
        }
    }

    public class Decl extends SyntaxNode {
        boolean isConst;

        public Decl(boolean isConst, int pos) {
            super(pos, 0);
            this.isConst = isConst;
        }
    }

    public class Def extends SyntaxNode {
        boolean isConst;
        String name = "";
        int rangex;
        int rangey;

        public Def(boolean isConst, String name, int rangex, int rangey, int pos) {
            super(pos, 0);
            this.isConst = isConst;
            this.name = name;
            this.rangex = rangex;
            this.rangey = rangey;
        }


    }

    public class InitVal extends SyntaxNode {
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        int dimension;
        boolean isConst;

        public InitVal(boolean isConst, ArrayList<ArrayList<Integer>> exps, int dimension, int pos) {
            super(pos, 0);
            this.exps = exps;
            this.isConst = isConst;
            this.dimension = dimension;
        }
    }

    public class Exp extends SyntaxNode {
        //<AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp>
        boolean isConst;
        String op;


        public Exp(String op, boolean isConst, int value, int pos) {
            super(pos, value);
            this.op = op;
            this.isConst = isConst;
        }
    }

    public class MulExp extends SyntaxNode {
        //<MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp>
        String op;


        public MulExp(String op, int value, int pos) {
            super(pos, value);
            this.op = op;
        }
    }

    public class UnaryExp extends SyntaxNode {
        //<UnaryExp> ::= <PrimaryExp> | 1
        //               <Ident> '(' [<FuncRParams>] ')' | 2/3
        //               <UnaryOp> <UnaryExp> 4
        int type;
        UnaryOp unaryOp;

        public UnaryExp(String op, boolean isConst, int type, int value, int pos) {
            super(pos, value);
            this.type = 3;
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
            super(pos, 0);
            this.name = name;
        }
    }

    public enum UnaryOp {
        PLUS, MINUS, NOT
    }

    public class PrimaryExp extends SyntaxNode {
        //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
        int type;
        int number;


        public PrimaryExp(int type, boolean isConst, int value, int pos) {
            super(pos, value);

            this.type = type;
        }
    }

    public class LVal extends SyntaxNode {
        boolean isConst;
        int rangex;
        int rangey;
        int dimension = 0;
        String name = "";


        public LVal(boolean isConst, int rangex, int rangey, int dimension, String name, int value, int pos) {
            super(pos, value);
            this.isConst = isConst;

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
            super(pos, 0);
            this.type = type;
            this.name = name;
        }

    }

    public class FuncFParams extends SyntaxNode {

        public FuncFParams(int pos) {
            super(pos, 0);
        }
    }

    public class FuncFParam extends SyntaxNode {
        String name = "";
        int dimension;


        public FuncFParam(String name, int dimension, int value, int pos) {
            super(pos,0);
            this.name = name;
            this.dimension = dimension;

        }

    }

    public class Stmt extends SyntaxNode {
        //   1     <Stmt> ::= <LVal> '=' <Exp> ';'
        //   2      11       | [Exp] ';' //有无⽆Exp两种情况
        //   3             | <Block>
        //   4             | 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
        //   5             | 'while' '(' <Cond> ')' <Stmt>
        //   6             | 'break;'
        //   7             | 'continue;'
        //   8             | 'return' [<Exp>] ';'
        //   9             | <LVal> = 'getint();'
        //   10            | 'printf('FormatString{,<Exp>}');'
        int type;

        public Stmt(int type, int pos) {
            super(pos,0);
            this.type = type;
        }
    }

    public class Continue extends SyntaxNode {

        public Continue(int pos) {
            super(pos,0);
        }
    }

    public class Break extends SyntaxNode {

        public Break(int pos) {
            super(pos,0);
        }
    }

    public class IfStmt extends SyntaxNode {
        int cond;
        int thenStmt;
        int elseStmt;

        public IfStmt(int cond, int thenStmt, int elseStmt, int pos) {
            super(pos,0);
            this.cond = cond;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

    }

    public class Block extends SyntaxNode {
        public Block(int pos) {
            super(pos,0);
        }
    }

    public class Cond extends SyntaxNode {

        public Cond(int pos) {
            super(pos,0);
        }
    }

    public class LAndExp extends SyntaxNode {

        public LAndExp(int pos) {
            super(pos,0);
        }
    }

    public class EqExp extends SyntaxNode {

        public EqExp(int pos) {
            super(pos,0);
        }
    }

    public class RelExp extends SyntaxNode {
        String rel;  //< | > | <= | >=

        public RelExp(String rel, int pos) {
            super(pos,0);
            this.rel = rel;
        }
    }

    public class WhileStmt extends SyntaxNode {

        public WhileStmt(int pos) {
            super(pos,0);
        }
    }

    public class PrintfStmt extends SyntaxNode {
        String formatString;

        public PrintfStmt(String formatString, int pos) {
            super(pos,0);
            this.formatString = formatString;
        }
    }

    public class Ident extends SyntaxNode {
        String name;

        public Ident(String name, int pos) {
            super(pos,0);
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
