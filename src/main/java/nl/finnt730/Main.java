package nl.finnt730;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.HashMap;
import java.util.Map;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        String input = """
                
                var arr = [1,2,3,4,5,6,7,8,9,10];
                
                func mod(a, b) {
                    return a - (b * (a / b));
                }
                
                func printEvenNumbers(array) {
                    var i = 0;
                    while (i < 10) {
                        if (mod(array[i], 2) == 0) {
                            print(array[i]);
                        }
                        i = i + 1;
                    }
                }
                
                printEvenNumbers(arr);
                """;

        try {
            // Create lexer
            CharStream charStream = CharStreams.fromString(input);
            DLangLexer lexer = new DLangLexer(charStream);

            // Create parser
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            DLangParser parser = new DLangParser(tokens);

            // Parse
            ParseTree tree = parser.compileUnit();

            MathEvaluatorVisitor evaluator = new MathEvaluatorVisitor();
            evaluator.visit(tree);

            System.out.println("Input: " + input);
            System.out.println("Parse tree: " + tree.toStringTree(parser));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}