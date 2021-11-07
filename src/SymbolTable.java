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

    public Symbol get(String ident) {
        for (Symbol symbol : symbols) {
            if (symbol.name.equals(ident)) {
                return symbol;
            }
        }
        return null;
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
        ArrayList<Integer> value_1;
        ArrayList<ArrayList<Integer>> value_2;
        int value_0;
        int dimension;
        boolean isConst;
        int rangex;
        int rangey;

        public Symbol(String name, String type, boolean isConst, int value) {
            this.name = name;
            this.type = type;
            this.value_0 = value;
            this.dimension = 0;
            this.isConst = isConst;
        }


        public Symbol(String name, String type, boolean isConst, ArrayList<Integer> value) {
            this.name = name;
            this.type = type;
            this.value_1 = value;
            this.dimension = 1;
            this.isConst = isConst;

        }

        public Symbol(String name, String type, boolean isConst, ArrayList<ArrayList<Integer>> value, int rangx, int rangy) {
            this.name = name;
            this.type = type;
            this.value_2 = value;
            this.dimension = 2;
            this.rangex = rangx;
            this.rangey = rangy;
            this.isConst = isConst;

        }

    }


}

