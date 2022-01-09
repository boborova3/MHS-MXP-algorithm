package timer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.*;

public class Pokus {

    public static void main(String[] args) {
        Set<String> a1 = new HashSet<>();
        a1.add("A(a)");
        a1.add("¬D(a)");
        a1.add("¬F(a)");

        Set<String> a2 = new HashSet<>();
        a2.add("C(a)");
        a2.add("E(a)");
        a2.add("¬B(a)");

        Set<String> a3 = new HashSet<>();
        a3.add("¬B(a)");
        a3.add("¬D(a)");
        a3.add("¬F(a)");

        Set<String> a4 = new HashSet<>();
        a4.add("C(a)");
        a4.add("¬B(a)");
        a4.add("¬F(a)");

        Set<String> a5 = new HashSet<>();
        a5.add("E(a)");
        a5.add("¬B(a)");
        a5.add("¬D(a)");

        Set<String> a6 = new HashSet<>();
        a6.add("A(a)");
        a6.add("E(a)");
        a6.add("¬D(a)");

        Set<String> a7 = new HashSet<>();
        a7.add("A(a)");
        a7.add("C(a)");
        a7.add("¬F(a)");

        List<Set<String>> explanations = new ArrayList<>();
        explanations.add(a1);
        explanations.add(a2);
        explanations.add(a3);
        explanations.add(a4);
        explanations.add(a5);
        explanations.add(a6);
        explanations.add(a7);


        String paths1 = "B(a) \n" +
                "E(a) \n" +
                "A(a) \n" +
                "D(a) \n" +
                "C(a) \n" +
                "F(a) \n" +
                "B(a) E(a) \n" +
                "B(a) A(a) \n" +
                "B(a) D(a) \n" +
                "B(a) C(a) \n" +
                "B(a) F(a) \n" +
                "E(a) B(a) \n" +
                "E(a) A(a) \n" +
                "E(a) D(a) \n" +
                "E(a) C(a) \n" +
                "E(a) F(a) \n" +
                "A(a) B(a) \n" +
                "A(a) E(a) \n" +
                "A(a) D(a) \n" +
                "A(a) C(a) \n" +
                "A(a) F(a) \n" +
                "D(a) B(a) \n" +
                "D(a) E(a) \n" +
                "D(a) A(a) \n" +
                "D(a) C(a) \n" +
                "D(a) F(a) \n" +
                "C(a) B(a) \n" +
                "C(a) E(a) \n" +
                "C(a) A(a) \n" +
                "C(a) D(a) \n" +
                "C(a) F(a) \n" +
                "F(a) B(a) \n" +
                "F(a) E(a) \n" +
                "F(a) A(a) \n" +
                "F(a) D(a) \n" +
                "F(a) C(a) \n" +
                "B(a) E(a) A(a) \n" +
                "B(a) E(a) D(a) \n" +
                "B(a) E(a) C(a) \n" +
                "B(a) E(a) F(a) \n" +
                "B(a) A(a) E(a) \n" +
                "B(a) A(a) D(a) \n" +
                "B(a) A(a) C(a) \n" +
                "B(a) A(a) F(a) \n" +
                "B(a) D(a) E(a) \n" +
                "B(a) D(a) A(a) \n" +
                "B(a) D(a) C(a) \n" +
                "B(a) D(a) F(a) \n" +
                "B(a) C(a) E(a) \n" +
                "B(a) C(a) A(a) \n" +
                "B(a) C(a) D(a) \n" +
                "B(a) C(a) F(a) \n" +
                "B(a) F(a) E(a) \n" +
                "B(a) F(a) A(a) \n" +
                "B(a) F(a) D(a) \n" +
                "B(a) F(a) C(a) \n" +
                "E(a) A(a) B(a) \n" +
                "E(a) A(a) ¬D(a) \n" +
                "E(a) A(a) C(a) \n" +
                "E(a) A(a) F(a) \n" +
                "E(a) D(a) B(a) \n" +
                "E(a) D(a) ¬A(a) \n" +
                "E(a) D(a) C(a) \n" +
                "E(a) D(a) F(a) \n" +
                "E(a) C(a) A(a) \n" +
                "E(a) C(a) D(a) \n" +
                "E(a) C(a) ¬B(a) \n" +
                "E(a) C(a) F(a) \n" +
                "E(a) F(a) B(a) \n" +
                "E(a) F(a) A(a) \n" +
                "E(a) F(a) D(a) \n" +
                "E(a) F(a) C(a) \n" +
                "A(a) D(a) B(a) \n" +
                "A(a) D(a) ¬E(a) \n" +
                "A(a) D(a) C(a) \n" +
                "A(a) D(a) F(a) \n" +
                "A(a) C(a) B(a) \n" +
                "A(a) C(a) E(a) \n" +
                "A(a) C(a) D(a) \n" +
                "A(a) C(a) ¬F(a) \n" +
                "A(a) F(a) B(a) \n" +
                "A(a) F(a) E(a) \n" +
                "A(a) F(a) D(a) \n" +
                "A(a) F(a) ¬C(a) \n" +
                "D(a) C(a) B(a) \n" +
                "D(a) C(a) E(a) \n" +
                "D(a) C(a) A(a) \n" +
                "D(a) C(a) F(a) \n" +
                "D(a) F(a) B(a) \n" +
                "D(a) F(a) E(a) \n" +
                "D(a) F(a) A(a) \n" +
                "D(a) F(a) C(a) \n" +
                "C(a) F(a) B(a) \n" +
                "C(a) F(a) E(a) \n" +
                "C(a) F(a) ¬A(a) \n" +
                "C(a) F(a) D(a) \n" +
                "B(a) E(a) A(a) ¬D(a) \n" +
                "B(a) E(a) A(a) C(a) \n" +
                "B(a) E(a) A(a) F(a) \n" +
                "B(a) E(a) D(a) ¬A(a) \n" +
                "B(a) E(a) D(a) C(a) \n" +
                "B(a) E(a) D(a) F(a) \n" +
                "B(a) E(a) C(a) A(a) \n" +
                "B(a) E(a) C(a) D(a) \n" +
                "B(a) E(a) C(a) F(a) \n" +
                "B(a) E(a) F(a) A(a) \n" +
                "B(a) E(a) F(a) D(a) \n" +
                "B(a) E(a) F(a) C(a) \n" +
                "B(a) A(a) D(a) ¬E(a) \n" +
                "B(a) A(a) D(a) C(a) \n" +
                "B(a) A(a) D(a) F(a) \n" +
                "B(a) A(a) C(a) E(a) \n" +
                "B(a) A(a) C(a) D(a) \n" +
                "B(a) A(a) C(a) ¬F(a) \n" +
                "B(a) A(a) F(a) E(a) \n" +
                "B(a) A(a) F(a) D(a) \n" +
                "B(a) A(a) F(a) ¬C(a) \n" +
                "B(a) D(a) C(a) E(a) \n" +
                "B(a) D(a) C(a) A(a) \n" +
                "B(a) D(a) C(a) F(a) \n" +
                "B(a) D(a) F(a) E(a) \n" +
                "B(a) D(a) F(a) A(a) \n" +
                "B(a) D(a) F(a) C(a) \n" +
                "B(a) C(a) F(a) E(a) \n" +
                "B(a) C(a) F(a) ¬A(a) \n" +
                "B(a) C(a) F(a) D(a) \n" +
                "E(a) A(a) F(a) B(a) \n" +
                "E(a) A(a) F(a) ¬D(a) \n" +
                "E(a) A(a) F(a) C(a) \n" +
                "E(a) D(a) ¬A(a) ¬B(a) \n" +
                "E(a) D(a) ¬A(a) C(a) \n" +
                "E(a) D(a) ¬A(a) F(a) \n" +
                "E(a) D(a) C(a) A(a) \n" +
                "E(a) D(a) C(a) ¬B(a) \n" +
                "E(a) D(a) C(a) F(a) \n" +
                "E(a) D(a) F(a) B(a) \n" +
                "E(a) D(a) F(a) ¬A(a) \n" +
                "E(a) D(a) F(a) C(a) \n" +
                "E(a) C(a) F(a) A(a) \n" +
                "E(a) C(a) F(a) D(a) \n" +
                "E(a) C(a) F(a) ¬B(a) \n" +
                "A(a) D(a) ¬E(a) B(a) \n" +
                "A(a) D(a) ¬E(a) ¬F(a) \n" +
                "A(a) D(a) ¬E(a) C(a) \n" +
                "A(a) D(a) C(a) B(a) \n" +
                "A(a) D(a) C(a) E(a) \n" +
                "A(a) D(a) C(a) ¬F(a) \n" +
                "A(a) D(a) F(a) B(a) \n" +
                "A(a) D(a) F(a) E(a) \n" +
                "A(a) D(a) F(a) ¬C(a) \n" +
                "A(a) F(a) ¬C(a) B(a) \n" +
                "A(a) F(a) ¬C(a) E(a) \n" +
                "A(a) F(a) ¬C(a) ¬D(a) \n" +
                "D(a) C(a) F(a) B(a) \n" +
                "D(a) C(a) F(a) E(a) \n" +
                "D(a) C(a) F(a) ¬A(a) \n" +
                "C(a) F(a) ¬A(a) D(a) \n" +
                "C(a) F(a) ¬A(a) ¬B(a) \n" +
                "C(a) F(a) ¬A(a) ¬E(a) \n" +
                "B(a) E(a) A(a) F(a) ¬D(a) \n" +
                "B(a) E(a) A(a) F(a) C(a) \n" +
                "B(a) E(a) D(a) ¬A(a) ¬C(a) \n" +
                "B(a) E(a) D(a) ¬A(a) F(a) \n" +
                "B(a) E(a) D(a) C(a) A(a) \n" +
                "B(a) E(a) D(a) C(a) F(a) \n" +
                "B(a) E(a) D(a) F(a) ¬A(a) \n" +
                "B(a) E(a) D(a) F(a) C(a) \n" +
                "B(a) E(a) C(a) F(a) A(a) \n" +
                "B(a) E(a) C(a) F(a) D(a) \n" +
                "B(a) A(a) D(a) ¬E(a) ¬F(a) \n" +
                "B(a) A(a) D(a) ¬E(a) C(a) \n" +
                "B(a) A(a) D(a) C(a) E(a) \n" +
                "B(a) A(a) D(a) C(a) ¬F(a) \n" +
                "B(a) A(a) D(a) F(a) E(a) \n" +
                "B(a) A(a) D(a) F(a) ¬C(a) \n" +
                "B(a) A(a) F(a) ¬C(a) E(a) \n" +
                "B(a) A(a) F(a) ¬C(a) ¬D(a) \n" +
                "B(a) D(a) C(a) F(a) E(a) \n" +
                "B(a) D(a) C(a) F(a) ¬A(a) \n" +
                "B(a) C(a) F(a) ¬A(a) D(a) \n" +
                "B(a) C(a) F(a) ¬A(a) ¬E(a) \n" +
                "E(a) D(a) ¬A(a) C(a) ¬B(a) \n" +
                "E(a) D(a) ¬A(a) C(a) F(a) \n" +
                "E(a) D(a) ¬A(a) F(a) ¬B(a) \n" +
                "E(a) D(a) ¬A(a) F(a) C(a) \n" +
                "E(a) D(a) C(a) F(a) A(a) \n" +
                "E(a) D(a) C(a) F(a) ¬B(a) \n" +
                "A(a) D(a) ¬E(a) C(a) ¬B(a) \n" +
                "A(a) D(a) ¬E(a) C(a) ¬F(a) \n" +
                "A(a) D(a) F(a) E(a) ¬B(a) \n" +
                "A(a) D(a) F(a) E(a) C(a) \n" +
                "A(a) D(a) F(a) ¬C(a) E(a) \n" +
                "A(a) D(a) F(a) ¬C(a) ¬B(a) \n" +
                "A(a) F(a) ¬C(a) E(a) ¬D(a) \n" +
                "A(a) F(a) ¬C(a) E(a) ¬B(a) \n" +
                "D(a) C(a) F(a) ¬A(a) ¬B(a) \n" +
                "D(a) C(a) F(a) ¬A(a) ¬E(a) \n" +
                "C(a) F(a) ¬A(a) ¬E(a) D(a) \n" +
                "C(a) F(a) ¬A(a) ¬E(a) ¬B(a) \n" +
                "B(a) E(a) D(a) ¬A(a) ¬C(a) ¬F(a) \n" +
                "B(a) E(a) D(a) ¬A(a) F(a) ¬C(a) \n" +
                "B(a) E(a) D(a) C(a) F(a) A(a) \n" +
                "B(a) A(a) D(a) ¬E(a) C(a) ¬F(a) \n" +
                "B(a) A(a) D(a) F(a) E(a) C(a) \n" +
                "B(a) A(a) D(a) F(a) ¬C(a) E(a) \n" +
                "B(a) A(a) F(a) ¬C(a) E(a) ¬D(a) \n" +
                "B(a) D(a) C(a) F(a) ¬A(a) ¬E(a) \n" +
                "B(a) C(a) F(a) ¬A(a) ¬E(a) D(a) \n" +
                "E(a) D(a) ¬A(a) C(a) F(a) ¬B(a) \n" +
                "A(a) D(a) ¬E(a) C(a) ¬B(a) ¬F(a) \n" +
                "A(a) D(a) F(a) E(a) ¬B(a) C(a) \n" +
                "A(a) D(a) F(a) ¬C(a) E(a) ¬B(a) \n" +
                "A(a) D(a) F(a) ¬C(a) ¬B(a) E(a) \n" +
                "A(a) F(a) ¬C(a) E(a) ¬B(a) ¬D(a) \n" +
                "D(a) C(a) F(a) ¬A(a) ¬E(a) ¬B(a) ";

        String[] temp = paths1.split("\n");
        List<Set<String>> result = new ArrayList<>();
        for(String i : temp){
            String i1 = i.trim();
            String[] temp2 = i1.split(" ");
            Set<String> set = new HashSet<>(Arrays.asList(temp2));
            result.add(set);
        }

        for(Set<String> i : explanations){
            for(Set<String> j : result){
                if(j.containsAll(i)){
                    System.out.print("TOTO SA NACHADZA V PATHS: ");
                    for(String k : j){
                        System.out.print(k);
                    }
                    System.out.print("    ");

                    System.out.print("IDE O VYSVETLENIE: ");
                    for(String k : i){
                        System.out.print(k);
                    }
                    System.out.println();
                }
            }
        }


    }

}
