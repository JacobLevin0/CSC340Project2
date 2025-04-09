//package project2;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;

/**
 * The ClientWindow class represents the GUI for a multiplayer trivia game client.
 * It handles displaying questions, answer options, a timer, and a score tracker.
 * It also allows the player to poll for questions and submit answers.
 * 
 * @author Omar Fofana
 */
public class ClientWindow implements ActionListener
{
	private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel question;
	private JLabel timer;
	private JLabel scoreLabel;
	private TimerTask clock;
	private boolean ableToAnswer;
	private int score;
	private String currAnswer;

	private Player player;
	private JFrame window;
	private static SecureRandom random = new SecureRandom();

	/**
	 * Constructs a new ClientWindow with the specified Player object.
	 * Sets up the user interface and initializes values.
	 * 
	 * @param player The Player object representing the current client.
	 */
	public ClientWindow(Player player)
	{
		this.player = player;
        ableToAnswer = false;
        score = 0;
        currAnswer = "";

        window = new JFrame("Trivia Game");
        JOptionPane.showMessageDialog(window, "Welcome to the Trivia Game!");

        // Question
        question = new JLabel("Waiting for question...");
        question.setBounds(10, 5, 380, 100);
        window.add(question);

        // Options
        options = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        for (int i = 0; i < options.length; i++) {
            options[i] = new JRadioButton("Option " + (i + 1));
            options[i].addActionListener(this);
            options[i].setBounds(10, 110 + (i * 30), 350, 30);
            window.add(options[i]);
            optionGroup.add(options[i]);
        }

        // Timer
        timer = new JLabel("TIMER");
        timer.setBounds(250, 250, 100, 20);
        window.add(timer);

        // Score
        scoreLabel = new JLabel("SCORE: 0");
        scoreLabel.setBounds(50, 250, 150, 20);
        window.add(scoreLabel);

        // Poll Button
        poll = new JButton("Poll");
        poll.setBounds(10, 300, 100, 30);
        poll.addActionListener(this);
        poll.setEnabled(true);
        window.add(poll);

        // Submit Button
        submit = new JButton("Submit");
        submit.setBounds(200, 300, 100, 30);
        submit.addActionListener(this);
        submit.setEnabled(false);
        window.add(submit);

        // Window Setup
        window.setSize(400, 400);
        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
	}

	/**
	 * Handles actions triggered by button and radio button clicks.
	 * 
	 * @param e The ActionEvent that was triggered.
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String input = e.getActionCommand();

        switch (input) {
            case "Poll":
                player.poll();
                break;
            case "Submit":
                if (ableToAnswer) {
                    for (JRadioButton rb : options) {
                        if (rb.isSelected()) {
                            currAnswer = rb.getText();
                            break;
                        }
                    }
                    if (!currAnswer.isEmpty()) {
                        player.createWriteThread("My Answer");
                        setStatus(false);
                        poll.setEnabled(false);
                        submit.setEnabled(false);
                    } else {
                        JOptionPane.showMessageDialog(window, "Please select an option first.");
                    }
                }
                break;
            default:
                // Radio buttons clicked
                currAnswer = input;
                break;
        }
	}
	
	/**
	 * A TimerTask responsible for updating the countdown timer on the UI.
	 * Ends polling and answering capability when time runs out.
	 * 
	 * @author Omar Fofana
	 */
	public class TimerCode extends TimerTask
	{
		private int duration;

		/**
		 * Constructs a TimerCode with a specified countdown duration.
		 * 
		 * @param duration The starting duration in seconds.
		 */
		public TimerCode(int duration)
		{
			this.duration = duration;
		}

		/**
		 * Executes the countdown logic, updating the timer label every second.
		 */
		@Override
		public void run()
		{
			if (duration < 0) {
                timer.setText("Timer expired");
                poll.setEnabled(true);
                submit.setEnabled(false);
                setStatus(false);
                this.cancel();
                return;
            }

            timer.setForeground(duration < 6 ? Color.RED : Color.BLACK);
            timer.setText("Time: " + duration + "s");
            duration--;
            window.repaint();
        }
	}

	/**
	 * Updates the question and available options displayed on the UI.
	 * 
	 * @param newQ A String array where the first element is the question, and the next four are the answer options.
	 */
	public void updateQuestion(String[] newQ){
		question.setText(newQ[0]);
		for(int i = 0; i < 4; i++){
			options[i].setText(newQ[i + 1]);
		}
	}

	/**
	 * Enables or disables the player's ability to submit answers.
	 * 
	 * @param status True to allow answering, false to disable.
	 */
	public void setStatus(boolean status){
		ableToAnswer = status;
		poll.setEnabled(!status);
		submit.setEnabled(status);
	}

	/**
	 * Updates the player's score by a given amount and reflects it on the UI.
	 * 
	 * @param change The value to be added to the current score.
	 */
	public void updateScore(int change){
		score = score + change;
		scoreLabel.setText("SCORE: " + score);
	}

	/**
	 * Returns the answer currently selected by the player.
	 * 
	 * @return The text of the selected answer.
	 */
	public String getAnswer(){
		return currAnswer;
	}

	/**
	 * Starts or resets the timer with a specified duration in seconds.
	 * 
	 * @param time The countdown duration in seconds.
	 */
	public void updateTimerDuration(int time){
		if(clock != null){
			clock.cancel();
		}
		clock = new TimerCode(time);
		Timer t = new Timer();
		t.schedule(clock, 0, 1000);
	}

	/**
	 * Displays the final game results in a message dialog.
	 * 
	 * @param results An array of result strings to be shown.
	 */
	public void displayResults(String[] results) {
        StringBuilder sb = new StringBuilder("Final Results:\n\n");
        for (String line : results) {
            sb.append(line).append("\n");
        }
        JOptionPane.showMessageDialog(window, sb.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

	/**
	 * Prepares the UI for the next question, re-enabling the poll button.
	 */
	public void resetForNextQuestion() {
		SwingUtilities.invokeLater(() -> {
			poll.setEnabled(true);
		});
	}
}