import java.util.ArrayList;
import java.util.HashMap;

public class Intermediate {
    AbstractSyntaxTree ast;
    HashMap<Integer, ArrayList<Integer>> tree;
    ArrayList<Quadruple> quadruples = new ArrayList<>();
    int cnt = 0;

    public Intermediate(AbstractSyntaxTree ast) {
        this.ast = ast;
        this.tree = ast.ast;
    }

    public void addQuadruple(String op, String dst, String src1, String src2) {
        Quadruple quadruple = new Quadruple(op, dst, src1, src2);
        this.quadruples.add(quadruple);
    }

    public void Traversal(int node) {
        //System.out.println(node);
        ArrayList<Integer> list = tree.get(node);
        if (list.size() != 0) {
            for (int i : list) {
                cnt++;
                Traversal(i);
                cnt--;
            }
        }

        System.out.println(ast.getById(node));
    }

    
}
