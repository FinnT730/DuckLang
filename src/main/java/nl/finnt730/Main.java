package nl.finnt730;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        String input = """
main :: class() {

    main :: func(args: []String) {
        system::println("Hello World!")

        int x = 10
        int y = 20
        int sum = x + y

        system::println("The sum of " + x + " and " + y + " is " + sum)
        
        static::dummy()
    }

    static dummy :: func() {
        String dummy = "Hello World, this is a function"
        // toCharArray() is a runtime function that converts a String to a char array
        var arr = dummy.toCharArray()
        foreach(char c : arr) {
            if(c == ' ') {
                system::println("Space found!")
            } else {
                system::println(c)
            }
        }
    }

}
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

            LangEvaluatorVisitor2 evaluator = new LangEvaluatorVisitor2();
            evaluator.visit(tree);

            System.out.println("Execution finished.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}