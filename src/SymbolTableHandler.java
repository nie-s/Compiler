import java.util.HashMap;

public class SymbolTableHandler {
    SymbolTable globalSymbolTable = new SymbolTable();
    HashMap<String, SymbolTable> localSymbolTableList = new HashMap<>();

    public SymbolTableHandler() {

    }

    public boolean searchInTable(String ident, String currentFunc) {
        if (currentFunc.equals("")) {
            return globalSymbolTable.search(ident);
        } else {
            return localSymbolTableList.get(currentFunc).search(ident);
        }
    }


}
