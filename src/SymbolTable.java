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

    public void addSymbol(Symbol symbol) {
        this.symbols.add(symbol);
    }


    public static class Symbol {
        String name;
        String type;
        String value;
        int initVal;

        public Symbol(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public Symbol(String name, String type, int initVal) {
            this.name = name;
            this.type = type;
            this.initVal = initVal;
        }

    }


}

