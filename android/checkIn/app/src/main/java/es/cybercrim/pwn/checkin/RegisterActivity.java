package es.cybercrim.pwn.checkin;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static java.net.URLEncoder.encode;

public class RegisterActivity extends Activity {
    static final int MY_PERMISSIONS_REQUEST_INTERNET = 54;
    static final int MY_PERMISSIONS_REQUEST_NETSTATE = 37;
    public URL reg = new URL("https://checkmate.cybercrim.es/register");
    private View mRegisterView;
    private View mProgressView;
    private EditText mEmailView;
    private EditText mIdView;
    private EditText mFirstNameView;
    private EditText mLastNameView;
    private EditText mPassView;
    private EditText mPassConfView;
    private RadioGroup mRegWho;
    private RadioButton mRegStu, mRegProf;
    private Button mRegisterButton;

    private UserRegisterTask mAuthTask = null;

    public RegisterActivity() throws MalformedURLException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        // Checks for the Internet permissions
        if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(RegisterActivity.this,
                    Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(RegisterActivity.this,
                        Manifest.permission.INTERNET)) {
                    // explain
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(RegisterActivity.this,
                            new String[]{Manifest.permission.INTERNET},
                            MY_PERMISSIONS_REQUEST_INTERNET);
                }
            } else {
            }
        }

        // Checks for the Access to the Network State permission
        if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(RegisterActivity.this,
                    Manifest.permission.ACCESS_NETWORK_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(RegisterActivity.this,
                        Manifest.permission.ACCESS_NETWORK_STATE)) {
                    // explain
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(RegisterActivity.this,
                            new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                            MY_PERMISSIONS_REQUEST_NETSTATE);
                }
            } else {
            }
        }

        mEmailView = (EditText) findViewById(R.id.emailReg);
        mIdView = (EditText) findViewById(R.id.idReg);
        mFirstNameView = (EditText) findViewById(R.id.first_name);
        mLastNameView = (EditText) findViewById(R.id.last_name);
        mPassView = (EditText) findViewById(R.id.passwordReg);
        mPassConfView = (EditText) findViewById(R.id.passwordRegConf);
        mRegisterButton = (Button) findViewById(R.id.email_register_in_button);
        mProgressView = findViewById(R.id.register_progress);
        mRegisterView = findViewById(R.id.register_form);

        mRegWho = (RadioGroup) findViewById(R.id.regGroup);
        mRegStu = (RadioButton) findViewById(R.id.stuReg);
        mRegProf = (RadioButton) findViewById(R.id.profReg);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPassView.setError(null);

        // Store values at the time of the login attempt.
        String uType = "";
        String email = mEmailView.getText().toString();
        String id = mIdView.getText().toString();
        String firstName = mFirstNameView.getText().toString();
        String lastName = mLastNameView.getText().toString();
        String password = mPassView.getText().toString();
        String password_confirm = mPassConfView.getText().toString();
        boolean same = password.equals(password_confirm);

        // Check which radio button is selected.
        int selectedId = mRegWho.getCheckedRadioButtonId();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPassView.setError(getString(R.string.error_invalid_password));
            focusView = mPassView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Checks if password field is empty. Then checks if both passwords match
        if (TextUtils.isEmpty(password)) {
            mPassView.setError(getString(R.string.error_field_required));
            focusView = mPassView;
            cancel = true;
        } else if (!same) {
            mPassView.setError(getString(R.string.error_pass_conf));
            mPassConfView.setError(getString(R.string.error_pass_conf));
            focusView = mPassView;
            cancel = true;
        }

        if (selectedId != mRegStu.getId() && selectedId != mRegProf.getId()) {
            Toast.makeText(getApplicationContext(), R.string.error_radio_button, Toast.LENGTH_SHORT).show();
        } else if (selectedId == mRegStu.getId()) {
            uType = "0";
        } else if (selectedId == mRegProf.getId()) {
            uType = "1";
        } else {
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserRegisterTask(uType);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        // If the user enters any user id/email
        return email.length() > 0;
    }

    private boolean isPasswordValid(String password) {
        // If the user enters any password
        return password.length() > 0;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUserType;
        private final String mRegNow;

        UserRegisterTask(String uType) {
            mUserType = uType;
            mRegNow = getString(R.string.canary_register);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpsURLConnection test = (HttpsURLConnection) reg.openConnection();
                test.setRequestMethod("POST");
                test.setDoOutput(true);
                test.setDoInput(true);
                DataOutputStream out = new DataOutputStream(test.getOutputStream());
                out.writeBytes(String.format("email=%s&pid=%s&FirstName=%s&LastName=%s&password=%s&confirm=%s&userType=%s&professor=%s",
                        encode(mEmailView.getText().toString(), getString(R.string.web_charset)),
                        encode(mIdView.getText().toString(), getString(R.string.web_charset)),
                        encode(mFirstNameView.getText().toString(), getString(R.string.web_charset)),
                        encode(mLastNameView.getText().toString(), getString(R.string.web_charset)),
                        encode(mPassView.getText().toString(), getString(R.string.web_charset)),
                        encode(mPassConfView.getText().toString(), getString(R.string.web_charset)),
                        encode(mUserType, getString(R.string.web_charset)),
                        encode(mRegNow, getString(R.string.web_charset))));
                out.close();

                test.getInputStream().close();
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
                Toast.makeText(RegisterActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RegisterActivity.this, getString(R.string.login_fail), Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

