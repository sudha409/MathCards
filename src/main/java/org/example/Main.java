package org.example;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    static class Attempt {
        int a, b, expected; char op; Integer userAnswer; boolean answered;
        Attempt(int a, int b, char op, int expected) {
            this.a = a;
            this.b = b;
            this.op = op;
            this.expected = expected;
            this.userAnswer = null;
            this.answered = false;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Math Cards - Timed Quiz");

        System.out.print("Total duration in seconds (default 60): ");
        int durationSec = readIntOrDefault(scanner, 60);

        System.out.print("Maximum number of questions (default 100, ignored if time ends first): ");
        int maxQuestions = readIntOrDefault(scanner, 100);

        System.out.print("Choose operations (enter any of + - * / without spaces, e.g. +-*/ or press Enter for all): ");
        String opsLine = scanner.nextLine().trim();
        Set<Character> ops = parseOps(opsLine);
        if (ops.isEmpty()) ops.addAll(Arrays.asList('+', '-', '*', '/'));

        Random rand = new Random();
        List<Attempt> attempts = new ArrayList<>();

        ExecutorService exec = Executors.newSingleThreadExecutor();

        long endTime = System.currentTimeMillis() + durationSec * 1000L;
        int q = 1;
        while (System.currentTimeMillis() < endTime && q <= maxQuestions) {
            char op = pickOp(ops, rand);
            int a, b, expected;
            switch (op) {
                case '+':
                    a = rand.nextInt(50) + 1;
                    b = rand.nextInt(50) + 1;
                    expected = a + b;
                    break;
                case '-':
                    a = rand.nextInt(50) + 1;
                    b = rand.nextInt(50) + 1;
                    if (b > a) { int t = a; a = b; b = t; }
                    expected = a - b;
                    break;
                case '*':
                    a = rand.nextInt(13) + 1;
                    b = rand.nextInt(13) + 1;
                    expected = a * b;
                    break;
                case '/':
                default:
                    b = rand.nextInt(12) + 1;
                    int quotient = rand.nextInt(12) + 1;
                    a = b * quotient;
                    expected = quotient;
                    break;
            }

            Attempt att = new Attempt(a, b, op, expected);
            attempts.add(att);

            System.out.printf("Q%d: %d %c %d = ?\n", q, a, op, b);

            long remainingMs = endTime - System.currentTimeMillis();
            if (remainingMs <= 0) break;

            Callable<String> readTask = () -> {
                try { return scanner.nextLine(); } catch (Exception e) { return ""; }
            };
            Future<String> future = exec.submit(readTask);
            try {
                String line = future.get(remainingMs, TimeUnit.MILLISECONDS).trim();
                att.answered = true;
                try { att.userAnswer = Integer.parseInt(line); } catch (NumberFormatException nfe) { att.userAnswer = null; }
            } catch (TimeoutException te) {
                future.cancel(true);
                break;
            } catch (InterruptedException | ExecutionException e) {
                future.cancel(true);
                break;
            }

            q++;
        }

        exec.shutdownNow();

        int total = attempts.size();
        int correct = 0;
        int answered = 0;
        List<Attempt> wrongList = new ArrayList<>();
        for (Attempt a : attempts) {
            if (a.answered && a.userAnswer != null) answered++;
            if (a.userAnswer != null && a.userAnswer == a.expected) {
                correct++;
            } else {
                wrongList.add(a);
            }
        }

        int wrong = total - correct;
        int score = correct * 2;

        System.out.println();
        System.out.println("--- Time's up / Quiz finished ---");
        System.out.println("Total questions shown: " + total);
        System.out.println("Answered questions: " + answered);
        System.out.println("Correct: " + correct);
        System.out.println("Wrong: " + wrong);
        System.out.println("Score (2 pts each): " + score);

        System.out.print("Review wrong answers? (y/N): ");
        String rev = scanner.nextLine().trim().toLowerCase();
        if (rev.equals("y") || rev.equals("yes")) {
            if (wrongList.isEmpty()) {
                System.out.println("No wrong answers to review — well done!");
            } else {
                System.out.println("--- Review Wrong Answers ---");
                int i = 1;
                for (Attempt a : wrongList) {
                    String your = a.answered ? (a.userAnswer == null ? "invalid" : "" + a.userAnswer) : "(no answer)";
                    System.out.printf("%d) %d %c %d = ? | your: %s | correct: %d\n", i++, a.a, a.op, a.b, your, a.expected);
                }
            }
        }

        System.out.println("Done. Goodbye.");
    }

    private static int readIntOrDefault(Scanner scanner, int def) {
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) return def;
        try { return Integer.parseInt(line); } catch (Exception e) { return def; }
    }

    private static Set<Character> parseOps(String s) {
        Set<Character> set = new LinkedHashSet<>();
        for (char c : s.toCharArray()) {
            if (c == '+' || c == '-' || c == '*' || c == '/') set.add(c);
        }
        return set;
    }

    private static char pickOp(Set<Character> ops, Random rand) {
        int i = rand.nextInt(ops.size());
        int k = 0;
        for (char c : ops) {
            if (k++ == i) return c;
        }
        return '+';
    }
}
