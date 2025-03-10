import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrammarAnalyzer {
    LexicalAnalyzer lexicalAnalyzer;
    ExceptionHandler exceptionHandler;
    SymbolTableHandler symbolTableHandler;
    SemanticAnalyzer semanticAnalyzer;
    AbstractSyntaxTree ast;
    Word currentWord = new Word();
    int currentLine = 0;
    String currentFunc = "";
    String currentFuncType = "";
    boolean isDupFunc = false;
    int loopCnt = 0;
    int currentLayer = 0;
    int idCounter = 0;
    int labelCnt = 0;
    int currentWhile = 0;
    int condCnt = 0;
    int start = 0;
    int end = 0;
    String shift_lval = "";
    boolean isWhileEnd = false;
    boolean output = true;
    BufferedWriter out;
    BufferedWriter error;

    Stack<Quadruple> params = new Stack<>();

    public GrammarAnalyzer(LexicalAnalyzer lexicalAnalyzer, ExceptionHandler exceptionHandler,
                           SymbolTableHandler symbolTableHandler, AbstractSyntaxTree ast, SemanticAnalyzer semanticAnalyzer) {
        this.lexicalAnalyzer = lexicalAnalyzer;
        this.exceptionHandler = exceptionHandler;
        this.symbolTableHandler = symbolTableHandler;
        this.ast = ast;
        this.semanticAnalyzer = semanticAnalyzer;
    }

    public void analyse() {
        try {
            out = new BufferedWriter(new FileWriter("output.txt"));
            error = new BufferedWriter(new FileWriter("error.txt"));
            Program();
            PRINT("<CompUnit>");
            PRINT("<CompUnit>");
            out.close();
        } catch (MyException e) {

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
        semanticAnalyzer.label("GLOBAL:");
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

        semanticAnalyzer.label("GLOBAL_END:");
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

        semanticAnalyzer.funcDef(currentFuncType, currentFunc);
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
        if (!checkVoidReturn(funcDefId)) {
            semanticAnalyzer.jr();
        }

        semanticAnalyzer.funcEnd();


        PRINT("<FuncDef>");
        return funcDefId;
    }

    //<MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
    public int mainFuncDef() throws MyException {
        int mainId = idCounter++;
        currentFuncType = "main";
        currentFunc = "main";
        symbolTableHandler.addFunc("main");
        semanticAnalyzer.mainDef();
        ast.addNode(mainId, ast.new Func("int", "main", currentLine));
        GETWORD();
        CHECKLPARENT();
        checkRparent();
        GETWORD();
        ast.addChild(mainId, block());
        semanticAnalyzer.funcEnd();

        PRINT("<MainFuncDef>");
        checkReturn(mainId);
        return mainId;

    }

    //<FuncFParams> ::= <FuncFParam> { ',' <FuncFParam> }
    public int funcFParams() {
        int funcFId = idCounter++;
        params.clear();

        ast.addNode(funcFId, ast.new FuncFParams(currentLine));
        ast.addChild(funcFId, funcFParam());
        while (lexicalAnalyzer.checkComma()) {
            GETWORD(); //Comma
            GETWORD();
            ast.addChild(funcFId, funcFParam());
        }

        while (!params.isEmpty()) {   //recover
            semanticAnalyzer.quadruples.add(params.pop());
        }

        PRINT("<FuncFParams>");
        return funcFId;
    }

    //<FuncFParam> ::= <BType> <Ident> ['[' ']' { '[' <ConstExp>/ ']' }]
    public int funcFParam() {
        int funcFParamId = idCounter++;
        if (!currentWord.isInt()) {
            ERROR(14, currentLine, currentWord.getValue());
        }
        GETWORD();
        CHECKIDENT();
        String ident = currentWord.getValue();
        if (checkDupDefine(ident)) ERROR('b', currentLine, currentWord.getValue());
        int value = 0;
        int dimension = 0;
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            checkRbrack();
            if (lexicalAnalyzer.checkLbrack()) {
                dimension++;
                GETWORD();
                GETWORD();
                int constExp = constExp();
                value = (ast.map.get(constExp)).value;
                ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, value
                        , currentLine));
                ast.addChild(funcFParamId, constExp);
                checkRbrack();
            } else {
                ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, 0, currentLine));
            }
        } else {
            ast.addNode(funcFParamId, ast.new FuncFParam(ident, dimension, 0, currentLine));
        }

        if (dimension == 0) {
            addFuncParam(ident, new SymbolTable.Symbol(ident, "int", false, 0));
            params.push(new Quadruple("RECOVER", ident + ".1", "0", "0"));
        } else if (dimension == 1) {
            addFuncParam(ident, new SymbolTable.Symbol(ident, "int", false, null, 0));
            params.push(new Quadruple("RECOVER", ident + ".1", "1", "0"));
        } else {
            addFuncParam(ident, new SymbolTable.Symbol(ident, "int", false, null, 0, value));
            params.push(new Quadruple("RECOVER", ident + ".1", "2", String.valueOf(value)));
        }
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
            ast.addNode(stmtId, ast.new Stmt(4, currentLine));
            ast.addChild(stmtId, id);
        } else if (currentWord.isWhile()) {
            int id = whileStatement();
            ast.addNode(stmtId, ast.new Stmt(5, currentLine));
            ast.addChild(stmtId, id);
        } else if (currentWord.isBreak()) {
            if (loopCnt > 0) {
                ast.addNode(stmtId, ast.new Stmt(6, currentLine));
                ast.addNode(idCounter++, ast.new Break(currentLine));
                ast.addChild(stmtId, idCounter);
                semanticAnalyzer.jump("$WHILE_END_" + currentWhile);
            } else {
                ERROR('m', currentLine, "break");
            }
            checkSemi();
        } else if (currentWord.isContinue()) {
            if (loopCnt > 0) {
                ast.addNode(stmtId, ast.new Stmt(7, currentLine));
                ast.addNode(idCounter++, ast.new Continue(currentLine));
                ast.addChild(stmtId, idCounter);
                semanticAnalyzer.jump("$WHILE_" + currentWhile);
            } else {
                ERROR('m', currentLine, "continue");
            }
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
                    ast.addNode(stmtId, ast.new Stmt(8, currentLine));
                    if (currentFunc.equals("main")) {
                        boolean seLast = semanticAnalyzer.output;
                        semanticAnalyzer.output = false;
                        exp();
                        semanticAnalyzer.output = seLast;
                        semanticAnalyzer.exit();
                    } else {
                        ast.addChild(stmtId, id);
                        id = exp();
                        semanticAnalyzer.ret(id);
                        semanticAnalyzer.jr();
                    }

                    checkSemi();
                } else {
                    ast.addNode(stmtId, ast.new Stmt(8, currentLine));
                    semanticAnalyzer.jr();
                }

            }

        } else if (currentWord.isPrintf()) {
            int printfId = printf();
            ast.addNode(stmtId, ast.new Stmt(10, currentLine));
            ast.addChild(stmtId, printfId);
        } else if (currentWord.isSemiColon()) {
            ast.addNode(stmtId, ast.new Stmt(11, currentLine));
        } else if (currentWord.isLbrace()) {
            // <Block>
            ast.addNode(stmtId, ast.new Stmt(3, currentLine));
            ast.addChild(stmtId, block());
            CHECKRBRACE();
        } else if (currentWord.isConst()) {
            GETWORD();
            constDeclare();
        } else if (currentWord.isInt()) {
            varDeclare();
        } else if (currentWord.isIdent()) {
            // <LVal> '=' <Exp> ';'
            //  [Exp] ';'
            //  <LVal> = 'getint();'
            int saveIndex = lexicalAnalyzer.index;

            output = false;
            boolean seLast = semanticAnalyzer.output;
            semanticAnalyzer.output = false;
            lVal(false, 0, false);
            GETWORD();
            if (!currentWord.isAssign()) {
                currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                output = true;
                semanticAnalyzer.output = seLast;
                ast.addNode(stmtId, ast.new Stmt(2, currentLine));
                ast.addChild(stmtId, exp());
            } else {
                currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                int line = currentWord.lineCnt;
                output = true;
                semanticAnalyzer.output = seLast;
                String ident = currentWord.getValue();
                int lValId = lVal(false, line, false);
                String shift_local = shift_lval;
                checkValConst(lValId, line);
                GETWORD();
                GETWORD();
                int layer = symbolTableHandler.getLayer(ident, currentLayer);
                if (!currentWord.isGetInt()) {
                    int expId = exp();
                    ast.addNode(stmtId, ast.new Stmt(1, currentLine));
                    ast.addChild(stmtId, lValId);
                    ast.addChild(stmtId, expId);
                    semanticAnalyzer.sw(ident + "." + layer, shift_local, expId);
                } else {
                    ast.addNode(stmtId, ast.new Stmt(9, currentLine));
                    ast.addChild(stmtId, lValId);
                    GETWORD();
                    CHECKLPARENT();
                    checkRparent();
//                    semanticAnalyzer.getInt();
                    semanticAnalyzer.sw(ident + "." + layer, shift_local, "@getInt");
                }
            }
            checkSemi();

        } else {
            ast.addNode(stmtId, ast.new Stmt(2, currentLine));
            ast.addChild(stmtId, exp());
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
            int expId = exp();
            ast.addChild(printfId, expId);
            expCnt++;
        }

        ArrayList<Integer> expList = ast.ast.get(printfId);
        StringBuilder sb = new StringBuilder();
        int j = 0;
        for (int i = 1; i < formatString.length() - 1; i++) {
            if (formatString.charAt(i) == '\\') {
                if (!sb.toString().equals("")) {
                    semanticAnalyzer.printString(sb.toString());
                    sb = new StringBuilder();
                }
                semanticAnalyzer.printChar("\\n");
                i++;
            } else if (formatString.charAt(i) != '%') {
                sb.append(formatString.charAt(i));
            } else {
                if (!sb.toString().equals("")) {
                    semanticAnalyzer.printString(sb.toString());
                    sb = new StringBuilder();
                }
                semanticAnalyzer.printInt(expList.get(j++));
                i++;
            }
        }
        if (!sb.toString().equals("")) {
            semanticAnalyzer.printString(sb.toString());
            sb = new StringBuilder();
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
        condCnt = 0;
        boolean seLast = semanticAnalyzer.output;
        semanticAnalyzer.output = false;
        int saveIndex = lexicalAnalyzer.index;
        int lastcondition = condition();

        start = ++labelCnt;
        end = start + condCnt + 1;
        labelCnt += condCnt + 5;

        boolean lastoutput = output;
        output = false;
        currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
        semanticAnalyzer.output = seLast;
        int condId = condition_2(lastcondition);
        output = lastoutput;

        checkRparent();
        GETWORD();

        //如果最后一个and成立，直接跳到label；如果不成立，跳到下一个（其实不用跳）
        //如果最后一个and不成立，直接跳到end，如果不成立，跳到下一个（其实不用跳）
        //中间的and，成立不跳，不成立下一个
        int end_tmp = end;

        semanticAnalyzer.label("$Cond_" + (end - 1) + ":");
        int thenId = stmt();
        semanticAnalyzer.jump("$Cond_" + (end_tmp + 1));
        int elseId = 0;


        semanticAnalyzer.label("$Cond_" + (end_tmp) + ":");
        if (lexicalAnalyzer.checkElse()) {
            GETWORD();
            GETWORD();
            elseId = stmt();
        }

        semanticAnalyzer.label("$Cond_" + (end_tmp + 1) + ":");
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
        boolean seLast = semanticAnalyzer.output;
        semanticAnalyzer.output = false;
        int saveIndex = lexicalAnalyzer.index;
        condCnt = 0;
        int lastcondition = condition();


        start = ++labelCnt;
        end = start + condCnt + 2;
        labelCnt += condCnt + 5;

        output = false;
        currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
        semanticAnalyzer.output = seLast;
        int conditionId = condition_2(lastcondition);

        output = true;
        ast.addChild(whileId, conditionId);
        int whileCnt = ++labelCnt;
        int lastWhile = currentWhile;
        currentWhile = whileCnt;

        checkRparent();
        GETWORD();
        semanticAnalyzer.label("$Cond_" + (end - 1) + ":");

        int condcnt_tmp = condCnt;
        int end_tmp = end;

        ast.addChild(whileId, stmt());

        isWhileEnd = true;
        int saveEndIndex = lexicalAnalyzer.index;
        currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
        output = false;

        start = ++labelCnt;
        end = end_tmp;

        condCnt = condcnt_tmp;
        semanticAnalyzer.label("$WHILE_" + currentWhile + ":");
        condition_2(lastcondition);
        output = true;
        semanticAnalyzer.label("$Cond_" + end + ":");
        semanticAnalyzer.label("$WHILE_END_" + currentWhile + ":");
        labelCnt++;
        isWhileEnd = false;
        currentWord = lexicalAnalyzer.getByIndex(saveEndIndex - 1);
        currentWhile = lastWhile;

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

    public int condition_2(int lastcondition) {
        int conditionId = idCounter++;
        ast.addNode(conditionId, ast.new Cond(currentLine));
        lOrExp_2(lastcondition, conditionId);
        PRINT("<Cond>");
        return conditionId;
    }

    //<LOrExp> ::= <LAndExp> | <LOrExp> '||' <LAndExp>
    // <LOrExp> ::= <LAndExp> {'||' <LAndExp>}
    public void lOrExp(int parent) {
        ast.addChild(parent, lAndExp());
        condCnt++;
        while (lexicalAnalyzer.checkOr()) {
            condCnt++;
            PRINT("<LOrExp>");
            GETWORD();
            GETWORD();
            ast.addChild(parent, lAndExp());
        }
        PRINT("<LOrExp>");
    }

    public void lOrExp_2(int lastcondition, int parent) {
        ArrayList<Integer> list = ast.ast.get(lastcondition);
        int i = 1;
        int nextLabel;
        int cnt = ast.ast.get(list.get(0)).size();

        if (i == ast.ast.get(lastcondition).size()) {  //如果只有一个或条件
            nextLabel = isWhileEnd ? end - 1 : end; //如果是do while后面的条件， 成立则跳到do；如果不是 不成立则跳到最后
            ast.addChild(parent, lAndExp_2(nextLabel, ast.ast.get(list.get(0)).size(), true));
        } else {
            nextLabel = start + cnt;  //下一个或的位置
            ast.addChild(parent, lAndExp_2(nextLabel, ast.ast.get(list.get(0)).size(), false));
        }
        while (lexicalAnalyzer.checkOr()) {
            PRINT("<LOrExp>");
            GETWORD();
            GETWORD();
            i++;
            cnt = ast.ast.get(list.get(i - 1)).size();
            if (i == ast.ast.get(lastcondition).size()) {
                nextLabel = isWhileEnd ? end - 1 : end; //如果是最后一个或条件
            } else {
                nextLabel = start + cnt; //下一个或条件
            }
            boolean last = (i == ast.ast.get(lastcondition).size());
            int land = lAndExp_2(nextLabel, cnt, last);
            ast.addChild(parent, land);
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
            condCnt++;
            PRINT("<LAndExp>");
            GETWORD();
            GETWORD();
            ast.addChild(lAndId, eqExp());
        }
        PRINT("<LAndExp>");
        return lAndId;
    }

    public int lAndExp_2(int nextLabel, int lastand, boolean last) {
        int lAndId = idCounter++;
        ast.addNode(lAndId, ast.new LAndExp(currentLine));
        semanticAnalyzer.label("$Cond_" + start + ":");
        int eqExpId = eqExp();
        ast.addChild(lAndId, eqExpId);
        int j = 0;

        if (last && j != lastand - 1 && isWhileEnd) {
            semanticAnalyzer.beqz(eqExpId, "$Cond_" + end);
        } else if (last && j == lastand - 1 && isWhileEnd) {
            semanticAnalyzer.beq(eqExpId, "1", "$Cond_" + (end - 1));
        } else if (last && j == lastand - 1) {
            semanticAnalyzer.beqz(eqExpId, "$Cond_" + end);
        } else if (j == lastand - 1) {
            semanticAnalyzer.beq(eqExpId, "1", "$Cond_" + (end - 1));
        } else {
            semanticAnalyzer.beqz(eqExpId, "$Cond_" + nextLabel);
        }

        start++;
        j++;
        while (lexicalAnalyzer.checkAnd()) {
            PRINT("<LAndExp>");
            GETWORD();
            GETWORD();
            semanticAnalyzer.label("$Cond_" + start + ":");
            eqExpId = eqExp();
            ast.addChild(lAndId, eqExpId);

            if (last && j != lastand - 1 && isWhileEnd) {
                semanticAnalyzer.beqz(eqExpId, "$Cond_" + end);
            } else if (last && j == lastand - 1 && isWhileEnd) {
                semanticAnalyzer.beq(eqExpId, "1", "$Cond_" + (end - 1));
            } else if (last && j == lastand - 1) {
                semanticAnalyzer.beqz(eqExpId, "$Cond_" + end);
            } else if (j == lastand - 1) {
                semanticAnalyzer.beq(eqExpId, "1", "$Cond_" + (end - 1));
            } else {
                semanticAnalyzer.beqz(eqExpId, "$Cond_" + nextLabel);
            }
            start++;
            j++;
        }
        PRINT("<LAndExp>");
        return lAndId;
    }

    // <EqExp> ::= <RelExp> | <EqExp> (== | !=) <RelExp>
    // <EqExp> ::= <RelExp> { (== | !=) <RelExp> }
    public int eqExp() {
        int eqExpId = idCounter++;
        ast.addNode(eqExpId, ast.new EqExp(currentLine));
        int relExpId = relExp("");
        ast.addChild(eqExpId, relExpId);
        int last = relExpId;
        if (lexicalAnalyzer.checkEq()) {
            while (lexicalAnalyzer.checkEq()) {
                PRINT("<EqExp>");
                GETWORD();
                String eq = currentWord.getValue();
                GETWORD();
                relExpId = relExp(eq);
                ast.addChild(eqExpId, relExpId);
                if (eq.equals("==")) {
                    semanticAnalyzer.eq(idCounter, last, relExpId);
                } else {
                    semanticAnalyzer.neq(idCounter, last, relExpId);
                }
                last = idCounter++;
            }
        } else {
            semanticAnalyzer.neqz(idCounter, last);
            last = idCounter;
        }

        PRINT("<EqExp>");
        return last;
    }

    // <RelExp> ::= <AddExp> | <RelExp> (< | > | <= | >=) <AddExp>
    // <RelExp> ::= <AddExp>  { (< | > | <= | >=) <AddExp> }
    public int relExp(String eq) {
        int relExpId = idCounter++;
        ast.addNode(relExpId, ast.new RelExp(eq, currentLine));
        int addExpId = addExp("", false);
        ast.addChild(relExpId, addExpId);
        int last = addExpId;

        while (lexicalAnalyzer.checkRel()) {
            PRINT("<RelExp>");
            GETWORD();
            String op = currentWord.getValue();
            GETWORD();
            addExpId = addExp(op, false);
            ast.addChild(relExpId, addExpId);
            switch (op) {
                case "<":
                    semanticAnalyzer.lss(idCounter, last, addExpId);
                    break;
                case ">":
                    semanticAnalyzer.grt(idCounter, last, addExpId);
                    break;
                case "<=":
                    semanticAnalyzer.leq(idCounter, last, addExpId);
                    break;
                case ">=":
                    semanticAnalyzer.geq(idCounter, last, addExpId);
                    break;
            }

            last = idCounter++;
        }
        relExpId = last;

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
            rangex = (ast.map.get(constExp())).value;
            checkRbrack();
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                rangey = (ast.map.get(constExp())).value;
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
        int constInitVal = constInitVal(ident, dimension);
        semanticAnalyzer.defineEnd();
        if (!isDup) {
            ast.addNode(constDefineId, ast.new Def(true, ident, rangex, rangey, currentLine));
            ast.addChild(constDefineId, constInitVal);
        }

        PRINT("<ConstDef>");

        return constDefineId;
    }

    // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'
    public int constInitVal(String name, int dimension) {
        int constInitValId = idCounter++;
        int shift = 0;
        int rangex = 0;
        int rangey = 0;
        if (dimension == 0) {
            int value = (ast.map.get(constExp())).value;
            semanticAnalyzer.conval(name + "." + currentLayer, value);
            symbolTableHandler.addToTable(currentLayer,
                    new SymbolTable.Symbol(name, "int", true, value));
        } else if (dimension == 1) {
            CHECKLBRACE();
            GETWORD();
            ArrayList<Integer> list = new ArrayList<>();
            if (!currentWord.isRbrace()) {
                int value = (ast.map.get(constExp())).value;
                rangex++;
                semanticAnalyzer.conval(name + "." + currentLayer, value);
                list.add(value);
                PRINT("<ConstInitVal>");
                while (lexicalAnalyzer.checkComma()) {
                    GETWORD(); //comma
                    GETWORD();
                    value = (ast.map.get(constExp())).value;
                    rangex++;
                    shift += 4;
                    semanticAnalyzer.conval(name + "." + currentLayer, value);
                    list.add(value);
                    PRINT("<ConstInitVal>");
                }
                GETWORD();
            }
            symbolTableHandler.addToTable(currentLayer,
                    new SymbolTable.Symbol(name, "int", true, list, rangex));
            CHECKRBRACE();
        } else {
            CHECKLBRACE();
            ArrayList<ArrayList<Integer>> list = new ArrayList<>();
            do {
                GETWORD();
                CHECKLBRACE();
                GETWORD();
                rangex++;
                if (!currentWord.isRbrace()) {
                    ArrayList<Integer> tmp = new ArrayList<>();
                    int value = (ast.map.get(constExp())).value;
                    semanticAnalyzer.conval(name + "." + currentLayer, value);
                    tmp.add(value);
                    rangey++;
                    shift += 4;
                    PRINT("<ConstInitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD(); //comma
                        GETWORD();
                        value = (ast.map.get(constExp())).value;
                        semanticAnalyzer.conval(name + "." + currentLayer, value);
                        tmp.add(value);
                        rangey++;
                        shift += 4;
                        PRINT("<ConstInitVal>");
                    }
                    GETWORD();
                    list.add(tmp);
                }
                CHECKRBRACE();
                PRINT("<ConstInitVal>");
                GETWORD();
            } while (currentWord.isComma());
            rangey /= rangex;
            symbolTableHandler.addToTable(currentLayer,
                    new SymbolTable.Symbol(name, "int", true, list, rangex, rangey));
            ast.addNode(constInitValId, ast.new InitVal(true, null, dimension, currentLine));
            CHECKRBRACE();
        }

        semanticAnalyzer.define(name, currentLayer, rangex, rangey);
        semanticAnalyzer.conval_recover();

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

        int dimension = 0;
        int rangex = 0;
        int rangey = 0;
        //{ '[' <ConstExp> ']' }
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            GETWORD();
            rangex = (ast.map.get(constExp())).value;
            checkRbrack();
            if (lexicalAnalyzer.checkLbrack()) {
                dimension++;
                GETWORD();
                GETWORD();
                rangey = (ast.map.get(constExp())).value;
                checkRbrack();
            }
        }
        // '='
        int initValId = 0;

        if (lexicalAnalyzer.checkAssign()) {
            GETWORD();
            GETWORD();
            if (currentFunc.equals("")) {
                initValId = constInitVal(ident, dimension);
            } else {
                initValId = initVal(ident, dimension);
            }
            if (!isDup) {
                ast.addNode(varDefineId, ast.new Def(false, ident, rangex, rangey, currentLine));
                ast.addChild(varDefineId, initValId);
            }
        } else if (!isDup) {
            semanticAnalyzer.define(ident, currentLayer, rangex, rangey);
            ast.addNode(varDefineId, ast.new Def(false, ident, rangex, rangey, currentLine));
            if (dimension == 0) {
                symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(ident, "int", false, 0));
            } else if (dimension == 1) {
                symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(ident, "int", false, null, rangex));
            } else {
                symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(ident, "int", false, null, rangex, rangey));
            }
        }

        semanticAnalyzer.defineEnd();

        // <InitVal>
        PRINT("<VarDef>");
        return varDefineId;
    }

    public int initVal(String name, int dimension) {
        int initValId = idCounter++;
        ArrayList<ArrayList<Integer>> exps = new ArrayList<>();
        int shift = 0;
        int rangex = 0;
        int rangey = 0;
        if (dimension == 0) {
            int expId = exp();
            semanticAnalyzer.assign(name + "." + currentLayer, expId);
            symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(name, "int", false, 0));
        } else if (dimension == 1) {
            CHECKLBRACE();
            GETWORD();
            if (!currentWord.isRbrace()) {
                int expId = exp();
                semanticAnalyzer.assign(name + "." + currentLayer, expId);
                shift += 4;
                rangex++;
                ArrayList<Integer> tmp = new ArrayList<>();
                tmp.add(initValId);
                PRINT("<InitVal>");
                while (lexicalAnalyzer.checkComma()) {
                    GETWORD(); //comma
                    GETWORD();
                    expId = exp();
                    semanticAnalyzer.assign(name + "." + currentLayer, expId);
                    shift += 4;
                    rangex++;
                    tmp.add(expId);
                    PRINT("<InitVal>");
                }
                GETWORD();
            }
            symbolTableHandler.addToTable(currentLayer, new SymbolTable.Symbol(name, "int", false, null, rangex));
            CHECKRBRACE();
        } else {
            CHECKLBRACE();
            do {
                GETWORD();
                CHECKLBRACE();
                GETWORD();
                rangex++;
                if (!currentWord.isRbrace()) {
                    int expId = exp();
                    semanticAnalyzer.assign(name + "." + currentLayer, expId);
                    shift += 4;
                    rangey++;
                    ArrayList<Integer> tmp = new ArrayList<>();
                    PRINT("<InitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD(); //comma
                        GETWORD();
                        expId = exp();
                        semanticAnalyzer.assign(name + "." + currentLayer, expId);
                        shift += 4;
                        rangey++;
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
            rangey /= rangex;
            symbolTableHandler.addToTable(currentLayer,
                    new SymbolTable.Symbol(name, "int", false, null, rangex, rangey));

        }

        semanticAnalyzer.define(name, currentLayer, rangex, rangey);
        semanticAnalyzer.assign_recover();
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

        int mulExpId = mulExp(isConst);
        AbstractSyntaxTree.Exp exp = ast.new Exp("+", isConst, (ast.map.get(mulExpId)).value, currentLine);
        ast.addNode(addExpId, exp);
        ast.addChild(addExpId, mulExpId);


        int last = mulExpId;

        while (lexicalAnalyzer.checkUnaryAdd()) {
            PRINT("<AddExp>");
            GETWORD();
            String unaryAdd = currentWord.value;

            GETWORD();
            mulExpId = mulExp(isConst);
            ast.addChild(addExpId, mulExpId);
            if (!isConst) {
                if (unaryAdd.equals("+")) {
                    semanticAnalyzer.add(addExpId, last, mulExpId);
                } else {
                    semanticAnalyzer.sub(addExpId, last, mulExpId);
                }
                last = addExpId;
            } else {
                if (unaryAdd.equals("+")) {
                    ast.map.get(last).value += (ast.map.get(mulExpId)).value;
                } else {
                    ast.map.get(last).value -= (ast.map.get(mulExpId)).value;
                }
            }

        }

        PRINT("<AddExp>");
        return last;

    }

    // <MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp>
    // <MulExp> ::= <UnaryExp> {  (*|/|%) <UnaryExp>  }
    public int mulExp(boolean isConst) {
        int mulExpId = idCounter++;
        int unaryExpId = unaryExp(isConst);
        AbstractSyntaxTree.MulExp mul = ast.new MulExp("*", (ast.map.get(unaryExpId)).value, currentLine);
        ast.addNode(mulExpId, mul);
        ast.addChild(mulExpId, unaryExpId);

        int last = unaryExpId;
        while (lexicalAnalyzer.checkUnaryCal()) {
            PRINT("<MulExp>");
            GETWORD();
            String cal = currentWord.getValue();
            GETWORD();
            unaryExpId = unaryExp(isConst);
            ast.addChild(mulExpId, unaryExpId);
            if (!isConst) {
                if (cal.equals("*")) {
                    semanticAnalyzer.mul(mulExpId, last, unaryExpId);
                } else if (cal.equals("/")) {
                    semanticAnalyzer.div(mulExpId, last, unaryExpId);
                } else {
                    semanticAnalyzer.mod(mulExpId, last, unaryExpId);
                }
                last = mulExpId;
            } else {
                int value = (ast.map.get(unaryExpId)).value;
                if (cal.equals("*")) {
                    ast.map.get(last).value *= value;
                } else if (cal.equals("/")) {
                    ast.map.get(last).value /= value;
                } else {
                    ast.map.get(last).value %= value;
                }
            }
        }

        return last;

    }

    public int unaryExp(boolean isConst) {
        int unaryExpId = idCounter++;
        if (currentWord.isIdent() && lexicalAnalyzer.checkFuncParam()) {
            String ident = currentWord.getValue();
            int funcLine = currentWord.lineCnt;
            GETWORD();
            if (lexicalAnalyzer.checkRbrack()) {
                ERROR('j', currentLine, currentWord.getValue());
            } else if (!lexicalAnalyzer.checkRparent()) {
                GETWORD();
                int funcRParamsId = funcRParams(ident, funcLine);
                ast.addNode(unaryExpId, ast.new UnaryExp("+", isConst, 3, 0, currentLine));
                ast.addChild(unaryExpId, funcRParamsId);
                checkRparent();
            } else {
                checkFuncParamsCnt(ident, 0, null, funcLine);
                int identId = idCounter++;
                ast.addNode(identId, ast.new Ident(ident, currentLine));
                ast.addNode(unaryExpId, ast.new UnaryExp("+", isConst, 3, 0, currentLine));
                ast.addChild(unaryExpId, identId);
                checkRparent();
            }
            semanticAnalyzer.call(ident, getFuncParamsCnt(ident));
            semanticAnalyzer.funcRet(unaryExpId);
        } else if (currentWord.isUnaryOp()) {
            unaryOp();
            String unaryOp = currentWord.getValue();
            GETWORD();
            int unaryId = unaryExp(isConst);
            AbstractSyntaxTree.UnaryExp unaryExp = ast.new UnaryExp(unaryOp, isConst, 4, 0, currentLine);
            if (!isConst) {
                if (unaryOp.equals("+")) {
                    unaryExpId = unaryId;
                } else if (unaryOp.equals("-")) {
                    semanticAnalyzer.sub(unaryExpId, "0", unaryId);
                } else {
                    semanticAnalyzer.not(unaryExpId, unaryId);
                }
                ast.addNode(unaryExpId, unaryExp);
                ast.addChild(unaryExpId, unaryId);
            } else {
                AbstractSyntaxTree.SyntaxNode node = ast.map.get(unaryId);
                int value = (ast.map.get(unaryId)).value;
                if (unaryOp.equals("+")) {
                    (ast.map.get(unaryId)).value = value;
                } else if (unaryOp.equals("-")) {
                    (ast.map.get(unaryId)).value = -value;
                } else {
                    (ast.map.get(unaryId)).value = (value == 0) ? 0 : 1;
                }
                unaryExpId = unaryId;
            }
        } else {
            int primaryExpId = primaryExp(isConst, unaryExpId);

            ast.addNode(unaryExpId, ast.new UnaryExp("+", isConst,
                    1, (ast.map.get(primaryExpId)).value, currentLine));
            ast.addChild(unaryExpId, primaryExpId);
            unaryExpId = primaryExpId;
        }
        PRINT("<UnaryExp>");
        return unaryExpId;
    }

    //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
    public int primaryExp(boolean isConst, int unaryExpId) {
        int primaryExpId = idCounter++;
        int id;
        if (currentWord.isLparent()) {
            GETWORD();
            id = isConst ? constExp() : exp();
            primaryExpId = id;
            checkRparent();
            ast.addNode(primaryExpId, ast.new PrimaryExp(1, isConst, (ast.map.get(id)).value, currentLine));
            ast.addChild(primaryExpId, id);
        } else if (currentWord.isIdent()) {
            String ident = currentWord.getValue();
            id = lVal(isConst, currentWord.lineCnt, true);
            primaryExpId = id;
            ast.addNode(primaryExpId, ast.new PrimaryExp(2, isConst, (ast.map.get(id)).value, currentLine));
            ast.addChild(primaryExpId, id);
        } else if (currentWord.isNumber()) {
            PRINT("<Number>");
            if (!isConst) number(primaryExpId, Integer.parseInt(currentWord.getValue()));
            int number = (int) Long.parseLong(currentWord.getValue());
            ast.addNode(primaryExpId, ast.new PrimaryExp(3, isConst, number, currentLine));
        } else {
            ERROR(9, currentLine, currentWord.getValue());
        }
        PRINT("<PrimaryExp>");
        return primaryExpId;
    }

    // <LVal> ::= <Ident> {'[' <Exp> ']'}
    public int lVal(boolean isConst, int line, boolean loadword) {
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
            rangx = isConst ? ast.map.get(constExp()).value : exp();
            dimension++;
            checkRbrack();
        }
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            GETWORD();
            rangy = isConst ? ast.map.get(constExp()).value : exp();
            checkRbrack();
        }

        if (isConst) {
            int value = getLVal(name, dimension, rangx, rangy);
            ast.addNode(lValId, ast.new LVal(isConst, rangx, rangy, dimension, name, value, currentLine));
        } else {

            ast.addNode(lValId, ast.new LVal(isConst, rangx, rangy, dimension, name, 0, currentLine));
        }


        int layer = symbolTableHandler.getLayer(name, currentLayer);
        int shift = (getArrayLength(name) * rangx + rangy) * 4;
        if (isConst && loadword) {
//            semanticAnalyzer.lw(lValId, name + "." + layer + "$" + shift);  //TODO
            shift_lval = String.valueOf(shift);
        } else if (!isConst && loadword) {
            if (dimension == 2) {
                semanticAnalyzer.mul(idCounter++, rangx, String.valueOf(getArrayLength(name)));
                semanticAnalyzer.add(idCounter++, idCounter - 2, rangy);
                semanticAnalyzer.sll(idCounter++, idCounter - 2, 2);
                semanticAnalyzer.lw(lValId, name + "." + layer, idCounter - 1);
            } else if (dimension == 1) {
                semanticAnalyzer.sll(idCounter++, rangx, 2);
                semanticAnalyzer.lw(lValId, name + "." + layer, idCounter - 1);
            } else {
                semanticAnalyzer.lval(lValId, name + "." + layer);
//                semanticAnalyzer.lw(lValId, name + "." + layer);
            }
        } else if (!isConst) {
            if (dimension == 2) {
                semanticAnalyzer.mul(idCounter++, rangx, String.valueOf(getArrayLength(name)));
                semanticAnalyzer.add(idCounter++, idCounter - 2, rangy);
                semanticAnalyzer.sll(idCounter++, idCounter - 2, 2);
                shift_lval = "tmp@" + (idCounter - 1);
            } else if (dimension == 1) {
                semanticAnalyzer.sll(idCounter++, rangx, 2);
                shift_lval = "tmp@" + (idCounter - 1);
            } else {
                shift_lval = "0";
            }
        }

        PRINT("<LVal>");
        return lValId;

    }

    //<FuncRParams> → <Exp> { ',' <Exp> }
    public int funcRParams(String ident, int funcLine) {
        int funcRParamsId = idCounter++;
        int paracnt = 0;
        ArrayList<Integer> types = new ArrayList<>();

        ast.addNode(funcRParamsId, ast.new FuncR(ident, currentLine));
        types.add(funcRParam(ident, paracnt++, funcRParamsId));

//        while (lexicalAnalyzer.checkComma() && paracnt < size)
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            types.add(funcRParam(ident, paracnt++, funcRParamsId));
        }

        checkFuncParamsCnt(ident, paracnt, types, funcLine);
        PRINT("<FuncRParams>");
        return funcRParamsId;
    }

    public int funcRParam(String ident, int cnt, int funcRParamsId) {
        int saveIndex = lexicalAnalyzer.index;

        output = false;
        boolean seLast = semanticAnalyzer.output;
        semanticAnalyzer.output = false;

        int lvalId = lVal(false, currentLine, false);
        int lValIndex = lexicalAnalyzer.index;
        currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
        semanticAnalyzer.output = seLast;
        output = true;

        String funcName = currentWord.getValue();
        if (lexicalAnalyzer.checkLparent()) {
            AbstractSyntaxTree.Func func = ast.getFuncByName(funcName);
            if (func.type.equals("VOIDTK")) {
                ERROR('e', currentLine, "void");
            }
        }

        int expLVal = -1;
        int dim = getFuncParamDim(ident, cnt);
        if (dim == 0) {
            int expId = exp();
            ast.addChild(funcRParamsId, expId);
            expLVal = lexicalAnalyzer.index;
            semanticAnalyzer.para(expId, dim, 0);
        } else {
            int rangey = symbolTableHandler.getRangey(funcName, currentLayer);
            int shift = 0;
            int layer = symbolTableHandler.getLayer(funcName, currentLayer);
            if (lexicalAnalyzer.checkLbrack()) {
                GETWORD();  //[
                GETWORD();  //[
                shift = exp();
                GETWORD();  //]
                semanticAnalyzer.para(funcName + "." + layer, shift, dim, rangey);
            } else {
                semanticAnalyzer.para(funcName + "." + layer, dim, rangey);
            }
            expLVal = lexicalAnalyzer.index;

        }

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

    public void number(int id, int num) {
//        semanticAnalyzer.li(id, String.valueOf(num));
        semanticAnalyzer.number(id, String.valueOf(num));
    }

    public void ERROR(int errorCode, int errorLine, String errorMessage) {
        if (output) exceptionHandler.addError(new MyException(errorCode, errorLine, errorMessage));
    }

    public void PRINT(String str) {
        if (output) {
            try {
//                System.out.println(str);
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

    public int getLVal(String ident, int dimension, int rangex, int rangey) {
        SymbolTable.Symbol symbol = symbolTableHandler.getSymbol(ident, currentLayer);
        if (symbol.dimension == 0) {
            return symbol.value_0;
        } else if (symbol.dimension == 1) {
            return symbol.value_1.get(rangex);
        } else {
            return symbol.value_2.get(rangex).get(rangey);
        }
    }

    public int getArrayLength(String ident) {
        SymbolTable.Symbol symbol = symbolTableHandler.getSymbol(ident, currentLayer);
        return symbol == null ? 0 : symbol.rangey;
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

    public int getFuncParamsCnt(String ident) {
        SymbolTable symbolTable = symbolTableHandler.functions.get(ident);
        if (symbolTable == null) return 0;
        return symbolTable.symbols.size();
    }

    public int getFuncParamDim(String ident, int cnt) {
        SymbolTable symbolTable = symbolTableHandler.functions.get(ident);
        if (symbolTable == null) return 0;
        return symbolTable.symbols.get(cnt).dimension;
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

    public boolean checkVoidReturn(int id) {
        if (currentFuncType.equals("void")) {
            int blockId = ast.getChild(id).get(ast.getChild(id).size() - 1);
            if (ast.getChild(blockId).size() == 0) {
                return false;
            }

            int stmtId = ast.getChild(blockId).get(ast.getChild(blockId).size() - 1);
            if (!(ast.getById(stmtId) instanceof AbstractSyntaxTree.Stmt)) {
                return false;
            } else {
                AbstractSyntaxTree.Stmt stmt = (AbstractSyntaxTree.Stmt) ast.getById(stmtId);
                return stmt.type == 8;
            }
        }
        return true;
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
