//package project2;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;

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
	
	// write setters and getters as you need
	
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




		/*JOptionPane.showMessageDialog(window, "This is a trivia game");
		
		window = new JFrame("Trivia");
		question = new JLabel("Q1. This is a sample question"); // represents the question
		window.add(question);
		question.setBounds(10, 5, 350, 100);;
		
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index=0; index<options.length; index++)
		{
			options[index] = new JRadioButton("Option " + (index+1));  // represents an option
			// if a radio button is clicked, the event would be thrown to this class to handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110+(index*20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
		}

		timer = new JLabel("TIMER");  // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(30);  // represents clocked task that should run after X seconds
		Timer t = new Timer();  // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);
		
		
		scoreLabel = new JLabel("SCORE"); // represents the score
		scoreLabel.setBounds(50, 250, 100, 20);
		window.add(scoreLabel);

		poll = new JButton("Poll");  // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);  // calls actionPerformed of this class
		window.add(poll);
		
		submit = new JButton("Submit");  // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);  // calls actionPerformed of this class
		window.add(submit);
		
		
		window.setSize(400,400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);

		this.player = player;
		ableToAnswer = false;
		score = 0; */
	}

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
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




	 /* 	System.out.println("You clicked " + e.getActionCommand());
		
		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();  
		switch(input)
		{
			case "Option 1":	// Your code here
								break;
			case "Option 2":	// Your code here
								break;
			case "Option 3":	// Your code here
								break;
			case "Option 4":	// Your code here
								break;
			case "Poll":		// Your code here
								player.poll();
								break;
			case "Submit":		// Your code here
								break;
			default:
								System.out.println("Incorrect Option");
		}
		
		// test code below to demo enable/disable components
		// DELETE THE CODE BELOW FROM HERE***
		if(poll.isEnabled())
		{
			poll.setEnabled(false);
			submit.setEnabled(true);
		}
		else
		{
			poll.setEnabled(true);
			submit.setEnabled(false);
		}
		
		question.setText("Q2. This is another test problem " + random.nextInt());
		
		// you can also enable disable radio buttons
//		options[random.nextInt(4)].setEnabled(false);
//		options[random.nextInt(4)].setEnabled(true);
		// TILL HERE *** 
		*/
	}
	
	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask
	{
		private int duration;  // write setters and getters as you need
		public TimerCode(int duration)
		{
			this.duration = duration;
		}
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
	
	public void updateQuestion(String[] newQ){
		question.setText(newQ[0]);
		for(int i = 0; i < 4; i++){
			options[i].setText(newQ[i + 1]);
		}
	}

	public void setStatus(boolean status){
		ableToAnswer = status;
		poll.setEnabled(!status);   // Only buzz if not already active
		submit.setEnabled(status); // Only submit if active
	}

	public void updateScore(int change){
		score = score + change;
		scoreLabel.setText("SCORE: " + score);
	}

	public String getAnswer(){
		return currAnswer;
	}

	public void updateTimerDuration(int time){
		if(clock!=null){
			clock.cancel();
		}
		clock = new TimerCode(time);
		Timer t = new Timer();
		t.schedule(clock, 0, 1000);
	}


	public void displayResults(String[] results) {
        StringBuilder sb = new StringBuilder("Final Results:\n\n");
        for (String line : results) {
            sb.append(line).append("\n");
        }
        JOptionPane.showMessageDialog(window, sb.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }


	public void resetForNextQuestion() {
		SwingUtilities.invokeLater(() -> {
			poll.setEnabled(true);       // Re-enable poll/buzz button
		});
	}



}