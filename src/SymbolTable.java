import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    ArrayList<Symbol> symbols = new ArrayList<>();

    public SymbolTable() {
    }

    public boolean search(String ident) {
        for (Symbol symbol : symbols) {
            if (symbol.name.equals(ident)) {
                return true;
            }
        }
        return false;
    }

    public String getType(String ident) {
        for (Symbol symbol : symbols) {
            if (symbol.name.equals(ident)) {
                return symbol.type;
            }
        }
        return "";
    }

    public int getDimension(String ident) {
        for (Symbol symbol : symbols) {
            if (symbol.name.equals(ident)) {
                return symbol.dimension;
            }
        }
        return 0;
    }

    public void addSymbol(Symbol symbol) {
        this.symbols.add(symbol);
    }


    public static class Symbol {
        String name;
        String type;
        String value;
        int dimension;
        int initVal;

        public Symbol(String name, String type, int initVal, int dimension) {
            this.name = name;
            this.type = type;
            this.initVal = initVal;
            this.dimension = dimension;
        }

    }


}

