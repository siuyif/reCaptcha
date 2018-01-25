package android.lib.recaptcha;

/**
 * Exception to send when the downloaded <a href="http://captcha.net/">CAPTCHA</a> content is malformed.
 */
public class ReCaptchaException extends Exception {
    private static final long serialVersionUID = -198070144274436116L;

    public ReCaptchaException() {
    }

    public ReCaptchaException(final String detailMessage) {
        super(detailMessage);
    }

    public ReCaptchaException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ReCaptchaException(final Throwable throwable) {
        super(throwable);
    }
}