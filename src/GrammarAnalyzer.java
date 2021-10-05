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
            GETWORD();
            Program();
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
            GETWORD();
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
            System.out.println("=======" + e.errorLine + "======" + e.errorCode);

        }
    }

    public void funcDef() throws MyException {
        //<FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
        String funcType = currentWord.getType();

        GETWORD();
        CHECKIDENT();
        GETWORD();
        if (currentWord.isLparent()) {
            if (lexicalAnalyzer.checkRparent()) {
                GETWORD();
                GETWORD();
            } else {
                funcFParams();
                CHECKRPARENT();
                GETWORD();
            }
        } else {
            ERROR(100, currentLine);
        }
        block();
        PRINT("<FuncDef>");
    }

    public void mainFuncDef() throws MyException {
        //<MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
        GETWORD();
        CHECKLPARENT();
        GETWORD();
        CHECKRPARENT();
        GETWORD();
        block();
        PRINT("<MainFuncDef>");
    }

    public void funcFParams() throws MyException {
        //<FuncFParams> ::= <FuncFParam> { ',' <FuncFParam> }
        do {
            GETWORD();
            funcFParam();
            GETWORD();
        } while (currentWord.isComma());
        PRINT("<FuncFParams>");
    }

    public void funcFParam() throws MyException {
        //<FuncFParam> ::= <BType> <Ident> ['[' ']' { '[' <ConstExp> ']' }]
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

    public void block() throws MyException {
        //<Block> ::= '{' { <BlockItem> } '}'
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
                    GETWORD();
                    CHECKSEMI();
                }
            } catch (MyException e) {
                currentWord = lexicalAnalyzer.getByIndex(saveIndex);
                exp();
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

    public void whileStatement() throws MyException {
        // 'while' '(' <Cond> ')' <Stmt>
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
            if (!lexicalAnalyzer.checkRparent()) {
                funcRParams();
            } else {
                GETWORD();
            }
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

    public void funcRParams() throws MyException {
        //<FuncRParams> → <Exp> { ',' <Exp> }
        do {
            GETWORD();
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
        if (output) {
            try {
                //System.out.println(str);
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
