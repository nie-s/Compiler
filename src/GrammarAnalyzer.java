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
                ConstDeclare();
            }

            //<VarDecl>
            if (!lexicalAnalyzer.checkFunc()) {
                varDeclare();
            } else {
                break;
            }
        }


        //<FuncDef>
        while (currentWord.isInt() || currentWord.isVoid()) {
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
            mainFuncDeclare();
        } catch (MyException e) {

        }

    }

    public void ConstDeclare() {
        try {
            GETWORD();
            if (!currentWord.isInt()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            do {
                constDefine();

            } while (currentWord.isComma());
            if (!currentWord.isSemiColon()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            PRINT("<ConstDecl>");
        } catch (MyException e) {
            //TODO error
        }
    }


    public void varDeclare() {
        // <VarDecl> ::= <BType> <VarDef> {,<VarDef>} ';'
        try {
            do {
                varDefine();
            } while (currentWord.isComma());
            if (!currentWord.isSemiColon()) {
                ERROR(100, currentLine);
            }
            GETWORD();
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
        GETWORD();
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
            if (currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                constExp();
                GETWORD();
                if (currentWord.isRbrack()) {
                    ERROR(100, currentLine);
                }
            }
            GETWORD();
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

    public void constExp() {
        addExp();
        PRINT("<ConstExp>");
    }

    public void constInitVal(int dimension) throws MyException {
        if (dimension == 1) {
            constExp();
        } else if (dimension == 2) {                //TODO calculate const value
            if (currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            do {
                constExp();
                GETWORD();
            } while (currentWord.isComma());
        } else {
            if (currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            do {
                if (currentWord.isLbrace()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
                do {
                    constExp();
                    GETWORD();
                } while (currentWord.isComma());
            } while (currentWord.isComma());
        }
        PRINT("<ConstInitVal>");
    }

    public void addExp() { //TODO addExp
        PRINT("<AddExp>");
    }

    public void varDefine() throws MyException {
        //<Ident> | <Ident> { '[' <ConstExp> ']' } '=' <InitVal>
        GETWORD();
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
            if (currentWord.isRbrack()) {
                ERROR(100, currentLine);
            }
            if (currentWord.isLbrack()) {
                dimension++;
                GETWORD();
                constExp();
                GETWORD();
                if (currentWord.isRbrack()) {
                    ERROR(100, currentLine);
                }
            }
            GETWORD();
        }
        // '='
        if (!currentWord.isAssign()) {
            ERROR(100, currentLine);
        }
        GETWORD();
        // <ConstInitVal>
        initVal(dimension);
        PRINT("<VarDef>");
    }

    public void initVal(int dimension) throws MyException {
        if (dimension == 1) {
            exp();
        } else if (dimension == 2) {                //TODO calculate const value
            if (currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            do {
                exp();
                GETWORD();
            } while (currentWord.isComma());
        } else {
            if (currentWord.isLbrace()) {
                ERROR(100, currentLine);
            }
            GETWORD();
            do {
                if (currentWord.isLbrace()) {
                    ERROR(100, currentLine);
                }
                GETWORD();
                do {
                    exp();
                    GETWORD();
                } while (currentWord.isComma());
            } while (currentWord.isComma());
        }
        PRINT("<InitVal>");
    }

    public void exp() {
        addExp();
        PRINT("<Exp>");
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
    }


}
