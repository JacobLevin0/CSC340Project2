import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The QuestionsLoader class is responsible for loading trivia questions
 * from a text file and storing them in a map for easy access.
 * It provides methods to read questions from a file, retrieve them,
 * and print them to the console.
 * 
 * The expected file format is:
 * 
 * <pre>
 * [questionNumber] [question] | [option1] | [option2] | [option3] | [option4] | [answer]
 * </pre>
 * 
 * Each line should be properly delimited by " | " and start with a question number and text.
 * 
 * @author Omar Fofana
 */
public class QuestionsLoader {

    private static final String questions_file = "Questions.txt";

    private Map<Integer, QuestionInfo> questions = new HashMap<>();

    /**
     * The QuestionInfo class represents a single trivia question
     * with four possible options and the correct answer.
     */
    public static class QuestionInfo {

        int questionNumber;
        String question;
        String option1;
        String option2;
        String option3;
        String option4;
        String answer;

        /**
         * Constructs a QuestionInfo without an explicit question number.
         * 
         * @param question The question text.
         * @param option1  First answer option.
         * @param option2  Second answer option.
         * @param option3  Third answer option.
         * @param option4  Fourth answer option.
         * @param answer   The correct answer.
         */
        public QuestionInfo(String question, String option1, String option2, String option3, String option4,
                            String answer) {
            this.question = question;
            this.option1 = option1;
            this.option2 = option2;
            this.option3 = option3;
            this.option4 = option4;
            this.answer = answer;
        }

        /**
         * Constructs a QuestionInfo with a question number.
         * 
         * @param questionNumber The number identifying the question.
         * @param question       The question text.
         * @param option1        First answer option.
         * @param option2        Second answer option.
         * @param option3        Third answer option.
         * @param option4        Fourth answer option.
         * @param answer         The correct answer.
         */
        public QuestionInfo(int questionNumber, String question, String option1, String option2, String option3,
                            String option4, String answer) {
            this.questionNumber = questionNumber;
            this.question = question;
            this.option1 = option1;
            this.option2 = option2;
            this.option3 = option3;
            this.option4 = option4;
            this.answer = answer;
        }
    }

    /**
     * Reads questions from the questions file and stores them in a map.
     * Each line is expected to be delimited by '|' and begin with a question number.
     */
    public void readFIle() {
        try (BufferedReader reader = new BufferedReader(new FileReader(questions_file))) {
            reader.readLine(); // Skip header line
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s*\\|\\s*", 6);
                if (parts.length < 6) {
                    System.out.println("Invalid format: " + line);
                    continue;
                }

                String[] first = parts[0].trim().split("\\s+", 2);
                if (first.length < 2) {
                    System.out.println("Invalid question number and question: " + parts[0]);
                    continue;
                }

                try {
                    int questionNumber = Integer.parseInt(first[0]);
                    String question = first[1];
                    String option1 = parts[1];
                    String option2 = parts[2];
                    String option3 = parts[3];
                    String option4 = parts[4];
                    String answer = parts[5];

                    questions.put(questionNumber,
                            new QuestionInfo(question, option1, option2, option3, option4, answer));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid question number: " + parts[0]);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Returns the map of questions loaded from the file.
     * 
     * @return A map where the key is the question number, and the value is a QuestionInfo object.
     */
    public Map<Integer, QuestionInfo> getQuestions() {
        return questions;
    }

    /**
     * Prints all the loaded questions, options, and answers to the console.
     */
    public void printQuestions() {
        if (questions.isEmpty()) {
            System.out.println("No questions");
        } else {
            System.out.println("Loaded questions, options, and answers:");
            for (Entry<Integer, QuestionsLoader.QuestionInfo> entry : questions.entrySet()) {
                QuestionInfo questionInfo = entry.getValue();
                System.out.println("Question number : " + entry.getKey() +
                        " | question : " + questionInfo.question +
                        " | option1 : " + questionInfo.option1 +
                        " | option2 : " + questionInfo.option2 +
                        " | option3 : " + questionInfo.option3 +
                        " | option4 : " + questionInfo.option4 +
                        " | answer : " + questionInfo.answer);
            }
        }
    }

    /**
     * The main method for testing file reading and printing questions.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        QuestionsLoader questionsLoader = new QuestionsLoader();
        questionsLoader.readFIle();
        questionsLoader.printQuestions();
    }
}