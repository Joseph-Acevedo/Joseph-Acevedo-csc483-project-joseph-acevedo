package edu.arizona.cs;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class QueryEngine {

    private static final String ANSWERS_OUTPUT_FILEPATH = "C:\\Users\\Joseph\\git\\csc483-project-joseph-acevedo\\src\\main\\resources\\output.txt";
    private static final String QUESTION_FILEPATH = "C:\\Users\\Joseph\\git\\csc483-project-joseph-acevedo\\src\\main\\resources\\questions.txt";
    private List<Question> questions;

    public void createAnswers() throws FileNotFoundException {
        questions = new ArrayList();
        Scanner qaScanner = new Scanner(new File(QUESTION_FILEPATH));
        Question currQuestion = new Question();

        int qaCount = 0;
        while (qaScanner.hasNextLine()) {
            String line = qaScanner.nextLine().toLowerCase().trim();
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
                    String[] answers = line.split("\\|");
                    currQuestion.answers = new String[answers.length];
                    for (int i = 0; i < answers.length; i++) {
                        currQuestion.answers[i] = answers[i];
                    }
                    System.out.printf("Question: %s\nAnswers:\n", currQuestion.question);
                    for (String answer : currQuestion.answers) {
                        System.out.println("\t"+answer);
                    }
                    qaCount = 0;
                    questions.add(currQuestion);
                    currQuestion = new Question();
                    break;
            }
        }
    }

    public float testQuestions(WikiParser wiki) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(ANSWERS_OUTPUT_FILEPATH));
        float mmr = 0;
        for (Question q : this.questions) {
            List<ResultClass> results = wiki.answerQuestion(q.question.trim());

            writer.write("\nQuestion: " + q.question + "\n");
            boolean correct = false;
            for (int i = 0; i < results.size(); i++) {
                ResultClass result = results.get(i);
                for (String answer : q.answers) {
                    if (result.doc.get("title").equalsIgnoreCase(answer)) {
                        writer.write(String.format("%d. *%s*\n", i + 1, result.doc.get("title")));
                        mmr += (1.0/(i + 1));
                        correct = true;
                        break;
                    }
                }
                if (!correct) {
                    writer.write(String.format("%d.  %s \n", i + 1, result.doc.get("title")));
                } else {
                    break;
                }
            }
        }
        writer.close();
        mmr /= questions.size();
        return mmr;
    }

    public static void main(String[] args) {
        WikiParser wiki = new WikiParser("C:\\Users\\Joseph\\Desktop\\wiki-subset-20140602", true);
        QueryEngine qe = new QueryEngine();

        try {
            wiki.buildIndex();
            Scanner scanner = new Scanner(System.in);
            qe.createAnswers();
            float mmr = qe.testQuestions(wiki);
            System.out.println("MMR Score: " + mmr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
