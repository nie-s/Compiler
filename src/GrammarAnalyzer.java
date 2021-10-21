import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrammarAnalyzer {
    LexicalAnalyzer lexicalAnalyzer;
    ExceptionHandler exceptionHandler;
    SymbolTableHandler symbolTableHandler;
    AbstractSyntaxTree ast;
    Word currentWord = new Word();
    int currentLine = 0;
    String currentFunc = "";
    String currentFuncType = "";
    boolean isDupFunc = false;
    int loopCnt = 0;
    int currentLayer = 0;
    int idCounter = 0;
    boolean output = true;
    BufferedWriter out;
    BufferedWriter error;

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
            error = new BufferedWriter(new FileWriter("error.txt"));
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
                    ERROR(5, currentLine, currentWord.getValue());
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

        if (!currentWord.isInt()) {
            ERROR(14, currentLine, currentWord.getValue());
        }
        GETWORD();
        ast.addChild(constDeclareId, constDefine());
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            ast.addChild(constDeclareId, constDefine());
        }
        checkSemi();
        PRINT("<ConstDecl>");

        return constDeclareId;
    }

    // <VarDecl> ::= <BType> <VarDef> {,<VarDef>} ';'
    public int varDeclare() {
        int varDeclareId = idCounter++;
        ast.addNode(varDeclareId, ast.new Decl(false, currentLine));
//        try {
        GETWORD();
        ast.addChild(varDeclareId, varDefine());
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            ast.addChild(varDeclareId, varDefine());
        }
        checkSemi();
        PRINT("<VarDecl>");
//        } catch (MyException e) {
//            System.out.println("=======" + e.errorLine + "======" + e.errorCode);
//        }
        return varDeclareId;
    }

    //<FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
    public int funcDef() throws MyException {
        int funcDefId = idCounter++;
        String funcType = currentWord.getType();
        currentFuncType = currentWord.value;
        PRINT("<FuncType>");
        GETWORD();
        CHECKIDENT();
        String ident = currentWord.getValue();
        currentFunc = ident;
        if (checkFunc(ident)) {
            ERROR('b', currentLine, ident);
            isDupFunc = true;
        } else {
            isDupFunc = false;
        }
        symbolTableHandler.addFunc(ident);
        AbstractSyntaxTree.Func func = ast.new Func(funcType, ident, currentLine);
        ast.addNode(funcDefId, func);
        ast.addFunc(ident, func);
        GETWORD();
        if (currentWord.isLparent()) {
            if (lexicalAnalyzer.checkRparent()) {
                GETWORD();
                GETWORD();
            } else if (lexicalAnalyzer.checkLbrace()) {
                ERROR('j', currentLine, currentWord.getValue());
                GETWORD();

            } else {
                GETWORD();
                currentLayer++;
                symbolTableHandler.createSymbolTable(currentLayer);
                ast.addChild(funcDefId, funcFParams());
                currentLayer--;
                checkRparent();
                GETWORD();
            }
        } else {
            ERROR(12, currentLine, currentWord.getValue());
        }
        ast.addChild(funcDefId, block());
        checkReturn(funcDefId);
        PRINT("<FuncDef>");
        return funcDefId;
    }

    //<MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
    public int mainFuncDef() throws MyException {
        int mainId = idCounter++;
        currentFuncType = "main";
        currentFunc = "main";
        symbolTableHandler.addFunc("main");
        ast.addNode(mainId, ast.new Func("int", "main", currentLine));
        GETWORD();
        CHECKLPARENT();
        checkRparent();
        GETWORD();
        ast.addChild(mainId, block());
        PRINT("<MainFuncDef>");
        checkReturn(mainId);
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
            ERROR(14, currentLine, currentWord.getValue());
        }
        GETWORD();
        CHECKIDENT();
        String ident = currentWord.getValue();
        if (checkDupDefine(ident)) ERROR('b', currentLine, currentWord.getValue());

        int dimension = 0;
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            checkRbrack();
            if (lexicalAnalyzer.checkLbrack()) {
                dimension++;
                GETWORD();
                GETWORD();
                ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, currentLine));
                ast.addChild(funcFParamId, constExp());
                checkRbrack();
            } else {
                ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, currentLine));
            }
        } else {
            ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, currentLine));
        }

        addFuncParam(ident, new SymbolTable.Symbol(ident, "int", 0, dimension));
        PRINT("<FuncFParam>");
        return funcFParamId;
    }

    //<Block> ::= '{' { <BlockItem> } '}'
    public int block() {
        int blockId = idCounter++;
        currentLayer++;
        ast.addNode(blockId, ast.new Block(currentLine));
        symbolTableHandler.createSymbolTable(currentLayer);
        CHECKLBRACE();
        GETWORD();
        while (!currentWord.isRbrace()) {
            ast.addChild(blockId, blockItem());
            GETWORD();
        }

        PRINT("<Block>");
        symbolTableHandler.deleteSymbolTable(currentLayer--);
        return blockId;
    }

    //<BlockItem> ::= <Decl> | <Stmt>
    public int blockItem() {
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
    public int stmt() {
        int stmtId = idCounter++;
        if (currentWord.isIf()) {
            int id = ifStatement();
            ast.addNode(stmtId, ast.new Stmt(4, id, currentLine));
        } else if (currentWord.isWhile()) {
            int id = whileStatement();
            ast.addNode(stmtId, ast.new Stmt(5, id, currentLine));
        } else if (currentWord.isBreak()) {
            if (loopCnt > 0) ast.addNode(stmtId, ast.new Stmt(6, currentLine));
            else ERROR('m', currentLine, "break");
            checkSemi();
        } else if (currentWord.isContinue()) {
            if (loopCnt > 0) ast.addNode(stmtId, ast.new Stmt(7, currentLine));
            else ERROR('m', currentLine, "continue");
            checkSemi();
        } else if (currentWord.isReturn()) {
            String t = lexicalAnalyzer.getByIndex(lexicalAnalyzer.index).value;
            lexicalAnalyzer.index--;
            if (t.equals("}")) {
                ERROR('i', currentLine, "");
            } else {

                GETWORD();
                int id = 0;
                if (!currentWord.isSemiColon()) {
                    if (currentFuncType.equals("void")) {
                        ERROR('f', currentLine, currentFunc);
                    }
                    id = exp();
                    checkSemi();
                }
                ast.addNode(stmtId, ast.new Stmt(8, id, currentLine));
            }
        } else if (currentWord.isPrintf()) {
            int printfId = printf();
            ast.addNode(stmtId, ast.new Stmt(10, printfId, currentLine));
        } else if (currentWord.isSemiColon()) {
            ast.addNode(stmtId, ast.new Stmt(2, 0, currentLine));
        } else if (currentWord.isLbrace()) {
            // <Block>
            ast.addNode(stmtId, ast.new Stmt(3, block(), currentLine));
            CHECKRBRACE();
        } else if (currentWord.isIdent()) {
            // <LVal> '=' <Exp> ';'
            //  [Exp] ';'
            //  <LVal> = 'getint();'
            int saveIndex = lexicalAnalyzer.index;

            output = false;
            lVal(0);
            GETWORD();
            if (!currentWord.isAssign()) {
                currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                output = true;
                ast.addNode(stmtId, ast.new Stmt(2, exp(), currentLine));
            } else {
                currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                int line = currentWord.lineCnt;
                output = true;
                int lValId = lVal(line);
                checkValConst(lValId, line);
                GETWORD(); // assign =
                GETWORD();
                if (!currentWord.isGetInt()) {
                    int expId = exp();
                    ast.addNode(stmtId, ast.new Stmt(1, lValId, expId, currentLine));
                } else {
                    ast.addNode(stmtId, ast.new Stmt(9, lValId, currentLine));
                    GETWORD();
                    CHECKLPARENT();
                    checkRparent();
                }
            }
            checkSemi();

        } else {
            ast.addNode(stmtId, ast.new Stmt(2, exp(), currentLine));
            checkSemi();
        }
        PRINT("<Stmt>");
        return stmtId;
    }

    public int printf() {
        int printfId = idCounter++;
        int printfLine = currentLine;
        int expCnt = 0;
        String formatString = "";
        //'printf('FormatString{,<Exp>}');'
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        if (!currentWord.isStrcon()) {
            ERROR(11, currentLine, currentWord.getValue());
        } else if (!currentWord.checkStrcon()) {
            ERROR('a', currentLine, currentWord.getValue());
        }
        formatString = currentWord.value;
        ast.addNode(printfId, ast.new PrintfStmt(currentWord.value, currentLine));
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            ast.addChild(printfId, exp());
            expCnt++;
        }
        checkFormat(formatString, expCnt, printfLine);
        checkRparent();
        checkSemi();
        return printfId;
    }

    // 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
    public int ifStatement() {
        int ifStatementId = idCounter;
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        int condId = condition();
        checkRparent();
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
    public int whileStatement() {
        int whileId = idCounter++;
        loopCnt++;
        ast.addNode(whileId, ast.new WhileStmt(currentLine));
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        ast.addChild(whileId, condition());
        checkRparent();
        GETWORD();
        ast.addChild(whileId, stmt());
        loopCnt--;
        return whileId;
    }

    public int condition() {
        int conditionId = idCounter++;
        ast.addNode(conditionId, ast.new Cond(currentLine));
        lOrExp(conditionId);
        PRINT("<Cond>");
        return conditionId;
    }

    //<LOrExp> ::= <LAndExp> | <LOrExp> '||' <LAndExp>
    // <LOrExp> ::= <LAndExp> {'||' <LAndExp>}
    public void lOrExp(int parent) {
        ast.addChild(parent, lAndExp());
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
    public int lAndExp() {
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
    public int eqExp() {
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
    public int relExp(String eq) {
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
    public int constDefine() {
        //<Ident>
        int constDefineId = idCounter++;
        CHECKIDENT();
        String ident = currentWord.getValue();
        int rangex = 0;
        int rangey = 0;
        boolean isDup = checkDupDefine(ident);
        if (isDup) ERROR('b', currentLine, ident);

        int dimension = 0;
        //{ '[' <ConstExp> ']' }
        GETWORD();
        if (currentWord.isLbrack()) {
            dimension++;
            GETWORD();
            rangex = constExp();
            checkRbrack();
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                rangey = constExp();
                checkRbrack();
                GETWORD();
            }
        }
        // '='
        if (!currentWord.isAssign()) {
            ERROR(10, currentLine, currentWord.getValue());
        }
        GETWORD();
        // <ConstInitVal>
        int constInitVal = constInitVal(dimension);

        if (!isDup) {
            ast.addNode(constDefineId, ast.new Def(true, ident, currentLine));
            ast.addChild(constDefineId, rangex);
            ast.addChild(constDefineId, rangey);
            ast.addChild(constDefineId, constInitVal);
            symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(ident, "const", constInitVal, dimension));
        }

        PRINT("<ConstDef>");

        return constDefineId;
    }

    // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'
    public int constInitVal(int dimension) {
        int constInitValId = idCounter++;
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        if (dimension == 0) {
            int constExpId = constExp();
            ArrayList<Integer> tmp = new ArrayList<>();
            tmp.add(constExpId);
            exps.add(tmp);
        } else if (dimension == 1) {
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
    public int varDefine() {
        int varDefineId = idCounter++;
        CHECKIDENT();
        String ident = currentWord.getValue();
        boolean isDup = checkDupDefine(ident);
        if (isDup) ERROR('b', currentLine, ident);

        ast.addNode(varDefineId, ast.new Def(false, ident, currentLine));
        int dimension = 0;
        int rangex = 0;
        int rangey = 0;
        //{ '[' <ConstExp> ']' }
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            GETWORD();
            rangex = constExp();
            checkRbrack();
            if (lexicalAnalyzer.checkLbrack()) {
                dimension++;
                GETWORD();
                GETWORD();
                rangey = constExp();
                checkRbrack();
            }
        }
        // '='
        int initValId = 0;
        if (lexicalAnalyzer.checkAssign()) {
            GETWORD();
            GETWORD();
            initValId = initVal(dimension);
        }
        if (!isDup) {
            ast.addNode(varDefineId, ast.new Def(false, ident, currentLine));
            ast.addChild(varDefineId, initValId);
            ast.addChild(varDefineId, rangex);
            ast.addChild(varDefineId, rangey);
            symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(ident, "int", initValId, dimension));
        }
        // <InitVal>
        PRINT("<VarDef>");
        return varDefineId;
    }

    public int initVal(int dimension) {
        int initValId = idCounter++;
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        if (dimension == 0) {
            int expId = exp();
            ArrayList<Integer> tmp = new ArrayList<>();
            tmp.add(initValId);
            exps.add(tmp);
        } else if (dimension == 1) {
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
                    expId = exp();
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
                    int expId = exp();
                    ArrayList<Integer> tmp = new ArrayList<>();
                    PRINT("<InitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD(); //comma
                        GETWORD();
                        expId = exp();
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
        PRINT("<InitVal>");
        ast.addNode(initValId, ast.new InitVal(false, exps, dimension, currentLine));
        return initValId;
    }

    public int constExp() {
        int addExpId = addExp("+", true);
        PRINT("<ConstExp>");
        return addExpId;
    }

    public int exp() {
        int addExpId = addExp("+", false);
        PRINT("<Exp>");
        return addExpId;
    }

    // <AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp>
    // <AddExp> ::= <MulExp> { (+|−) <MulExp> }
    public int addExp(String op, boolean isConst) {
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
    public int mulExp(String op) {
        int mulExpId = idCounter++;
        ast.addNode(mulExpId, ast.new MulExp(currentLine));
        ast.addChild(mulExpId, unaryExp(""));

        while (lexicalAnalyzer.checkUnaryCal()) {
            PRINT("<MulExp>");
            GETWORD();
            String cal = currentWord.getValue();
            GETWORD();
            ast.addChild(mulExpId, unaryExp(cal));
        }
        PRINT("<MulExp>");

        return mulExpId;
    }

    //<UnaryExp> ::= <PrimaryExp> | <Ident> '(' [<FuncRParams>] ')' | <UnaryOp> <UnaryExp>
    public int unaryExp(String op) {
        int unaryExpId = idCounter++;
        if (currentWord.isIdent() && lexicalAnalyzer.checkFuncParam()) {
            String ident = currentWord.getValue();

            SymbolTable symbolTable = symbolTableHandler.functions.get(ident);

            int funcLine = currentWord.lineCnt;
            if (!checkFunc(ident)) {
                ERROR('c', funcLine, ident);
            }
            GETWORD();
            if (lexicalAnalyzer.checkRbrack()) {
                ERROR('j', currentLine, currentWord.getValue());
            } else if (!lexicalAnalyzer.checkRparent()) {
                GETWORD();
                int funcRParamsId = funcRParams(ident, funcLine);
                ast.addNode(unaryExpId, ast.new UnaryExp("+", 3, funcRParamsId, currentLine));
                checkRparent();
            } else {
                checkFuncParamsCnt(ident, 0, null, funcLine);
                int identId = idCounter++;
                ast.addNode(identId, ast.new Ident(ident, currentLine));
                ast.addNode(unaryExpId, ast.new UnaryExp("+", 3, identId, currentLine));
                ast.addChild(unaryExpId, identId);
                checkRparent();

            }
        } else if (currentWord.isUnaryOp()) {
            unaryOp();
            String unaryOp = currentWord.getValue();
            GETWORD();
            int unaryId = unaryExp(unaryOp);
            ast.addNode(unaryExpId, ast.new UnaryExp(unaryOp, 4, unaryId, currentLine));
        } else {
            int primaryExpId = primaryExp();
            ast.addNode(unaryExpId, ast.new UnaryExp("+", 4, primaryExpId, currentLine));
        }
        PRINT("<UnaryExp>");


        return unaryExpId;
    }

    //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
    public int primaryExp() {
        int primaryExpId = idCounter++;
        int id;
        if (currentWord.isLparent()) {
            GETWORD();
            id = exp();
            checkRparent();
            ast.addNode(primaryExpId, ast.new PrimaryExp(1, id, currentLine));
        } else if (currentWord.isIdent()) {
            String ident = currentWord.getValue();
            id = lVal(currentWord.lineCnt);
            ast.addNode(primaryExpId, ast.new PrimaryExp(2, id, currentLine));
        } else if (currentWord.isNumber()) {
            id = Integer.parseInt(currentWord.getValue());
            ast.addNode(primaryExpId, ast.new PrimaryExp(3, id, currentLine));
            number();
        } else {
            ERROR(9, currentLine, currentWord.getValue());
        }
        PRINT("<PrimaryExp>");
        return primaryExpId;
    }

    // <LVal> ::= <Ident> {'[' <Exp> ']'}
    public int lVal(int line) {
        int lValId = idCounter++;
        CHECKIDENT();
        if (!checkDefine(currentWord.getValue())) {
            ERROR('c', line, currentWord.getValue());
        }
        String name = currentWord.value;
        int dimension = 0;
        int rangx = 0;
        int rangy = 0;
        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            rangx = exp();
            dimension++;
            checkRbrack();
        }
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            GETWORD();
            rangy = exp();
            checkRbrack();
        }
        ast.addNode(lValId, ast.new LVal(false, rangx, rangy, dimension, name, currentLine));
        PRINT("<LVal>");
        return lValId;
    }

    //<FuncRParams> → <Exp> { ',' <Exp> }
    public int funcRParams(String ident, int funcLine) {
        int funcRParamsId = idCounter++;
        int paracnt = 1;
        ArrayList<Integer> types = new ArrayList<>();

        ast.addNode(funcRParamsId, ast.new FuncR(ident, currentLine));
        types.add(funcRParam(funcRParamsId));

//        while (lexicalAnalyzer.checkComma() && paracnt < size) {
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            paracnt++;
            types.add(funcRParam(funcRParamsId));
        }
        checkFuncParamsCnt(ident, paracnt, types, funcLine);
        PRINT("<FuncRParams>");
        return funcRParamsId;
    }

    public int funcRParam(int funcRParamsId) {
        int saveIndex = lexicalAnalyzer.index;
        output = false;
        int lvalId = lVal(currentLine);
        int lValIndex = lexicalAnalyzer.index;
        currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
        output = true;
        String funcName = currentWord.getValue();
        if (lexicalAnalyzer.checkLparent()) {
            AbstractSyntaxTree.Func func = ast.getFuncByName(funcName);
            if (func.type.equals("VOIDTK")) {
                ERROR('e', currentLine, "void");
            }
        }   //TODO
        ast.addChild(funcRParamsId, exp());
        int expLVal = lexicalAnalyzer.index;

        if (lValIndex == expLVal) {
            String name = ((AbstractSyntaxTree.LVal) ast.getById(lvalId)).name;
            return symbolTableHandler.checkDimension(name, currentLayer) - checkDimension(lvalId);
        }

        return 0;
    }

    //<UnaryOp> ::= + | - | !
    public void unaryOp() {
        if (!currentWord.isMinus() && !currentWord.isPlus() && !currentWord.isNot()) {
            ERROR(5, currentLine, currentWord.getValue());
        }
        PRINT("<UnaryOp>");
    }

    public void number() {
        PRINT("<Number>");
    }

    public void ERROR(int errorCode, int errorLine, String errorMessage) {
        if (output) exceptionHandler.addError(new MyException(errorCode, errorLine, errorMessage));
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


    public void addFuncParam(String ident, SymbolTable.Symbol symbol) {
        symbolTableHandler.addToTable(currentLayer, symbol);
        if (!isDupFunc) symbolTableHandler.addFuncParam(currentFunc, symbol);
    }

    public void GETWORD() {
        if (!lexicalAnalyzer.hasWord()) {
            ERROR(2, currentLine, "");
        }
        currentWord = lexicalAnalyzer.getWord();
        currentLine = currentWord.lineCnt;
        PRINT(currentWord.type + " " + currentWord.value);
    }

    public void checkSemi() {
        if (lexicalAnalyzer.checkSemi()) {
            GETWORD();
        } else {
            ERROR('i', currentLine, "");
        }
    }

    public void CHECKRBRACE() {
        if (!currentWord.isRbrace()) {
            ERROR(6, currentLine, currentWord.getValue());
        }
    }

    public void CHECKIDENT() {
        if (!currentWord.isIdent()) {
            ERROR(1, currentLine, currentWord.getValue());
        }
    }

    public boolean checkDefine(String ident) {
        return symbolTableHandler.searchInTable(ident, currentLayer);
    }

    public boolean checkDupDefine(String ident) {
        return symbolTableHandler.searchInCurrentLayer(ident, currentLayer);
    }

    public void checkRparent() {
        if (lexicalAnalyzer.checkRparent()) {
            GETWORD();
        } else {
            ERROR('j', currentLine, "");
        }
    }

    public void CHECKLPARENT() {
        if (!currentWord.isLparent()) {
            ERROR(7, currentLine, currentWord.getValue());
        }
    }

    public void checkRbrack() {
        if (lexicalAnalyzer.checkRbrack()) {
            GETWORD();
        } else {
            ERROR('k', currentLine, "");
        }
    }

    public void CHECKLBRACE() {
        if (!currentWord.isLbrace()) {
            ERROR(3, currentLine, currentWord.getValue());
        }
    }

    public boolean checkFunc(String func) {
        return symbolTableHandler.searchFunc(func);
    }

    public void checkFuncParamsCnt(String ident, int cnt, ArrayList<Integer> types, int funcLine) {
        SymbolTable symbolTable = symbolTableHandler.functions.get(ident);
        if (symbolTable == null) return;
        if (symbolTable.symbols.size() != cnt) {
            ERROR('d', funcLine, ident + ", expected " + symbolTableHandler.functions.get(ident).symbols.size() +
                    " but got " + cnt);
        } else {
            ArrayList<SymbolTable.Symbol> symbols = symbolTableHandler.functions.get(ident).symbols;
            for (int i = 0; i < cnt; i++) {
                if (symbols.get(i).dimension != types.get(i)) {
                    ERROR('e', funcLine, ident + " at " + symbols.get(i).name + ", expected dimension "
                            + symbols.get(i).dimension + ", but got " + types.get(i));
                    return;
                }
            }
        }
    }

    public void checkValConst(int id, int line) {
        String name = ((AbstractSyntaxTree.LVal) ast.getById(id)).name;
        String type = symbolTableHandler.checkType(name, currentLayer);
        if (type.equals("const")) {
            ERROR('h', line, name);
        }
    }

    public void checkReturn(int id) {
        if (currentFuncType.equals("int") || currentFuncType.equals("main")) {
            int blockId = ast.getChild(id).get(ast.getChild(id).size() - 1);
            if (ast.getChild(blockId).size() == 0) {
                ERROR('g', currentLine, currentFuncType);
                return;
            }

            int stmtId = ast.getChild(blockId).get(ast.getChild(blockId).size() - 1);
            if (!(ast.getById(stmtId) instanceof AbstractSyntaxTree.Stmt)) {
                ERROR('g', currentLine, currentFuncType);
            } else {
                AbstractSyntaxTree.Stmt stmt = (AbstractSyntaxTree.Stmt) ast.getById(stmtId);
                if (stmt.type != 8) {
                    ERROR('g', currentLine, currentFuncType);
                }
            }

        }
    }

    public void checkFormat(String string, int cnt, int line) {
        int para = 0;
        Pattern p = Pattern.compile("%d");
        Matcher m = p.matcher(string);

        while (m.find()) {
            para++;
        }

        if (para != cnt) {
            ERROR('l', line, "got " + cnt + "but required " + para);
        }

    }

    public int checkDimension(int id) {
        AbstractSyntaxTree.LVal lVal = (AbstractSyntaxTree.LVal) ast.getById(id);
        return lVal.dimension;
    }

}
