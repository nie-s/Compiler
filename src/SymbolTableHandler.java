import java.util.HashMap;

public class SymbolTableHandler {
    HashMap<String, SymbolTable> functions = new HashMap<>();
    HashMap<Integer, SymbolTable> symbolTableList = new HashMap<>();


    public SymbolTableHandler() {
        SymbolTable globalSymbolTable = new SymbolTable();
        symbolTableList.put(0, globalSymbolTable);
    }

    public boolean searchInTable(String ident, int current) {
        for (int i : symbolTableList.keySet()) {
            if (i <= current) {
                boolean find = symbolTableList.get(i).search(ident);
                if (find) {
                    return true;
                }
            }
        }
        return false;
    }

    public SymbolTable.Symbol getSymbol(String ident, int current) {
        for (int i : symbolTableList.keySet()) {
            if (i <= current) {
                boolean find = symbolTableList.get(i).search(ident);
                if (find) {
                    return symbolTableList.get(i).get(ident);
                }
            }
        }
        return null;
    }

    public boolean searchInCurrentLayer(String ident, int current) {
        return symbolTableList.get(current).search(ident);
    }

    public void createSymbolTable(int layer) {
        if (!symbolTableList.containsKey(layer)) this.symbolTableList.put(layer, new SymbolTable());
    }

    public void deleteSymbolTable(int layer) {
        this.symbolTableList.remove(layer);
    }

    public void addToTable(int layer, SymbolTable.Symbol symbol) {
        symbolTableList.get(layer).addSymbol(symbol);
    }

    public boolean searchFunc(String name) {
        return functions.containsKey(name);
    }

    public void addFunc(String name) {
        if (!functions.containsKey(name)) this.functions.put(name, new SymbolTable());
    }

    public void addFuncParam(String name, SymbolTable.Symbol symbol) {
        this.functions.get(name).addSymbol(symbol);
    }

    public String checkType(String name, int layer) {
        for (; layer >= 0; layer--) {
            if (symbolTableList.get(layer).search(name)) {
                return symbolTableList.get(layer).getType(name);
            }
        }
        return "";
    }

    public int checkDimension(String name, int layer) {
        for (; layer >= 0; layer--) {
            if (symbolTableList.get(layer).search(name)) {
                return symbolTableList.get(layer).getDimension(name);
            }
        }
        return 0;
    }

    public int getRangey(String name, int layer) {
        for (; layer >= 0; layer--) {
            if (symbolTableList.get(layer).search(name)) {
                if (symbolTableList.get(layer).get(name).dimension == 2) {
                    return symbolTableList.get(layer).get(name).rangey;
                } else if (symbolTableList.get(layer).get(name).dimension == 1) {
                    return symbolTableList.get(layer).get(name).rangex;
                } else {
                    return 0;
                }
            }
        }
        return 0;
    }

    public int getLayer(String name, int layer) {
        for (; layer >= 0; layer--) {
            if (symbolTableList.get(layer).search(name)) {
                return layer;
            }
        }

        return 0;
    }
}
