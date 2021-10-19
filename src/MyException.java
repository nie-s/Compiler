public class MyException extends Exception {
    int errorCode = 0;
    int errorLine = 0;
    String errorMessage = "";

    public MyException(int errorCode, int errorLine) {
        this.errorCode = errorCode;
        this.errorLine = errorLine;
    }


    public MyException(int errorCode, int errorLine, String errorMessage) {
        this.errorCode = errorCode;
        this.errorLine = errorLine;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
