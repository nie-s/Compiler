public class GrammarAnalyzer {
    LexicalAnalyzer lexicalAnalyzer;
    Word currentWord = new Word();
    int currentLine = 0;
    boolean output = true;

    public GrammarAnalyzer(LexicalAnalyzer lexicalAnalyzer) {
        this.lexicalAnalyzer = lexicalAnalyzer;
    }

    public void analyse() {
        GETWORD();
        Program();
    }

    //<CompUnit> ::= {<Decl>} {<FuncDef>} <MainFuncDef>
    //<Decl> ::= <ConstDecl> | <VarDecl>
    public void Program() {
        //{<Decl>}
        while (lexicalAnalyzer.hasWord()) {
            //<ConstDecl>
            if (currentWord.isConst()) {
                GETWORD();
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
                if (currentWord.isInt() || lexicalAnalyzer.checkMain()) {
                    break;
                } else if (currentWord.isInt() || currentWord.isVoid()) {
                    funcDef();
                } else {
                    ERROR(100, currentLine);
                }
            } catch (MyException e) {

            }
        }

        //<MainFuncDef>
        try {
            if (lexicalAnalyzer.hasWord())
                mainFuncDef();
        } catch (MyException e) {

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

    public void varDeclare() {
        // <VarDecl> ::= <BType> <VarDef> {,<VarDef>} ';'
        try {
            do {
                GETWORD();
                varDefine();
                GETWORD();
            } while (currentWord.isComma());
            CHECKSEMI();
            PRINT("<VarDecl>");
        } catch (MyException e) {

        }
    }

    public void funcDef() throws MyException {
        //<FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
        String funcType = currentWord.getType();

        GETWORD();
        CHECKIDENT();
        GETWORD();
        if (!currentWord.isLparent()) {
            funcRParams();
            GETWORD();
            CHECKRPARENT();
            GETWORD();
        }
        block();
        PRINT("<FuncDef>");
    }

    public void mainFuncDef() throws MyException {
        //<MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
        GETWORD();
        GETWORD();
        CHECKLPARENT();
        CHECKRPARENT();
        block();
        PRINT("<MainFuncDef>");
    }

    public void block() throws MyException {
        //<Block> ::= '{' { <BlockItem> } '}'
        if (!currentWord.isLbrace()) {
            ERROR(100, currentLine);
        }
        blockItem();
        GETWORD();
        CHECKRBRACE();

        PRINT("<Block>");
    }

    public void blockItem() throws MyException {
        //<BlockItem> ::= <Decl> | <Stmt>
        if (currentWord.isConst()) {
            constDeclare();
        } else if (currentWord.isInt()) {
            varDeclare();
        } else {
            stmt();
        }
    }

    public void stmt() throws MyException {
        /* <Stmt> ::= <LVal> '=' <Exp> ';'
                     | [Exp] ';' //有⽆Exp两种情况
                     | <Block>
                     | 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
                     | 'while' '(' <Cond> ')' <Stmt>
                     | 'break;' | 'continue;'
                     | 'return' [<Exp>] ';'
                     | <LVal> = 'getint();'
                     | 'printf('FormatString{,<Exp>}');'

         */
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
                exp();
                GETWORD();
            }
            CHECKRPARENT();
            GETWORD();
            CHECKSEMI();
        } else if (currentWord.isSemiColon()) {

        } else if (currentWord.isLbrace()) {
            // <Block>
            GETWORD();
            block();
            GETWORD();
            CHECKRBRACE();
        } else if (currentWord.isIdent()) {
            // <LVal> '=' <Exp> ';'
            //  [Exp] ';'
            //  <LVal> = 'getint();'
            GETWORD();
            if (currentWord.isAssign()) {
                if (lexicalAnalyzer.checkGetint()) {
                    GETWORD();
                    GETWORD();
                    CHECKLPARENT();
                    GETWORD();
                    CHECKRPARENT();
                    GETWORD();
                    CHECKSEMI();
                } else {
                    GETWORD();
                    exp();
                    GETWORD();
                    CHECKSEMI();
                }
            } else {
                exp();
                GETWORD();
                CHECKSEMI();
            }
        } else {
            ERROR(100, currentLine);
        }
        PRINT("<Stmt>");
    }

    public void ifStatement() throws MyException {
        // 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ]
        GETWORD();
        CHECKLPARENT();
        condition();
        CHECKRPARENT();
        stmt();
        if (lexicalAnalyzer.checkElse()) {
            stmt();
        }
    }

    public void whileStatement() throws MyException {
        // 'while' '(' <Cond> ')' <Stmt>
        GETWORD();
        CHECKLPARENT();
        condition();
        CHECKRPARENT();
        stmt();
    }

    public void condition() throws MyException {
        lOrExp();
        PRINT("<Cond>");
    }

    public void lOrExp() throws MyException {
        // <LOrExp> ::= <LAndExp> {'||' <LAndExp>}
        lAndExp();
        while (lexicalAnalyzer.checkOr()) {
            GETWORD();
            GETWORD();
            lAndExp();
        }
        PRINT("<LOrExp>");
    }

    public void lAndExp() throws MyException {
        // <LAndExp> ::= <EqExp> { && <EqExp> }
        eqExp();
        while (lexicalAnalyzer.checkAnd()) {
            GETWORD();
            GETWORD();
            eqExp();
        }
        PRINT("<LAndExp>");
    }

    public void eqExp() throws MyException {
        // <EqExp> ::= <RelExp> { (== | !=) <RelExp> }
        relExp();
        while (lexicalAnalyzer.checkEq()) {
            GETWORD();
            GETWORD();
            relExp();
        }

        PRINT("<EqExp>");
    }

    public void relExp() throws MyException {
        // <RelExp> ::= <AddExp>  { (< | > | <= | >=) <AddExp> }
        addExp();
        while (lexicalAnalyzer.checkRel()) {
            GETWORD();
            GETWORD();
            addExp();
        }
        PRINT("<RelExp>");
    }

    public void constDefine() throws MyException {
        //table
        // <ConstDef> ::= <Ident> { '[' <ConstExp> ']' } '=' <ConstInitVal>
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
            if (!currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                constExp();
                GETWORD();
                if (!currentWord.isRbrack()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
            }
        }
        // '='
//        GETWORD();
        if (!currentWord.isAssign()) {
            ERROR(100, currentLine);
        }
        GETWORD();
        // <ConstInitVal>
        constInitVal(dimension);
        PRINT("<ConstDef>");
    }

    public void constInitVal(int dimension) throws MyException {
        if (dimension == 1) {
            constExp();
        } else if (dimension == 2) {                //TODO calculate const value
            if (!currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                constExp();
                GETWORD();
            } while (currentWord.isComma());
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
                do {
                    GETWORD();
                    constExp();
                    GETWORD();
                } while (currentWord.isComma());
                CHECKRBRACE();
                GETWORD();
            } while (currentWord.isComma());
            CHECKRBRACE();
        }
        PRINT("<ConstInitVal>");
    }

    public void varDefine() throws MyException {
        //<Ident> | <Ident> { '[' <ConstExp> ']' } '=' <InitVal>
        //<Ident>
        CHECKIDENT();
        //table
        String ident = currentWord.getValue();
        int dimension = 1;
        //{ '[' <ConstExp> ']' }
        GETWORD();
        if (currentWord.isLbrack()) {
            dimension++;
            GETWORD();
            constExp();
            GETWORD();
            if (!currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                constExp();
                GETWORD();
                if (!currentWord.isRbrack()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
            }
        }
        // '='
        if (!currentWord.isAssign()) {
            ERROR(100, currentLine);
        }
        GETWORD();
        // <InitVal>
        initVal(dimension);
        PRINT("<VarDef>");
    }

    public void initVal(int dimension) throws MyException {
        if (dimension == 1) {
            exp();
        } else if (dimension == 2) {                //TODO calculate const value
            if (!currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                exp();
                GETWORD();
            } while (currentWord.isComma());
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
                do {
                    GETWORD();
                    exp();
                    GETWORD();
                } while (currentWord.isComma());
                CHECKRBRACE();
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

    public void addExp() throws MyException {
        //<AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp>
        // <AddExp> ::= <MulExp> { (+|−) <MulExp> }
        mulExp();

        while (lexicalAnalyzer.checkUnaryAdd()) {
            GETWORD();
            GETWORD();
            mulExp();
        }
        PRINT("<AddExp>");
    }

    public void mulExp() throws MyException {
        // <MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp>
        // <MulExp> ::= <UnaryExp> {  (*|/|%) <UnaryExp>  }
        unaryExp();

        while (lexicalAnalyzer.checkUnaryCal()) {
            GETWORD();
            GETWORD();
            unaryExp();
        }
        PRINT("<MulExp>");
    }

    public void unaryExp() throws MyException {
        //<UnaryExp> ::= <PrimaryExp> | <Ident> '(' [<FuncRParams>] ')' | <UnaryOp> <UnaryExp>
        if (lexicalAnalyzer.checkFuncParam()) {
            CHECKIDENT();
            GETWORD();
            GETWORD();
            funcRParams();
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

    public void primaryExp() throws MyException {
        //<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
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

    public void lVal() throws MyException {
        // <LVal> ::= <Ident> {'[' <Exp> ']'}
        CHECKIDENT();
        if (lexicalAnalyzer.checkArray()) {
            GETWORD();
            GETWORD();
            exp();
            GETWORD();
            if (currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
        }
        if (lexicalAnalyzer.checkArray()) {
            GETWORD();
            GETWORD();
            exp();
            GETWORD();
            if (currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
        }
        PRINT("<LVal>");
    }

    public void funcRParams() throws MyException {
        //<FuncRParams> → <Exp> { ',' <Exp> }
        do {
            exp();
            GETWORD();
        } while (currentWord.isComma());
        PRINT("<FuncRParams>");
    }

    public void unaryOp() throws MyException {
        //<UnaryOp> ::= + | - | !
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
        if (output) System.out.println(str);
    }

    public void GETWORD() {
        currentWord = lexicalAnalyzer.getWord();
        currentLine = currentWord.lineCnt;
        System.out.println(currentWord.type + " " + currentWord.value);
    }

    public void CHECKSEMI() throws MyException {
        if (!currentWord.isSemiColon()) {
            ERROR(100, currentLine);
        }
    }

    public void CHECKRBRACE() throws MyException {
        if (!currentWord.isRbrace()) {
            ERROR(100, currentLine);
        }
    }

    public void CHECKIDENT() throws MyException {
        if (!currentWord.isIdent()) {
            ERROR(100, currentLine);
        }
    }

    public void CHECKRPARENT() throws MyException {
        if (!currentWord.isRparent()) {
            ERROR(100, currentLine);
        }
    }

    public void CHECKLPARENT() throws MyException {
        if (!currentWord.isLparent()) {
            ERROR(100, currentLine);
        }
    }

}
