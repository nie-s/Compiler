import javax.swing.plaf.SplitPaneUI;

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
                ConstDeclare();
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
                } else if (currentWord.isInt()) {
                    intFuncDeclare();
                } else {
                    voidFuncDeclare();
                }
            } catch (MyException e) {
                if (e.getErrorCode() == 2) {
                    // currentFunction = "";
                    break;
                }
            }
        }

        //<MainFuncDef>
        try {
            if (lexicalAnalyzer.hasWord())
                mainFuncDeclare();
        } catch (MyException e) {

        }

    }

    public void ConstDeclare() {
        try {
            if (!currentWord.isInt()) {
                ERROR(100, currentLine);
            }
            do {
                GETWORD();
                constDefine();
                GETWORD();
            } while (currentWord.isComma());
            if (!currentWord.isSemiColon()) {
                ERROR(100, currentLine);
            }
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
            if (!currentWord.isSemiColon()) {
                ERROR(100, currentLine);
            }
            PRINT("<VarDecl>");
        } catch (MyException e) {

        }
    }

    public void voidFuncDeclare() throws MyException {

    }

    public void intFuncDeclare() throws MyException {

    }

    public void mainFuncDeclare() throws MyException {

    }

    public void constDefine() throws MyException {
        //table
        // <ConstDef> ::= <Ident> { '[' <ConstExp> ']' } '=' <ConstInitVal>
        //<Ident>
        if (!currentWord.isIdent()) {
            ERROR(100, currentLine);
        }
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
            if (!currentWord.isRbrace()) {
                ERROR(100, currentLine);
            }
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
                if (!currentWord.isRbrace()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
            } while (currentWord.isComma());
            if (!currentWord.isRbrace()) {
                ERROR(100, currentLine);
            }
        }
        PRINT("<ConstInitVal>");
    }

    public void varDefine() throws MyException {
        //<Ident> | <Ident> { '[' <ConstExp> ']' } '=' <InitVal>
        //<Ident>
        if (!currentWord.isIdent()) {
            ERROR(100, currentLine);
        }
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
            if (!currentWord.isRbrace()) {
                ERROR(100, currentLine);
            }
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
                if (!currentWord.isRbrace()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
            } while (currentWord.isComma());
            if (!currentWord.isRbrace()) {
                ERROR(100, currentLine);
            }
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
            if (!currentWord.isIdent()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            GETWORD();
            funcRParams();
            GETWORD();
            if (!currentWord.isRparent()) {
                ERROR(100, currentLine);
            }
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
            if (!currentWord.isRparent()) {
                ERROR(100, currentLine);
            }
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
        if (!currentWord.isIdent()) {
            ERROR(100, currentLine);
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


}
