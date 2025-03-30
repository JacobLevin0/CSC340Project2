import java.io.Serializable;

public class BuzzMessage implements Serializable {
    
    private int clientID;

    private int questionNumber;

    private String message;

    private String time;



    public BuzzMessage(int clientID, int questionNumber, String message, String time) {
        this.clientID = clientID;
        this.questionNumber = questionNumber;
        this.message = message;
        this.time=time;
    }


    public int getClientID() {
        return clientID;
    }




    public void setClientID(int clientID) {
        this.clientID = clientID;
    }



    public int getQuestionNumber() {
        return questionNumber;
    }



    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }



    public String getMessage() {
        return message;
    }



    public void setMessage(String message) {
        this.message = message;
    }


    public String getTime(){

        return time;
    }

    public void setTime(String time){
        this.time=time;
    }
    

}
