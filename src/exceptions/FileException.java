package exceptions;

public class FileException extends Exception {
    public FileException(String errorMessage) {
        super("Error: " + errorMessage);
    }
}
