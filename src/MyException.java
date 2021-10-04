public class MyException extends Exception {
    int errorCode = 0;
    int errorLine = 0;

    public MyException(int errorCode) {
        this.errorCode = errorCode;
    }

    public MyException(int errorCode, int errorLine) {
        this.errorCode = errorCode;
        this.errorLine = errorLine;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
