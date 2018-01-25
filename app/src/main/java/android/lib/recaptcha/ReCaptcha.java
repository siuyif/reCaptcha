package android.lib.recaptcha;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.os.AsyncTaskCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * {@code ReCaptcha} extends {@link android.widget.ImageView} to let you embed a <a href="http://captcha.net/">CAPTCHA</a>
 * in your applications in order to protect them against spam and other types of automated abuse.
 * @see <a href="https://developers.google.com/recaptcha/">reCAPTCHA</a>
 */
public class ReCaptcha extends ImageView {
    /**
     * Listener that is called when an attempt to show a <a href="http://captcha.net/">CAPTCHA</a>
     * is completed.
     */
    public interface OnShowChallengeListener {
        /**
         * Called when an attempt to show a <a href="http://captcha.net/">CAPTCHA</a> is completed.
         * @param shown {@code true} if a <a href="http://captcha.net/">CAPTCHA</a> is shown;
         * otherwise, {@code false}.
         */
        void onChallengeShown(boolean shown);
    }

    /**
     * Listener that is called when an answer entered by the user to solve the <a href="http://captcha.net/">CAPTCHA</a>
     * displayed is verified.
     */
    public interface OnVerifyAnswerListener {
        /**
         * Called when an answer entered by the user to solve the <a href="http://captcha.net/">CAPTCHA</a>
         * displayed is verified.
         *
         * @param success {@code true} if the <a href="http://captcha.net/">CAPTCHA</a> is solved successfully;
         *                otherwise, {@code false}.
         */
        void onAnswerVerified(boolean success);
    }

    private static final String TAG = "ReCaptcha";

    //region Constants

    private static final String VERIFICATION_URL           = "http://www.google.com/recaptcha/api/verify";
    private static final String CHALLENGE_URL              = "http://www.google.com/recaptcha/api/challenge?k=%s";
    private static final String RECAPTCHA_OBJECT_TOKEN_URL = "http://www.google.com/recaptcha/api/reload?c=%s&k=%s&type=%s";
    private static final String IMAGE_URL                  = "http://www.google.com/recaptcha/api/image?c=%s";

    //endregion

    private String challenge;
    private String imageToken;
    private String languageCode;

    //region Constructors

    public ReCaptcha(final Context context) {
        super(context);
    }

    public ReCaptcha(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ReCaptcha(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    //endregion

    //region Properties

    /**
     * Returns the challenge previously returned from the server.
     * <p>The challenge previously returned from the server will be available after {@link ReCaptcha.OnShowChallengeListener#onChallengeShown(boolean)} is called.</p>
     * @return The challenge previously returned from the server.
     */
    public final String getChallenge() {
        return this.challenge;
    }

    /**
     * Returns the image token previously returned from the server.
     * <p>The image token previously returned from the server will be available after {@link ReCaptcha.OnShowChallengeListener#onChallengeShown(boolean)} is called.</p>
     * @return The image token previously returned from the server.
     */
    public final String getImageToken() {
        return this.imageToken;
    }

    public final String getLanguageCode() {
        return this.languageCode;
    }

    /**
     * Forces the widget to render in a specific language.
     * <p>Auto-detects the user's language if unspecified.</p>
     * @param languageCode A valid language code.
     * @see <a href="https://developers.google.com/recaptcha/docs/language">https://developers.google.com/recaptcha/docs/language</a>
     */
    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    //endregion

    /**
     * Downloads and shows a <a href="http://captcha.net/">CAPTCHA</a> image asynchronously.
     * <p>This method executes asynchronously and can be invoked from the UI thread.</p>
     * @param publicKey The public key that is unique to your domain and sub-domains (unless it is global key).
     * @param listener The callback to call when an attempt to show a <a href="http://captcha.net/">CAPTCHA</a> is completed.
     */
    @UiThread
    public final void showChallengeAsync(@NonNull final String publicKey, @Nullable final ReCaptcha.OnShowChallengeListener listener) {
        if (TextUtils.isEmpty(publicKey)) {
            throw new IllegalArgumentException("publicKey cannot be null or empty");
        }

        this.setImageDrawable(null);

        this.challenge  = null;
        this.imageToken = null;

        AsyncTaskCompat.executeParallel(new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(final String... strings) {
                try {
                    return ReCaptcha.this.downloadImage(publicKey);
                } catch (final ReCaptchaException e) {
                    Log.e(ReCaptcha.TAG, "The downloaded CAPTCHA content is malformed", e);
                } catch (final IOException e) {
                    Log.e(ReCaptcha.TAG, "A protocol or network connection problem has occurred", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                if (bitmap == null) {
                    if (listener != null) {
                        listener.onChallengeShown(false);
                    }
                } else {
                    final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 2, bitmap.getHeight() * 2, true);

                    bitmap.recycle();

                    ReCaptcha.this.setImageBitmap(scaled);

                    if (listener != null) {
                        listener.onChallengeShown(true);
                    }
                }
            }
        }, publicKey);
    }

    /**
     * Checks asynchronously whether the answer entered by the user is correct after your application
     * is successfully displaying <a href="https://developers.google.com/recaptcha/">reCAPTCHA</a>.
     * @param privateKey The private key that is unique to your domain and sub-domains (unless it is a global key).
     * @param answer The string the user entered to solve the <a href="http://captcha.net/">CAPTCHA</a> displayed.
     * @param listener The callback to call when an answer entered by the user is verified.
     */
    @UiThread
    public final void verifyAnswerAsync(@NonNull final String privateKey, @NonNull final String answer, @Nullable final ReCaptcha.OnVerifyAnswerListener listener) {
        if (TextUtils.isEmpty(privateKey)) {
            throw new IllegalArgumentException("privateKey cannot be null or empty");
        }

        if (TextUtils.isEmpty(answer)) {
            throw new IllegalArgumentException("answer cannot be null or empty");
        }

        AsyncTaskCompat.executeParallel(new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(final String... params) {
                try {
                    return ReCaptcha.this.submitAnswer(params[0], params[1]);
                } catch (final IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                return Boolean.FALSE;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                if (listener != null) {
                    listener.onAnswerVerified(result);
                }
            }
        }, privateKey, answer);
    }

    @WorkerThread
    private Bitmap downloadImage(@NonNull final String publicKey) throws ReCaptchaException, IOException {
        try {
            final String challenge = this.getChallenge(publicKey);
            Log.d(ReCaptcha.TAG, "challenge = " + challenge);

            if (challenge == null) {
                throw new ReCaptchaException("ReCaptcha challenge not found");
            }

            final String imageToken = ReCaptcha.getImageToken(challenge, publicKey);
            Log.d(ReCaptcha.TAG, "imageToken = " + imageToken);

            if (imageToken == null) {
                throw new ReCaptchaException("Image token not found");
            }

            this.imageToken = imageToken;

            String url = String.format(ReCaptcha.IMAGE_URL, imageToken);

            if (!TextUtils.isEmpty(this.languageCode)) {
                url += "&hl=" + this.languageCode;
            }

            Response response = null;

            try {
                response = new OkHttpClient().newCall(new Request.Builder()
                    .url(url)
                    .build()).execute();

                if (response.isSuccessful()) {
                    final ResponseBody responseBody = response.body();

                    try {
                        final Bitmap bitmap = BitmapFactory.decodeStream(responseBody.byteStream());

                        if (bitmap == null) {
                            throw new ReCaptchaException("Invalid CAPTCHA image");
                        }

                        return bitmap;
                    } finally {
                        responseBody.close();
                    }
                }

                throw new ReCaptchaException("Received HTTP " + response.code());
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (final JSONException e) {
            throw new ReCaptchaException("Unable to parse challenge response", e);
        }
    }

    @WorkerThread
    private static String getImageToken(@NonNull final String challenge, @NonNull final String publicKey) throws ReCaptchaException, IOException {
        Response response = null;

        try {
            response = new OkHttpClient().newCall(new Request.Builder()
                .url(String.format(ReCaptcha.RECAPTCHA_OBJECT_TOKEN_URL, challenge, publicKey, "image"))
                .build()).execute();

            if (response.isSuccessful()) {
                final ResponseBody responseBody = response.body();

                try {
                    final String imageTokenResponse = responseBody.string();
                    Log.d(ReCaptcha.TAG, "imageTokenResponse = " + imageTokenResponse);

                    return ReCaptcha.substringBetween(imageTokenResponse, "('", "',");
                } finally {
                    responseBody.close();
                }
            }

            throw new ReCaptchaException("Received HTTP " + response.code());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @WorkerThread
    private String getChallenge(@NonNull final String publicKey) throws ReCaptchaException, IOException, JSONException {
        Response response = null;

        try {
            response = new OkHttpClient().newCall(new Request.Builder()
                .url(String.format(ReCaptcha.CHALLENGE_URL, publicKey))
                .build()).execute();

            if (response.isSuccessful()) {
                final ResponseBody responseBody = response.body();

                try {
                    final String challengeResponse = responseBody.string();
                    Log.d(ReCaptcha.TAG, "challengeResponse = " + challengeResponse);

                    this.challenge = new JSONObject(ReCaptcha.substringBetween(challengeResponse, "RecaptchaState = ", "}") + "}").getString("challenge");

                    return this.challenge;
                } finally {
                    responseBody.close();
                }
            }

            throw new ReCaptchaException("Received HTTP " + response.code());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @WorkerThread
    private boolean submitAnswer(@NonNull final String privateKey, @NonNull final String answer) throws IOException {
        Response response = null;

        try {
            response = new OkHttpClient().newCall(new Request.Builder()
                .url(ReCaptcha.VERIFICATION_URL)
                .post(new FormBody.Builder()
                    .add("privatekey", privateKey)
                    .add("remoteip", "127.0.0.1")
                    .add("challenge", this.imageToken)
                    .add("response", answer)
                    .build())
                .build()).execute();

            if (response.isSuccessful()) {
                final ResponseBody responseBody = response.body();

                try {
                    return responseBody.string().startsWith("true");
                } finally {
                    responseBody.close();
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return false;
    }

    private static String substringBetween(@Nullable final String str, @Nullable final String open, @Nullable final String close) {
        if (str == null || open == null || close == null) {
            return null;
        }

        final int start = str.indexOf(open);

        if (start != -1) {
            final int end = str.indexOf(close, start + open.length());

            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }

        return null;
    }
}