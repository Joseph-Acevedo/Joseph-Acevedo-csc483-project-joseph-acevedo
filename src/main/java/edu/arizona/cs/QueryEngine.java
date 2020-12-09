package edu.arizona.cs;

import org.tartarus.snowball.ext.PorterStemmer;

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
                    String[] answers = line.toLowerCase().split("\\|");
                    currQuestion.answers = new String[answers.length];
                    for (int i = 0; i < answers.length; i++) {
                        currQuestion.answers[i] = answers[i];
                    }

                    qaCount = 0;
                    questions.add(currQuestion);
                    currQuestion = new Question();
                    break;
            }
        }
    }

    public float testQuestions(WikiParser wiki) throws IOException {
        int numCorrect = 0;
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
                        writer.write(String.format("%d. *%s* (%.3f)\n", i + 1, result.doc.get("title"), result.score));
                        if (i == 0)
                            numCorrect++;
                        mmr += (1.0/(i + 1));
                        correct = true;
                        break;
                    }
                }
                if (!correct) {
                    writer.write(String.format("%d.  %s (%.3f)\n", i + 1, result.doc.get("title"), result.score));
                } else {
                    break;
                }
            }
        }
        System.out.printf("Answered %d/%d correct\n", numCorrect, this.questions.size());
        writer.close();
        mmr /= questions.size();
        return mmr;
    }

    /**
     * Tests the performance of the system when using different boost factors for modifying queries.
     *
     * @param wiki The system to modify
     * @throws IOException If the output file DNE
     */
    private void testBoostFactor(WikiParser wiki) throws IOException {
        float minBoost = 0.0f;
        float maxBoost = 2.0f;
        float boostStep = 0.1f;

        for (float currBoost = minBoost; currBoost < maxBoost; currBoost += boostStep) {
            WikiParser.BOOST_FACTOR = currBoost;
            System.out.printf("(%.1f, %.5f)\n", currBoost, testQuestions(wiki));
        }
    }

    public static void main(String[] args) {
        // Loads the index. If an index already exists it will load the index based on constants for including stop words and lemmatizing
        boolean buildIndex = false;
        WikiParser wiki = new WikiParser("C:\\Users\\Joseph\\Desktop\\wiki-subset-20140602", !buildIndex);
        QueryEngine qe = new QueryEngine();

        try {
            wiki.buildIndex();
            Scanner scanner = new Scanner(System.in);
            qe.createAnswers();
            // qe.testBoostFactor(wiki);
            float mmr = qe.testQuestions(wiki);
            System.out.println("MRR Score: " + mmr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
