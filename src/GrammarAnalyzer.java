import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GrammarAnalyzer {
    LexicalAnalyzer lexicalAnalyzer;
    Word currentWord = new Word();
    int currentLine = 0;
    boolean output = true;
    BufferedWriter out;

    public GrammarAnalyzer(LexicalAnalyzer lexicalAnalyzer) {
        this.lexicalAnalyzer = lexicalAnalyzer;
    }

    public void analyse() {
        try {
            out = new BufferedWriter(new FileWriter("output.txt"));
            Program();
            PRINT("<CompUnit>");
            out.close();
        } catch (MyException e) {
            System.out.println("=======" + e.errorLine + "======" + e.errorCode);
        } catch (IOException e) {
            //
        }
    }

    //<CompUnit> ::= {<Decl>} {<FuncDef>} <MainFuncDef>
    //<Decl> ::= <ConstDecl> | <VarDecl>
    public void Program() throws MyException {
        //{<Decl>}
        while (lexicalAnalyzer.hasWord()) {
            //<ConstDecl>
            GETWORD();
            if (currentWord.isConst()) {
                GETWORD();    //const
                constDeclare();

            }

            //<VarDecl>
            else if (!lexicalAnalyzer.checkFunc()) {
                varDeclare();
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
                    funcDef();
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
            if (lexicalAnalyzer.hasWord())
                mainFuncDef();
        } catch (MyException e) {
            System.out.println("=======" + e.errorLine + "======" + e.errorCode);
        }

    }

    public void constDeclare() {
        try {
            if (!currentWord.isInt()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                constDefine();
                GETWORD();
            } while (currentWord.isComma());
            CHECKSEMI();
            PRINT("<ConstDecl>");
        } catch (MyException e) {
            //TODO error
        }
    }

    // <VarDecl> ::= <BType> <VarDef> {,<VarDef>} ';'
    public void varDeclare() {
        try {
            do {
                GETWORD();
                varDefine();
                GETWORD();
            } while (currentWord.isComma());
            CHECKSEMI();
            PRINT("<VarDecl>");
        } catch (MyException e) {
            System.out.println("=======" + e.errorLine + "======" + e.errorCode);

        }
    }

    //<FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
    public void funcDef() throws MyException {
        String funcType = currentWord.getType();
        PRINT("<FuncType>");
        GETWORD();
        CHECKIDENT();
        GETWORD();
        if (currentWord.isLparent()) {
            if (lexicalAnalyzer.checkRparent()) {
                GETWORD();
                GETWORD();
            } else {
                GETWORD();
                funcFParams();
                GETWORD();
                CHECKRPARENT();
                GETWORD();
            }
        } else {
            ERROR(100, currentLine);
        }
        block();
        PRINT("<FuncDef>");
    }

    //<MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
    public void mainFuncDef() throws MyException {
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        block();
        PRINT("<MainFuncDef>");
    }

    //<FuncFParams> ::= <FuncFParam> { ',' <FuncFParam> }
    public void funcFParams() throws MyException {
        funcFParam();
        while (lexicalAnalyzer.checkComma()) {
            GETWORD(); //Comma
            GETWORD();
            funcFParam();
        }
        PRINT("<FuncFParams>");
    }

    //<FuncFParam> ::= <BType> <Ident> ['[' ']' { '[' <ConstExp> ']' }]
    public void funcFParam() throws MyException {
        if (!currentWord.isInt()) {
            ERROR(106, currentLine);
        }
        GETWORD();
        CHECKIDENT();

        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            CHECKRBRACK();
        }

        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            constExp();
            GETWORD();
            CHECKRBRACK();
        }

        PRINT("<FuncFParam>");
    }

    //<Block> ::= '{' { <BlockItem> } '}'
    public void block() throws MyException {
        if (!currentWord.isLbrace()) {
            ERROR(100, currentLine);
        }
        GETWORD();
        while (!currentWord.isRbrace()) {
            blockItem();
            GETWORD();
        }

        PRINT("<Block>");
    }

    //<BlockItem> ::= <Decl> | <Stmt>
    public void blockItem() throws MyException {
        if (currentWord.isConst()) {
            GETWORD();
            constDeclare();
        } else if (currentWord.isInt()) {
            varDeclare();
        } else {
            stmt();
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
    public void stmt() throws MyException {
        if (currentWord.isIf()) {
            ifStatement();
        } else if (currentWord.isWhile()) {
            whileStatement();
        } else if (currentWord.isBreak()) {
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isContinue()) {
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isReturn()) {
            GETWORD();
            if (!currentWord.isSemiColon()) {
                exp();
                GETWORD();
                CHECKSEMI();
            }
        } else if (currentWord.isPrintf()) {
            //'printf('FormatString{,<Exp>}');'
            GETWORD();
            CHECKLPARENT();
            GETWORD();
            if (!currentWord.isStrcon()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            while (currentWord.isComma()) {
                GETWORD();
                exp();
                GETWORD();
            }
            CHECKRPARENT();
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isSemiColon()) {

        } else if (currentWord.isLbrace()) {
            // <Block>
            block();
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
                    exp();
                } else {
                    currentWord = lexicalAnalyzer.getByIndex(saveIndex - 1);
                    output = true;
                    lVal();
                    GETWORD(); // assign =
                    GETWORD();
                    if (!currentWord.isGetInt()) {
                        exp();
                    } else {
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
                exp();
            }

        } else {
            try {
                exp();
                GETWORD();
                CHECKSEMI();
            } catch (MyException e) {
                ERROR(100, currentLine);
            }
        }
        PRINT("<Stmt>");
    }

    // 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
    public void ifStatement() throws MyException {
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        condition();
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        stmt();
        if (lexicalAnalyzer.checkElse()) {
            GETWORD();
            GETWORD();
            stmt();
        }
    }

    // 'while' '(' <Cond> ')' <Stmt>
    public void whileStatement() throws MyException {
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        condition();
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        stmt();
    }

    public void condition() throws MyException {
        lOrExp();
        PRINT("<Cond>");
    }

    //<LOrExp> ::= <LAndExp> | <LOrExp> '||' <LAndExp>
    // <LOrExp> ::= <LAndExp> {'||' <LAndExp>}
    public void lOrExp() throws MyException {
        lAndExp();
        while (lexicalAnalyzer.checkOr()) {
            PRINT("<LOrExp>");
            GETWORD();
            GETWORD();
            lAndExp();
        }
        PRINT("<LOrExp>");
    }

    // <LAndExp> ::= <EqExp> | <LAndExp> && <EqExp>
    // <LAndExp> ::= <EqExp> { && <EqExp> }
    public void lAndExp() throws MyException {
        eqExp();
        while (lexicalAnalyzer.checkAnd()) {
            PRINT("<LAndExp>");
            GETWORD();
            GETWORD();
            eqExp();
        }
        PRINT("<LAndExp>");
    }

    // <EqExp> ::= <RelExp> | <EqExp> (== | !=) <RelExp>
    // <EqExp> ::= <RelExp> { (== | !=) <RelExp> }
    public void eqExp() throws MyException {
        relExp();
        while (lexicalAnalyzer.checkEq()) {
            PRINT("<EqExp>");
            GETWORD();
            GETWORD();
            relExp();
        }

        PRINT("<EqExp>");
    }

    // <RelExp> ::= <AddExp> | <RelExp> (< | > | <= | >=) <AddExp>
    // <RelExp> ::= <AddExp>  { (< | > | <= | >=) <AddExp> }
    public void relExp() throws MyException {
        addExp();
        while (lexicalAnalyzer.checkRel()) {
            PRINT("<RelExp>");
            GETWORD();
            GETWORD();
            addExp();
        }
        PRINT("<RelExp>");
    }

    // <ConstDef> ::= <Ident> { '[' <ConstExp> ']' } '=' <ConstInitVal>
    public void constDefine() throws MyException {
        //table
        //<Ident>
        CHECKIDENT();
        String ident = currentWord.getValue();
        int dimension = 1;
        //{ '[' <ConstExp> ']' }
        GETWORD();
        if (currentWord.isLbrack()) {
            dimension++;
            GETWORD();
            constExp();
            GETWORD();
            CHECKRBRACK();
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                constExp();
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
        constInitVal(dimension);
        PRINT("<ConstDef>");
    }

    // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'
    public void constInitVal(int dimension) throws MyException {
        if (dimension == 1) {
            constExp();
        } else if (dimension == 2) {                //TODO calculate const value
            if (!currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            if (!currentWord.isRbrace()) {
                constExp();
                PRINT("<ConstInitVal>");
                while (lexicalAnalyzer.checkComma()) {
                    GETWORD(); //comma
                    GETWORD();
                    constExp();
                    PRINT("<ConstInitVal>");
                }
                GETWORD();
            }
            CHECKRBRACE();
        } else {
            if (!currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                if (!currentWord.isLbrace()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
                if (!currentWord.isRbrace()) {
                    constExp();
                    PRINT("<ConstInitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD(); //comma
                        GETWORD();
                        constExp();
                        PRINT("<ConstInitVal>");
                    }
                    GETWORD();
                }
                CHECKRBRACE();

                PRINT("<ConstInitVal>");
                GETWORD();
            } while (currentWord.isComma());
            CHECKRBRACE();
        }
        PRINT("<ConstInitVal>");
    }

    //<Ident> | <Ident> { '[' <ConstExp> ']' } '=' <InitVal>
    //<Ident>
    public void varDefine() throws MyException {
        CHECKIDENT();
        //table
        String ident = currentWord.getValue();
        int dimension = 1;
        //{ '[' <ConstExp> ']' }
        if (lexicalAnalyzer.checkLbrack()) {
            dimension++;
            GETWORD();
            GETWORD();
            constExp();
            GETWORD();
            CHECKRBRACK();
            if (lexicalAnalyzer.checkLbrack()) {
                dimension++;
                GETWORD();
                GETWORD();
                constExp();
                GETWORD();
                CHECKRBRACK();
            }
        }
        // '='
        if (lexicalAnalyzer.checkAssign()) {
            GETWORD();
            GETWORD();
            initVal(dimension);
        }
        // <InitVal>
        PRINT("<VarDef>");
    }

    public void initVal(int dimension) throws MyException {
        if (dimension == 1) {
            exp();
        } else if (dimension == 2) {                //TODO calculate const value
            if (!currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            if (!currentWord.isRbrace()) {
                exp();
                PRINT("<InitVal>");
                while (lexicalAnalyzer.checkComma()) {
                    GETWORD();
                    GETWORD();
                    exp();

                    PRINT("<InitVal>");
                }
                GETWORD();
            }
            CHECKRBRACE();
        } else {
            if (!currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                if (!currentWord.isLbrace()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
                if (!currentWord.isRbrace()) {
                    exp();
                    PRINT("<InitVal>");
                    while (lexicalAnalyzer.checkComma()) {
                        GETWORD();
                        GETWORD();
                        exp();
                        PRINT("<InitVal>");
                    }
                    GETWORD();
                }
                CHECKRBRACE();
                PRINT("<InitVal>");
                GETWORD();
            } while (currentWord.isComma());
            CHECKRBRACE();
        }
        PRINT("<InitVal>");
    }

    public void constExp() throws MyException {  //TODO check const
        addExp();
        PRINT("<ConstExp>");
    }

    public void exp() throws MyException {
        addExp();
        PRINT("<Exp>");
    }

    // <AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp>
    // <AddExp> ::= <MulExp> { (+|−) <MulExp> }
    public void addExp() throws MyException {
        mulExp();

        while (lexicalAnalyzer.checkUnaryAdd()) {
            PRINT("<AddExp>");
            GETWORD();
            GETWORD();
            mulExp();
        }
        PRINT("<AddExp>");
    }

    // <MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp>
    // <MulExp> ::= <UnaryExp> {  (*|/|%) <UnaryExp>  }
    public void mulExp() throws MyException {
        unaryExp();

        while (lexicalAnalyzer.checkUnaryCal()) {
            PRINT("<MulExp>");
            GETWORD();
            GETWORD();
            unaryExp();
        }
        PRINT("<MulExp>");
    }

    //<UnaryExp> ::= <PrimaryExp> | <Ident> '(' [<FuncRParams>] ')' | <UnaryOp> <UnaryExp>
    public void unaryExp() throws MyException {
        if (currentWord.isIdent() && lexicalAnalyzer.checkFuncParam()) {
            GETWORD();
            if (!lexicalAnalyzer.checkRparent()) {
                GETWORD();
                funcRParams();
            }
            GETWORD();
            CHECKRPARENT();
        } else if (currentWord.isUnaryOp()) {
            unaryOp();
            GETWORD();
            unaryExp();
        } else {
            primaryExp();
        }
        PRINT("<UnaryExp>");
    }

    //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
    public void primaryExp() throws MyException {
        if (currentWord.isLparent()) {
            GETWORD();
            exp();
            GETWORD();
            CHECKRPARENT();
        } else if (currentWord.isIdent()) {
            lVal();
        } else if (currentWord.isNumber()) {
            number();
        } else {
            //TODO error
        }
        PRINT("<PrimaryExp>");
    }

    // <LVal> ::= <Ident> {'[' <Exp> ']'}
    public void lVal() throws MyException {
        CHECKIDENT();
        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            exp();
            GETWORD();
            if (!currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
        }
        if (lexicalAnalyzer.checkLbrack()) {
            GETWORD();
            GETWORD();
            exp();
            GETWORD();
            if (!currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
        }
        PRINT("<LVal>");
    }

    //<FuncRParams> → <Exp> { ',' <Exp> }
    public void funcRParams() throws MyException {
        exp();
        while (lexicalAnalyzer.checkComma()) {
            GETWORD();
            GETWORD();
            exp();
        }
        PRINT("<FuncRParams>");
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
        throw new MyException(errorCode, errorLine);
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
            ERROR(103, currentLine);
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


}
