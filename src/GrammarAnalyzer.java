import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class GrammarAnalyzer {
    LexicalAnalyzer lexicalAnalyzer;
    ExceptionHandler exceptionHandler;
    SymbolTableHandler symbolTableHandler;
    AbstractSyntaxTree ast;
    Word currentWord = new Word();
    int currentLine = 0;
    String currentFunc = "";
    int currentLayer = 0;
    int idCounter = 0;
    boolean output = true;
    BufferedWriter out;

    public GrammarAnalyzer(LexicalAnalyzer lexicalAnalyzer, ExceptionHandler exceptionHandler,
                           SymbolTableHandler symbolTableHandler, AbstractSyntaxTree ast) {
        this.lexicalAnalyzer = lexicalAnalyzer;
        this.exceptionHandler = exceptionHandler;
        this.symbolTableHandler = symbolTableHandler;
        this.ast = ast;
    }

    public void analyse() {
        try {
            out = new BufferedWriter(new FileWriter("output.txt"));
            Program();
            PRINT("<CompUnit>");
            out.close();
        } catch (MyException e) {
            ;
        } catch (IOException e) {
            //
        }
    }

    //<CompUnit> ::= {<Decl>} {<FuncDef>} <MainFuncDef>
    //<Decl> ::= <ConstDecl> | <VarDecl>
    public void Program() throws MyException {
        AbstractSyntaxTree.Program program = ast.new Program(currentLine);
        int programId = idCounter++;
        ast.addNode(programId, program);
        //{<Decl>}
        while (lexicalAnalyzer.hasWord()) {
            //<ConstDecl>
            GETWORD();
            if (currentWord.isConst()) {
                GETWORD();    //const
                ast.addChild(programId, constDeclare());
            }
            //<VarDecl>
            else if (!lexicalAnalyzer.checkFunc()) {
                ast.addChild(programId, varDeclare());
            } else {
                break;
            }

        }

        //<FuncDef>
        while (lexicalAnalyzer.hasWord() && (currentWord.isInt() || currentWord.isVoid())) {
            try {
                if (currentWord.isInt() && lexicalAnalyzer.checkMain()) {
                    break;
                } else if (currentWord.isInt() || currentWord.isVoid()) {
                    ast.addChild(programId, funcDef());
                } else {
                    ERROR(100, currentLine);
                }
                GETWORD();
            } catch (MyException e) {
                System.out.println("======" + e.errorLine);
            }

        }

        //<MainFuncDef>
        try {
            GETWORD();
            if (lexicalAnalyzer.hasWord()) {
                ast.addChild(programId, mainFuncDef());
            }
        } catch (MyException e) {
            System.out.println("=======" + e.errorLine + "======" + e.errorCode);
        }

    }

    public int constDeclare() {
        int constDeclareId = idCounter++;
        ast.addNode(constDeclareId, ast.new Decl(true, currentLine));
        try {
            if (!currentWord.isInt()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                ast.addChild(constDeclareId, constDefine());
                GETWORD();
            } while (currentWord.isComma());
            CHECKSEMI();
            PRINT("<ConstDecl>");
        } catch (MyException e) {
            //TODO error
        }


        return constDeclareId;
    }

    // <VarDecl> ::= <BType> <VarDef> {,<VarDef>} ';'
    public int varDeclare() {
        int varDeclareId = idCounter++;
        ast.addNode(varDeclareId, ast.new Decl(false, currentLine));
        try {
            do {
                GETWORD();
                ast.addChild(varDeclareId, varDefine());
                GETWORD();
            } while (currentWord.isComma());
            CHECKSEMI();
            PRINT("<VarDecl>");
        } catch (MyException e) {
            System.out.println("=======" + e.errorLine + "======" + e.errorCode);
        }
        return varDeclareId;
    }

    //<FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
    public int funcDef() throws MyException {
        int funcDefId = idCounter++;
        String funcType = currentWord.getType();
        PRINT("<FuncType>");
        GETWORD();
        CHECKIDENT();
        String ident = currentWord.getValue();
        ast.addNode(funcDefId, ast.new Func(funcType, ident, currentLine));
        GETWORD();
        if (currentWord.isLparent()) {
            if (lexicalAnalyzer.checkRparent()) {
                GETWORD();
                GETWORD();
            } else {
                GETWORD();
                ast.addChild(funcDefId, funcFParams());
                GETWORD();
                CHECKRPARENT();
                GETWORD();
            }
        } else {
            ERROR(100, currentLine);
        }
        ast.addChild(funcDefId, block());
        PRINT("<FuncDef>");
        return funcDefId;
    }

    //<MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
    public int mainFuncDef() throws MyException {
        int mainId = idCounter++;
        ast.addNode(mainId, ast.new Func("main", "main", currentLine));
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        ast.addChild(mainId, block());
        PRINT("<MainFuncDef>");
        return mainId;
    }

    //<FuncFParams> ::= <FuncFParam> { ',' <FuncFParam> }
    public int funcFParams() throws MyException {
        int funcFId = idCounter++;
        ast.addNode(funcFId, ast.new FuncFParams(currentLine));
        ast.addChild(funcFId, funcFParam());
        while (lexicalAnalyzer.checkComma()) {
            GETWORD(); //Comma
            GETWORD();
            ast.addChild(funcFId, funcFParam());
        }
        PRINT("<FuncFParams>");
        return funcFId;
    }

    //<FuncFParam> ::= <BType> <Ident> ['[' ']' { '[' <ConstExp>/ ']' }]
    public int funcFParam() throws MyException {
        int funcFParamId = idCounter++;
        if (!currentWord.isInt()) {
            ERROR(106, currentLine);
        }
        GETWORD();
        CHECKIDENT();
        String ident = currentWord.getValue();
        int dimension = 0;
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            GETWORD();
            CHECKRBRACK();
            if (lexicalAnalyzer.checkLbrack()) {
                dimension++;
                GETWORD();
                GETWORD();
                ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, currentLine));
                ast.addChild(funcFParamId, constExp());
                GETWORD();
                CHECKRBRACK();
            } else {
                ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, currentLine));
            }
        } else {
            ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, currentLine));
        }


        PRINT("<FuncFParam>");
        return funcFParamId;
    }

    //<Block> ::= '{' { <BlockItem> } '}'
    public int block() throws MyException {
        int blockId = idCounter++;
        ast.addNode(blockId, ast.new Block(currentLine));
        CHECKLBRACE();
        GETWORD();
        while (!currentWord.isRbrace()) {
            ast.addChild(blockId, blockItem());
            GETWORD();
        }

        PRINT("<Block>");
        return blockId;
    }

    //<BlockItem> ::= <Decl> | <Stmt>
    public int blockItem() throws MyException {
        if (currentWord.isConst()) {
            GETWORD();
            return constDeclare();
        } else if (currentWord.isInt()) {
            return varDeclare();
        } else {
            return stmt();
        }
    }

    /* <Stmt> ::= <LVal> '=' <Exp> ';'
                 | [Exp] ';' //有⽆Exp两种情况
                 | <Block>
                 | 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
                 | 'while' '(' <Cond> ')' <Stmt>
                 | 'break;' | 'continue;'
                 | 'return' [<Exp>] ';'
                 | <LVal> = 'getint();'
                 | 'printf('FormatString{,<Exp>}');'     */
    public int stmt() throws MyException {
        int stmtId = idCounter++;
        if (currentWord.isIf()) {
            int id = ifStatement();
            ast.addNode(stmtId, ast.new Stmt(4, id, currentLine));
        } else if (currentWord.isWhile()) {
            int id = whileStatement();
            ast.addNode(stmtId, ast.new Stmt(5, id, currentLine));
        } else if (currentWord.isBreak()) {
            ast.addNode(stmtId, ast.new Stmt(6, currentLine));
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isContinue()) {
            ast.addNode(stmtId, ast.new Stmt(7, currentLine));
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isReturn()) {
            GETWORD();
            int id = 0;
            if (!currentWord.isSemiColon()) {
                id = exp();
                GETWORD();
                CHECKSEMI();
            }
            ast.addNode(stmtId, ast.new Stmt(8, id, currentLine));
        } else if (currentWord.isPrintf()) {
            int printfId = idCounter++;
            //'printf('FormatString{,<Exp>}');'
            GETWORD();
            CHECKLPARENT();
            GETWORD();
            if (!currentWord.isStrcon()) {
                ERROR(100, currentLine);
            } else if (!currentWord.checkStrcon()) {
                ERROR(1, currentLine);
            }
            ast.addNode(printfId, ast.new PrintfStmt(currentWord.value, currentLine));
            GETWORD();
            while (currentWord.isComma()) {
                GETWORD();
                ast.addChild(printfId, exp());
                GETWORD();
            }
            CHECKRPARENT();
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isSemiColon()) {
            ast.addNode(stmtId, ast.new Stmt(2, 0, currentLine));
        } else if (currentWord.isLbrace()) {
            // <Block>
            ast.addNode(stmtId, ast.new Stmt(3, block(), currentLine ));
            CHECKRBRACE();
        } else if (currentWord.isIdent()) {
            // <LVal> '=' <Exp> ';'
            //  [Exp] ';'
            //  <LVal> = 'getint();'
            int saveIndex = lexicalAnalyzer.index;
            try {
                output = false;
                lVal();
                GETWORD();
                if (!currentWord.isAssign()) {
                    currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                    output = true;
                    ast.addNode(stmtId, ast.new Stmt(2, exp(), currentLine));
                } else {
                    currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                    output = true;
                    int lValId = lVal();
                    GETWORD(); // assign =
                    GETWORD();
                    if (!currentWord.isGetInt()) {
                        int expId = exp();
                        ast.addNode(stmtId, ast.new Stmt(1, lValId, expId, currentLine));
                    } else {
                        ast.addNode(stmtId, ast.new Stmt(9, lValId, currentLine));
                        GETWORD();
                        CHECKLPARENT();
                        GETWORD();
                        CHECKRPARENT();
                    }
                }
                GETWORD();
                CHECKSEMI();
            } catch (MyException e) {
                currentWord = lexicalAnalyzer.getByIndex(saveIndex);
                ast.addNode(stmtId, ast.new Stmt(2, exp(), currentLine));
            }

        } else {
            try {
                ast.addNode(stmtId, ast.new Stmt(2, exp(), currentLine));
                GETWORD();
                CHECKSEMI();
            } catch (MyException e) {
                ERROR(100, currentLine);
            }
        }
        PRINT("<Stmt>");
        return stmtId;
    }

    // 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
    public int ifStatement() throws MyException {
        int ifStatementId = idCounter;
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        int condId = condition();
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        int thenId = stmt();
        int elseId = 0;
        if (lexicalAnalyzer.checkElse()) {
            GETWORD();
            GETWORD();
            elseId = stmt();
        }
        ast.addNode(ifStatementId, ast.new IfStmt(condId, thenId, elseId, currentLine));
        return ifStatementId;
    }

    // 'while' '(' <Cond> ')' <Stmt>
    public int whileStatement() throws MyException {
        int whileId = idCounter++;
        ast.addNode(whileId, ast.new WhileStmt(currentLine));
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        ast.addChild(whileId, condition());
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        ast.addChild(whileId, stmt());
        return whileId;
    }

    public int condition() throws MyException {
        int conditionId = idCounter++;
        ast.addNode(conditionId, ast.new Cond(currentLine));
        lOrExp(conditionId);
        PRINT("<Cond>");
        return conditionId;
    }

    //<LOrExp> ::= <LAndExp> | <LOrExp> '||' <LAndExp>
    // <LOrExp> ::= <LAndExp> {'||' <LAndExp>}
    public void lOrExp(int parent) throws MyException {
        while (lexicalAnalyzer.checkOr()) {
            PRINT("<LOrExp>");
            GETWORD();
            GETWORD();
            ast.addChild(parent, lAndExp());
        }
        PRINT("<LOrExp>");
    }

    // <LAndExp> ::= <EqExp> | <LAndExp> && <EqExp>
    // <LAndExp> ::= <EqExp> { && <EqExp> }
    public int lAndExp() throws MyException {
        int lAndId = idCounter++;
        ast.addNode(lAndId, ast.new LAndExp(currentLine));
        ast.addChild(lAndId, eqExp());
        while (lexicalAnalyzer.checkAnd()) {
            PRINT("<LAndExp>");
            GETWORD();
            GETWORD();
            ast.addChild(lAndId, eqExp());
        }
        PRINT("<LAndExp>");
        return lAndId;
    }

    // <EqExp> ::= <RelExp> | <EqExp> (== | !=) <RelExp>
    // <EqExp> ::= <RelExp> { (== | !=) <RelExp> }
    public int eqExp() throws MyException {
        int eqExpId = idCounter++;
        ast.addNode(eqExpId, ast.new EqExp(currentLine));
        ast.addChild(eqExpId, relExp(""));
        while (lexicalAnalyzer.checkEq()) {
            PRINT("<EqExp>");
            String eq = currentWord.getValue();
            GETWORD();
            GETWORD();
            ast.addChild(eqExpId, relExp(eq));
        }

        PRINT("<EqExp>");
        return eqExpId;
    }

    // <RelExp> ::= <AddExp> | <RelExp> (< | > | <= | >=) <AddExp>
    // <RelExp> ::= <AddExp>  { (< | > | <= | >=) <AddExp> }
    public int relExp(String eq) throws MyException {
        int relExpId = idCounter++;
        ast.addNode(relExpId, ast.new RelExp(eq, currentLine));
        ast.addChild(relExpId, addExp("", false));
        while (lexicalAnalyzer.checkRel()) {
            PRINT("<RelExp>");
            String op = currentWord.getValue();
            GETWORD();
            GETWORD();
            ast.addChild(relExpId, addExp(op, false));
        }
        PRINT("<RelExp>");
        return relExpId;
    }

    // <ConstDef> ::= <Ident> { '[' <ConstExp> ']' } '=' <ConstInitVal>
    public int constDefine() throws MyException {
        //<Ident>
        int constDefineId = idCounter++;
        CHECKIDENT();
        String ident = currentWord.getValue();
        ast.addNode(constDefineId, ast.new Def(true, ident, currentLine));

        int dimension = 0;
        //{ '[' <ConstExp> ']' }
        GETWORD();
        if (currentWord.isLbrack()) {
            dimension++;
            GETWORD();
            ast.addChild(constDefineId, constExp());
            GETWORD();
            CHECKRBRACK();
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                ast.addChild(constDefineId, constExp());
                GETWORD();
                CHECKRBRACK();
                GETWORD();
            }
        }
        // '='
        if (!currentWord.isAssign()) {
            ERROR(100, currentLine);
        }
        GETWORD();
        // <ConstInitVal>
        ast.addChild(constDefineId, constInitVal(dimension));
        AbstractSyntaxTree.Def def;
        PRINT("<ConstDef>");

        return constDefineId;
    }

    // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'
    public int constInitVal(int dimension) throws MyException {
        int constInitValId = idCounter++;
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        if (dimension == 0) {
            int constExpId = constExp();
            ArrayList<Integer> tmp = new ArrayList<>();
            tmp.add(constExpId);
            exps.add(tmp);
        } else if (dimension == 1) {                //TODO calculate const value
            CHECKLBRACE();
            GETWORD();
            if (!currentWord.isRbrace()) {
                int constExpId = constExp();
                ArrayList<Integer> tmp = new ArrayList<>();
                tmp.add(constExpId);
                PRINT("<ConstInitVal>");
                while (lexicalAnalyzer.checkComma()) {
                    GETWORD(); //comma
                    GETWORD();
                    constExpId = constExp();
                    tmp.add(constExpId);
                    PRINT("<ConstInitVal>");
                }
                GETWORD();
            }
            CHECKRBRACE();
        } else {
            CHECKLBRACE();
            do {
                GETWORD();
                CHECKLBRACE();
                GETWORD();
                if (!currentWord.isRbrace()) {
                    int constExpId = constExp();
                    ArrayList<Integer> tmp = new ArrayList<>();
                    PRINT("<ConstInitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD(); //comma
                        GETWORD();
                        constExpId = constExp();
                        tmp.add(constExpId);
                        PRINT("<ConstInitVal>");
                    }
                    exps.add(tmp);
                    GETWORD();
                }
                CHECKRBRACE();
                PRINT("<ConstInitVal>");
                GETWORD();
            } while (currentWord.isComma());
            CHECKRBRACE();
        }

        ast.addNode(constInitValId, ast.new InitVal(true, exps, dimension, currentLine));
        PRINT("<ConstInitVal>");
        return constInitValId;
    }

    //<Ident> | <Ident> { '[' <ConstExp> ']' } '=' <InitVal>
    //<Ident>
    public int varDefine() throws MyException {
        int varDefineId = idCounter++;
        ast.addNode(varDefineId, ast.new Def(true, currentWord.getValue(), currentLine));
        CHECKIDENT();
        int dimension = 0;
        //{ '[' <ConstExp> ']' }
        GETWORD();
        if (currentWord.isLbrack()) {
            dimension++;
            GETWORD();
            ast.addChild(varDefineId, constExp());
            GETWORD();
            CHECKRBRACK();
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                ast.addChild(varDefineId, constExp());
                GETWORD();
                CHECKRBRACK();
                GETWORD();
            }
        }
        // '='
        AbstractSyntaxTree.Def def;
        int initValId = 0;
        if (lexicalAnalyzer.checkAssign()) {
            GETWORD();
            GETWORD();
            ast.addChild(varDefineId, initVal(dimension));
        }
        // <InitVal>
        PRINT("<VarDef>");
        return varDefineId;
    }

    public int initVal(int dimension) throws MyException {
        int initValId = idCounter++;
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        if (dimension == 0) {
            int expId = exp();
            ArrayList<Integer> tmp = new ArrayList<>();
            tmp.add(initValId);
            exps.add(tmp);
        } else if (dimension == 1) {                //TODO calculate const value
            CHECKLBRACE();
            GETWORD();
            if (!currentWord.isRbrace()) {
                int expId = exp();
                ArrayList<Integer> tmp = new ArrayList<>();
                tmp.add(initValId);
                PRINT("<InitVal>");
                while (lexicalAnalyzer.checkComma()) {
                    GETWORD(); //comma
                    GETWORD();
                    expId = constExp();
                    tmp.add(expId);
                    PRINT("<InitVal>");
                }
                GETWORD();
            }
            CHECKRBRACE();
        } else {
            CHECKLBRACE();
            do {
                GETWORD();
                CHECKLBRACE();
                GETWORD();
                if (!currentWord.isRbrace()) {
                    int expId = constExp();
                    ArrayList<Integer> tmp = new ArrayList<>();
                    PRINT("<InitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD(); //comma
                        GETWORD();
                        expId = constExp();
                        tmp.add(expId);
                        PRINT("<InitVal>");
                    }
                    exps.add(tmp);
                    GETWORD();
                }
                CHECKRBRACE();
                PRINT("<InitVal>");
                GETWORD();
            } while (currentWord.isComma());
            CHECKRBRACE();
        }

        ast.addNode(initValId, ast.new InitVal(false, exps, dimension, currentLine));
        return initValId;
    }

    public int constExp() throws MyException {  //TODO check const
        int addExpId = addExp("+", true);
        PRINT("<ConstExp>");
        return addExpId;
    }

    public int exp() throws MyException {
        int addExpId = addExp("+", false);
        PRINT("<Exp>");
        return addExpId;
    }

    // <AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp>
    // <AddExp> ::= <MulExp> { (+|−) <MulExp> }
    public int addExp(String op, boolean isConst) throws MyException {
        int addExpId = idCounter++;
        ast.addNode(addExpId, ast.new Exp("", isConst, currentLine));

        int mulExpId = mulExp("+");
        ast.addChild(addExpId, mulExpId);
        while (lexicalAnalyzer.checkUnaryAdd()) {
            PRINT("<AddExp>");
            GETWORD();
            String unaryAdd = currentWord.value;
            GETWORD();
            mulExpId = mulExp(unaryAdd);
            ast.addChild(addExpId, mulExpId);
        }

        PRINT("<AddExp>");
        return addExpId;
    }

    // <MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp>
    // <MulExp> ::= <UnaryExp> {  (*|/|%) <UnaryExp>  }
    public int mulExp(String op) throws MyException {
        int mulExpId = idCounter++;
        ast.addNode(mulExpId, ast.new MulExp(currentLine));
        ast.addChild(mulExpId, unaryExp(""));

        while (lexicalAnalyzer.checkUnaryCal()) {
            PRINT("<MulExp>");
            GETWORD();
            String cal = currentWord.getValue();
            ast.addChild(mulExpId, unaryExp(cal));
            GETWORD();
        }
        PRINT("<MulExp>");

        return mulExpId;
    }

    //<UnaryExp> ::= <PrimaryExp> | <Ident> '(' [<FuncRParams>] ')' | <UnaryOp> <UnaryExp>
    public int unaryExp(String op) throws MyException {
        int unaryExpId = idCounter++;
        if (currentWord.isIdent() && lexicalAnalyzer.checkFuncParam()) {
            String ident = currentWord.getValue();
            GETWORD();
            if (!lexicalAnalyzer.checkRparent()) {
                GETWORD();
                int funcRParamsId = funcRParams(ident);
                ast.addNode(unaryExpId, ast.new UnaryExp("+", 3, funcRParamsId, currentLine));
            } else {
                int identId = idCounter++;
                ast.addNode(identId, ast.new Ident(ident, currentLine));
                ast.addNode(unaryExpId, ast.new UnaryExp("+", 3, identId, currentLine));
                ast.addChild(unaryExpId, identId);
            }
            GETWORD();
            CHECKRPARENT();
        } else if (currentWord.isUnaryOp()) {
            unaryOp();
            String unaryOp = currentWord.getValue();
            GETWORD();
            int unaryId = unaryExp(unaryOp);
            ast.addNode(unaryExpId, ast.new UnaryExp(unaryOp, 4, unaryId, currentLine));
        } else {
            int primaryExpId = primaryExp();
        }
        PRINT("<UnaryExp>");


        return unaryExpId;
    }

    //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
    public int primaryExp() throws MyException {
        int primaryExpId = idCounter++;
        int id;
        if (currentWord.isLparent()) {
            GETWORD();
            id = exp();
            GETWORD();
            CHECKRPARENT();
            ast.addNode(primaryExpId, ast.new PrimaryExp(1, id, currentLine));
        } else if (currentWord.isIdent()) {
            id = lVal();
            ast.addNode(primaryExpId, ast.new PrimaryExp(2, id, currentLine));
        } else if (currentWord.isNumber()) {
            id = Integer.parseInt(currentWord.getValue());
            ast.addNode(primaryExpId, ast.new PrimaryExp(3, id, currentLine));
            number();
        } else {
            ERROR(100, currentLine);
        }
        PRINT("<PrimaryExp>");
        return primaryExpId;
    }

    // <LVal> ::= <Ident> {'[' <Exp> ']'}
    public int lVal() throws MyException {
        int lValId = idCounter++;
        CHECKIDENT();
        String name = currentWord.value;
        int rangx = 0;
        int rangy = 0;
        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            rangx = exp();
            GETWORD();
            if (!currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
        }
        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            rangy = exp();
            GETWORD();
            if (!currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
        }
        ast.addNode(lValId, ast.new LVal(false, rangx, rangy, name, currentLine));
        PRINT("<LVal>");
        return lValId;
    }

    //<FuncRParams> → <Exp> { ',' <Exp> }
    public int funcRParams(String ident) throws MyException {
        int funcRParamsId = idCounter++;
        ast.addNode(funcRParamsId, ast.new FuncR(ident, currentLine));
        ast.addChild(funcRParamsId, exp());
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            ast.addChild(funcRParamsId, exp());
        }
        PRINT("<FuncRParams>");
        return funcRParamsId;
    }

    //<UnaryOp> ::= + | - | !
    public void unaryOp() throws MyException {
        if (!currentWord.isMinus() && !currentWord.isPlus() && !currentWord.isNot()) {
            ERROR(100, currentLine);
        }
        PRINT("<UnaryOp>");
    }

    public void number() {
        PRINT("<Number>");
    }

    public void ERROR(int errorCode, int errorLine) throws MyException {
        exceptionHandler.output(new MyException(errorCode, errorLine));
    }

    public void ERROR(int errorCode, int errorLine, String errorMessage) throws MyException {
        exceptionHandler.output(new MyException(errorCode, errorLine, errorMessage));
    }

    public void PRINT(String str) {
        if (output) {
            try {
                System.out.println(str);
                str = str.concat("\n");
                out.write(str);
            } catch (IOException e) {
                //
            }
        }
    }

    public void GETWORD() throws MyException {
        if (!lexicalAnalyzer.hasWord()) {
            ERROR(99, currentLine);
        }
        currentWord = lexicalAnalyzer.getWord();
        currentLine = currentWord.lineCnt;
        PRINT(currentWord.type + " " + currentWord.value);
    }

    public void CHECKSEMI() throws MyException {
        if (!currentWord.isSemiColon()) {
            ERROR(101, currentLine);
        }
    }

    public void CHECKRBRACE() throws MyException {
        if (!currentWord.isRbrace()) {
            ERROR(102, currentLine);
        }
    }

    public void CHECKIDENT() throws MyException {
        if (!currentWord.isIdent()) {
            ERROR(1, currentLine);
        }
    }

    public void CHECKIDENT(String currentFunc) throws MyException {
        if (!currentWord.isIdent()) {
            ERROR(1, currentLine, currentWord.type);
        } else if (symbolTableHandler.searchInTable(currentWord.value, currentLayer)) {
            ERROR((int) 'b', currentLine, currentWord.value);
        }

    }

    public void CHECKRPARENT() throws MyException {
        if (!currentWord.isRparent()) {
            ERROR(104, currentLine);
        }
    }

    public void CHECKLPARENT() throws MyException {
        if (!currentWord.isLparent()) {
            ERROR(105, currentLine);
        }
    }

    public void CHECKRBRACK() throws MyException {
        if (!currentWord.isRbrack()) {
            ERROR(107, currentLine);
        }
    }

    public void CHECKLBRACE() throws MyException {
        if (!currentWord.isLbrace()) {
            ERROR(100, currentLine);
        }
    }

}
