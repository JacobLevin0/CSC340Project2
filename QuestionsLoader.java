import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class QuestionsLoader {

    private static final String questions_file = "Questions.txt";

    private Map<Integer, QuestionInfo> questions = new HashMap<>();

    // Embedded class
    public static class QuestionInfo {

        int questionNumber;
        String question;
        String option1;
        String option2;
        String option3;
        String option4;
        String answer;

        public QuestionInfo(String question, String option1, String option2, String option3, String option4,
                String answer) {
            this.question = question;
            this.option1 = option1;
            this.option2 = option2;
            this.option3 = option3;
            this.option4 = option4;
            this.answer = answer;
        }

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
            System.out.println("Error reading file" + e.getMessage());
        }
    }

    public Map<Integer, QuestionInfo> getQuestions() {
        return questions;
    }

    public void printQuestions() {
        if (questions.isEmpty()) {
            System.out.println("No questions");
        }

        else {
            System.out.println(" Loaded questions and options and anwers");
            for (Entry<Integer, QuestionsLoader.QuestionInfo> entry : questions.entrySet()) {
                QuestionInfo questionInfo = entry.getValue();
                System.out.println("Question number : " + entry.getKey() +
                        "| question : " + questionInfo.question +
                        "| option1 : " + questionInfo.option1 + "| option2 : " + questionInfo.option2 +
                        "| option3 : " + questionInfo.option3 +
                        "| option4 : " + questionInfo.option4 + "| answer : " + questionInfo.answer);

            }
        }

    }

    public static void main(String[] args) {
        QuestionsLoader questionsLoader = new QuestionsLoader();

        questionsLoader.readFIle();
        questionsLoader.printQuestions();

    }

}
