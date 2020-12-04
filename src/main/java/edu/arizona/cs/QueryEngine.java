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

    public float testQuestions(WikiParser wiki) throws IOException {
        float mmr = 0;
        for (Question q : this.questions) {
            System.out.printf("Question: %s\nAnswer: %s\nResults:", q.question, q.answer);
            List<ResultClass> results = wiki.answerQuestion(q.question.trim(), new String[] {"text", "section"});

            for (int i = 0; i < results.size(); i++) {
                ResultClass result = results.get(i);
                if (result.doc.get("title").equalsIgnoreCase(q.answer)) {
                    System.out.println("Answer found at index " + i);
                    mmr += (1.0/(i + 1));
                    break;
                }
            }
        }
        mmr /= questions.size();
        return mmr;
    }

    public static void main(String[] args) {
        WikiParser wiki = new WikiParser("C:\\Users\\Joseph\\Desktop\\wiki-subset-20140602", true);
        QueryEngine qe = new QueryEngine("C:\\Users\\Joseph\\git\\csc483-project-joseph-acevedo\\src\\main\\resources\\questions.txt");

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
