import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LexicalAnalyzer {
    ArrayList<Word> symbolList = new ArrayList<>();
    HashMap<String, String> reservedWords = new HashMap<>();
    int lineNum = 0;
    int i = 0;
    String line = null;
    boolean findQuo = false;

    public LexicalAnalyzer() {
        init();
    }

    public void analyse(ArrayList<String> lines) throws IOException {
        while (lineNum < lines.size()) {
            //TODO error if out of index
            line = lines.get(lineNum);
            lineNum++;
            i = 0;
            for (; i < line.length(); i++) {
                char c = line.charAt(i);
                if (findQuo) {
                    if (c == '*' && i < line.length() - 1 && line.charAt(i + 1) == '/') {
                        findQuo = false;
                        i++;
                    }
                } else if (isNonDigit(c)) {
                    getIdent();
                } else if (Character.isDigit(c)) {    //TODO error if start with 0
                    getInt();
                } else if (c == '"') {
                    getString();
                } else if (Character.isSpaceChar(c)) {
                    continue;


                } else {
                    getNote();
                }
            }
        }
        print();
    }

    public void getIdent() {
        char c = line.charAt(i);
        StringBuilder wordBuilder = new StringBuilder();
        while (isNonDigit(c) || Character.isDigit(c)) {
            wordBuilder.append(c);
            i++;
            if (i < line.length()) c = line.charAt(i);
            else break;
        }
        i--;
        String word = wordBuilder.toString();
        addToList(word, reservedWords.getOrDefault(word, "IDENFR"));
    }

    public void getInt() {
        char c = line.charAt(i);
        StringBuilder wordBuilder = new StringBuilder();
        while (Character.isDigit(c)) {
            wordBuilder.append(c);
            i++;
            if (i < line.length()) c = line.charAt(i);
            else break;
        }
        while (i < line.length() && !Character.isSpaceChar(line.charAt(i))) {
            if (!isNonDigit(line.charAt(i)) && !Character.isDigit(line.charAt(i))) break;
            i++;
        }
        i--;
        String word = wordBuilder.toString();
        addToList(word, "INTCON");
    }

    public void getString() {
        char c = line.charAt(i);
        StringBuilder wordBuilder = new StringBuilder();
        do {
            wordBuilder.append(c);
            c = line.charAt(++i);                            //TODO restrict of NormalChar
        } while (c != '"');                                  //TODO no matching "
        wordBuilder.append(c);
        String formatString = wordBuilder.toString();
        addToList(formatString, "STRCON");
    }

    public void getNote() {
        char c = line.charAt(i);
        switch (c) {
            case '!':
                if (i < line.length() - 1 && line.charAt(i + 1) == '=') {
                    i++;
                    addToList("!=", "NEQ");
                } else {
                    addToList("!", "NOT");
                }
                break;
            case '&':
                i++;   //TODO error if not followed with '&'
                addToList("&&", "AND");
                break;
            case '|':
                i++;   //TODO error if not followed with '|'
                addToList("||", "OR");
                break;
            case '+':
                addToList("+", "PLUS");
                break;
            case '-':
                addToList("-", "MINU");
                break;
            case '*':
                addToList("*", "MULT");
                break;
            case '/':
                if (i < line.length() - 1 && line.charAt(i + 1) == '/') {
                    i = line.length();
                } else if (i < line.length() - 1 && line.charAt(i + 1) == '*') {
                    i++;
                    findQuo = true;
                } else {
                    addToList("/", "DIV");
                }
                break;
            case '%':
                addToList("%", "MOD");
                break;
            case '<':
                if (i < line.length() - 1 && line.charAt(i + 1) == '=') {
                    i++;
                    addToList("<=", "LEQ");
                } else {
                    addToList("<", "LSS");
                }
                break;
            case '>':
                if (i < line.length() - 1 && line.charAt(i + 1) == '=') {
                    i++;
                    addToList(">=", "GEQ");
                } else {
                    addToList(">", "GRE");
                }
                break;
            case '=':
                if (i < line.length() - 1 && line.charAt(i + 1) == '=') {
                    i++;
                    addToList("==", "EQL");
                } else {
                    addToList("=", "ASSIGN");
                }
                break;
            case ';':
                addToList(";", "SEMICN");
                break;
            case ',':
                addToList(",", "COMMA");
                break;
            case '(':
                addToList("(", "LPARENT");
                break;
            case ')':
                addToList(")", "RPARENT");
                break;
            case '[':
                addToList("[", "LBRACK");
                break;
            case ']':
                addToList("]", "RBRACK");
                break;
            case '{':
                addToList("{", "LBRACE");
                break;
            case '}':
                addToList("}", "RBRACE");
                break;
            default:
                //TODO error;
                break;

        }
    }

    public void init() {
        reservedWords.put("main", "MAINTK");
        reservedWords.put("const", "CONSTTK");
        reservedWords.put("int", "INTTK");
        reservedWords.put("break", "BREAKTK");
        reservedWords.put("continue", "CONTINUETK");
        reservedWords.put("if", "IFTK");
        reservedWords.put("else", "ELSETK");
        reservedWords.put("while", "WHILETK");
        reservedWords.put("getint", "GETINTTK");
        reservedWords.put("printf", "PRINTFTK");
        reservedWords.put("return", "RETURNTK");
        reservedWords.put("void", "VOIDTK");
    }

    public boolean isNonDigit(char c) {
        return Character.isLetter(c) || c == '_';
    }

    public boolean isNoneZeroDigit(char c) {
        return Character.isDigit(c) && c != '0';
    }

    public void addToList(String value, String type) {
        Word current = new Word(value, type, lineNum);
        this.symbolList.add(current);
    }

    public void print() throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter("output.txt"));
        for (Word symbol : symbolList) {
            out.write(symbol.type + " " + symbol.value + "\n");
            //System.out.println(symbol.type + " " + symbol.value);
        }
        out.close();
    }

}