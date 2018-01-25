package android.lib.recaptcha.samples;

import android.app.Activity;
import android.lib.recaptcha.ReCaptcha;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class SampleActivity extends Activity implements View.OnClickListener, ReCaptcha.OnShowChallengeListener, ReCaptcha.OnVerifyAnswerListener {
    private static final String PUBLIC_KEY  = "6LfMXDgUAAAAACjg5H1hNqk5iaLe42NLyar8ogcZ";
    private static final String PRIVATE_KEY = "6LfMXDgUAAAAAK70mYgH6hkdoBX9IbFIipRd7zV6";

    private ReCaptcha   reCaptcha;
    private ProgressBar progress;
    private EditText    answer;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_sample);

        this.reCaptcha = (ReCaptcha)this.findViewById(R.id.recaptcha);
        this.progress  = (ProgressBar)this.findViewById(R.id.progress);
        this.answer    = (EditText)this.findViewById(R.id.answer);

        this.findViewById(R.id.verify).setOnClickListener(this);
        this.findViewById(R.id.reload).setOnClickListener(this);

        this.showChallenge();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.verify:
                this.verifyAnswer();

                break;

            case R.id.reload:
                this.showChallenge();

                break;
        }
    }

    @Override
    public void onChallengeShown(final boolean shown) {
        this.progress.setVisibility(View.GONE);

        if (shown) {
            // If a CAPTCHA is shown successfully, displays it for the user to enter the words
            this.reCaptcha.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, R.string.show_error, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    // verify inputted answer
    public void onAnswerVerified(final boolean success) {
        if (success) {
            Toast position = Toast.makeText(this, R.string.verification_success, Toast.LENGTH_SHORT);
            position.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL,0, 250);
            position.show();

        }
        else {
            Toast position = Toast.makeText(this, R.string.verification_failed, Toast.LENGTH_SHORT);
            position.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL,0, 250);
            position.show();
        }

        //loads the next CAPTCHA and clears the edittext field
        this.showChallenge();
        answer.setText(null);
    }


    private void showChallenge() {
        // Displays a progress bar while downloading CAPTCHA
        this.progress.setVisibility(View.VISIBLE);
        this.reCaptcha.setVisibility(View.GONE);

        this.reCaptcha.setLanguageCode("en");
        this.reCaptcha.showChallengeAsync(SampleActivity.PUBLIC_KEY, this);
    }

    // to verify answer inputted by user
    private void verifyAnswer() {
        if (TextUtils.isEmpty(this.answer.getText())) {
            Toast position = Toast.makeText(this, R.string.instruction, Toast.LENGTH_SHORT);
            position.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL,0, 250);
            position.show();
        }
        else {
            // Displays a progress bar while submitting the answer for verification
            this.progress.setVisibility(View.VISIBLE);
            this.reCaptcha.verifyAnswerAsync(SampleActivity.PRIVATE_KEY, this.answer.getText().toString(), this);
        }
    }
}