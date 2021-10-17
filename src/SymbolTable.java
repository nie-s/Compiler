import java.util.HashMap;

public class SymbolTable {
    HashMap<String, Symbol> symbols;

    public SymbolTable() {
        symbols = new HashMap<>();
    }

    public boolean search(String ident) {
        return symbols.containsKey(ident);
    }

}

