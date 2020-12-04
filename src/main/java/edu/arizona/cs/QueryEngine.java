package edu.arizona.cs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class QueryEngine {

    private String qFilePath;
    private List<Question> questions;

    public QueryEngine(String qFilePath) {
        this.qFilePath = qFilePath;
    }

    public void createAnswers() throws FileNotFoundException {
        questions = new ArrayList();
        Scanner qaScanner = new Scanner(new File(this.qFilePath));
        Question currQuestion = new Question();

        int qaCount = 0;
        while (qaScanner.hasNextLine()) {
            String line = qaScanner.nextLine().trim();
            if (line.isEmpty())
                continue;

            switch (qaCount) {
                case 0:
                    currQuestion.category = line;
                    qaCount++;
                    break;
                case 1:
                    currQuestion.question = line;
                    qaCount++;
                    break;
                case 2:
                    currQuestion.answer = line;
                    qaCount = 0;
                    questions.add(currQuestion);
                    System.out.printf("%s\n%s\n%s\n\n", currQuestion.category, currQuestion.question, currQuestion.answer);
                    currQuestion = new Question();
                    break;
            }
        }
    }

    public void testQuestions(WikiParser wiki) throws IOException {
        for (Question q : this.questions) {
            System.out.printf("Question: %s\nAnswer: %s\nResults:", q.question, q.answer);
            // wiki.answerQuestion(q.question);
        }
    }

    public static void main(String[] args) {
        WikiParser wiki = new WikiParser("C:\\Users\\Joseph\\Desktop\\wiki-subset-20140602", true);
        QueryEngine qe = new QueryEngine("C:\\Users\\Joseph\\git\\csc483-project-joseph-acevedo\\src\\main\\resources\\questions.txt");

        try {
            wiki.buildIndex();
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Question: ");
                String input = scanner.nextLine().trim();
                wiki.answerQuestion(input);
            }
            // qe.createAnswers();
            // qe.testQuestions(wiki);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
