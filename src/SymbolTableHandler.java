import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class SymbolTableHandler {
    ArrayList<String> functions = new ArrayList<>();
    HashMap<Integer, SymbolTable> symbolTableList = new HashMap<>();


    public SymbolTableHandler() {
        SymbolTable globalSymbolTable = new SymbolTable();
        symbolTableList.put(0, globalSymbolTable);
    }

    public boolean searchInTable(String ident, int current) {
        for (int i : symbolTableList.keySet()) {
            if (i < current) {
                boolean find = symbolTableList.get(current).search(ident);
                if (find) {
                    return true;
                }
            }
        }
        return false;
    }

    public void createSymbolTable(int layer) {
        this.symbolTableList.put(layer, new SymbolTable());
    }

    public void addToTable(int layer, SymbolTable.Symbol symbol) {
        symbolTableList.get(layer).addSymbol(symbol);
    }

    public boolean searchFunc(String name) {
        return functions.contains(name);
    }

    public void addFunc(String name) {
        this.functions.add(name);
    }


}
