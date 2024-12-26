package io.github.sasiperi.logsafe.logger;

public class RedactionException extends RuntimeException
{
    private static final long serialVersionUID = -4534584325883367066L;

    public RedactionException(String message, Exception e) {
        super(message, e);
     }
}
