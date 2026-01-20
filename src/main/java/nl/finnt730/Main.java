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
//        system::println("--- Arrays Test ---")
//        
//        // Array creation
//        var arr = [10, 20, 30,15,1,65,1,156,156,156,56,48,456,48,156,48,156,4894,651,486,489,4621,864,651,894,651,6465,41]
//        system::println("Original array: " + arr)
//        
//        // Array access
//        int val = arr[1]
//        system::println("Value at index 1: " + val)
//        
//        // Array assignment
//        arr[1] = 99
//        system::println("Modified array: " + arr)
//        system::println("New value at index 1: " + arr[1])
//        
//        // Arrays of arrays
//        var matrix = [[1, 2], [3, 4]]
//        system::println("Matrix: " + matrix)
//        
//        // Accessing nested array
//        var row0 = matrix[0]
//        system::println("Row 0: " + row0)
//        
//        // Chained access
//        int cell = matrix[1][0]
//        system::println("Cell [1][0]: " + cell)
//        
//        // Setting nested array value
//        matrix[1][0] = 55
//        
//        debug matrix
//        foreach([]Int m : matrix[0]) {
//            system::println("Row: " + m)
//        }
//        
//        system::println("Modified Matrix: " + matrix)
//        system::println("Cell [1][0] is now: " + matrix[1][0])
//
//        // Add to list
//        var list = [1, 2]
//        list.add(3)
//        system::println("List after add: " + list)
//        system::println("Size: " + list.size())


         var str = "println(123)"
         var chatArr = str.toCharArray()
         var instr = []
         int index = 0
         var part = ""
         foreach(Char c : chatArr) {
//             system::println(c)
            part = part + c
            if(part == "println") {
                instr.add(part)
                part = ""
            }
//            if(part.endsWith("(") || part.endsWith(")")) {
//                instr.add(part.substring(0, part.length()-1))
//                instr.add(part.substring(part.length()-1, part.length()))
//                part = ""
//            }
        }
        debug instr
         
    }
}
                """;

        try {

            long start = System.currentTimeMillis();

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

            long end = System.currentTimeMillis();
            System.out.println("Execution time: " + (end - start) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}