package es.cybercrim.pwn.checkin;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static java.net.URLEncoder.encode;

public class StudentView extends AppCompatActivity {
    static final int MY_PERMISSIONS_REQUEST_BT_ADMIN = 29;
    static final int MY_PERMISSIONS_REQUEST_BT = 61;
    static final int MY_PERMISSIONS_REQUEST_LOC = 20;
    public URL uMac = new URL("https://checkmate.cybercrim.es/umac");
    private BluetoothAdapter mBluetoothAdapter;
    private Button mActivateBtn;
    private Button mScanBtn;
    private EditText mAddDevice;
    private Button mAddDeviceBtn;
    private boolean hasPerms = true;
    private String authcookie;
    private UpdateMacTask updateMac = null;

    public StudentView() throws MalformedURLException {
    }

    protected void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student);
        authcookie = DataHolder.getData();
        mActivateBtn = findViewById(R.id.btn_enable2);
        mScanBtn = findViewById(R.id.btn_sign_in2);
        mAddDevice = (EditText) findViewById(R.id.add_device_text);
        mAddDeviceBtn = findViewById(R.id.btn_add_device);

        if (ContextCompat.checkSelfPermission(StudentView.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(StudentView.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(StudentView.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // explain
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(StudentView.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOC);
                }
            } else {
                hasPerms = true;
            }
            hasPerms = false;
        }

        if (ContextCompat.checkSelfPermission(StudentView.this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(StudentView.this,
                    Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(StudentView.this,
                        Manifest.permission.BLUETOOTH)) {
                } else {
                    ActivityCompat.requestPermissions(StudentView.this,
                            new String[]{Manifest.permission.BLUETOOTH},
                            MY_PERMISSIONS_REQUEST_BT);
                }
            } else {
            }
            hasPerms = false;
        }

        if (ContextCompat.checkSelfPermission(StudentView.this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(StudentView.this,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(StudentView.this,
                        Manifest.permission.BLUETOOTH_ADMIN)) {
                } else {
                    ActivityCompat.requestPermissions(StudentView.this,
                            new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                            MY_PERMISSIONS_REQUEST_BT_ADMIN);
                }
            } else {
            }
            hasPerms = false;
        }
        if (hasPerms) {
            // Init radio
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            // Permissions should be granted, if not app will crash.
            // Now set the onclick listeners
            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    makeDiscoverable();
                }
            });

            mActivateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                        showDisabled();
                    } else {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 1000);
                        showEnabled();
                    }
                }
            });

            if (mBluetoothAdapter.isEnabled()) {
                showEnabled();
            } else {
                showDisabled();
            }

            mAddDeviceBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateMac = new UpdateMacTask();
                    updateMac.execute((Void) null);
                }
            });

        }
    }

    private void showDisabled() {
        mActivateBtn.setText(getString(R.string.bt_enable));
        mActivateBtn.setEnabled(true);
        mScanBtn.setEnabled(false);
    }

    private void showEnabled() {
        mActivateBtn.setText(getString(R.string.bt_disable));
        mActivateBtn.setEnabled(true);
        mScanBtn.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public class UpdateMacTask extends AsyncTask<Void, Void, Boolean> {
        UpdateMacTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpsURLConnection test = (HttpsURLConnection) uMac.openConnection();
                test.setRequestProperty("Cookie", encode(authcookie, getString(R.string.web_charset)));
                test.setRequestMethod("POST");
                test.setDoOutput(true);
                test.setDoInput(true);
                DataOutputStream out = new DataOutputStream(test.getOutputStream());
                out.writeBytes(String.format("mac=%s&email=%s&password=%s", encode(mAddDevice.getText().toString(), getString(R.string.web_charset)), encode(DataHolder.getData2().toString(), getString(R.string.web_charset)), encode(DataHolder.getData3().toString(), getString(R.string.web_charset))));
                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(test.getInputStream()));
                String str, response = "";
                while ((str = in.readLine()) != null) {
                    response += str;
                }
                test.getInputStream().close();

                if (response.contains("Invalid")) {
                    return false;
                }
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Toast.makeText(StudentView.this, getString(R.string.mac_update_success), Toast.LENGTH_SHORT).show();
                updateMac = null;
            } else {
                mAddDevice.setError(getString(R.string.mac_update_failure));
                mAddDevice.requestFocus();
                updateMac = null;
            }
        }

        @Override
        protected void onCancelled() {
            updateMac = null;
        }
    }
}
