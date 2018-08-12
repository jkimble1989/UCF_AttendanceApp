package es.cybercrim.pwn.checkin;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import javax.net.ssl.HttpsURLConnection;

import static android.net.Uri.encode;

public class TeacherView extends AppCompatActivity {
    static final int MY_PERMISSIONS_REQUEST_BT_ADMIN = 29;
    static final int MY_PERMISSIONS_REQUEST_BT = 61;
    static final int MY_PERMISSIONS_REQUEST_LOC = 20;
    public String authcookie;
    public ArrayList<String> paths;
    public URL url = new URL("https://checkmate.cybercrim.es/myclass");
    public URL sub = new URL("https://checkmate.cybercrim.es/recordAttendance");
    public String payload;
    public String classNum;
    private ArrayList<String> mDeviceList = new ArrayList<String>();
    private BluetoothAdapter mBluetoothAdapter;
    private TextView progText;
    private Button mActivateBtn;
    private Button mScanBtn;
    private Spinner mClassSpinner;
    private int foundDevices;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device.getAddress());
                // remove duplicate MAC addresses
                removeDuplicates(mDeviceList);
                foundDevices = mDeviceList.size();
                progText.setText(String.valueOf(foundDevices) + getString(R.string.devFound));
            }
        }
    };
    private boolean hasPerms = true, shouldDestroy = false;
    private TeacherView.getClasses classGot = null;
    private TeacherView.checkIn checkMate = null;

    public TeacherView() throws MalformedURLException {
    }

    protected void scanDevices() {
        mBluetoothAdapter.startDiscovery();
        shouldDestroy = true;
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teacher);
        authcookie = DataHolder.getData();

        mActivateBtn = findViewById(R.id.btn_enable);
        mScanBtn = findViewById(R.id.btn_scan);
        progText = findViewById(R.id.mainText);

        if (ContextCompat.checkSelfPermission(TeacherView.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(TeacherView.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(TeacherView.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // explain
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(TeacherView.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOC);
                }
            } else {
                hasPerms = true;
            }
            hasPerms = false;
        }

        if (ContextCompat.checkSelfPermission(TeacherView.this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(TeacherView.this,
                    Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(TeacherView.this,
                        Manifest.permission.BLUETOOTH)) {
                } else {
                    ActivityCompat.requestPermissions(TeacherView.this,
                            new String[]{Manifest.permission.BLUETOOTH},
                            MY_PERMISSIONS_REQUEST_BT);
                }
            } else {
            }
            hasPerms = false;
        }

        if (ContextCompat.checkSelfPermission(TeacherView.this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(TeacherView.this,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(TeacherView.this,
                        Manifest.permission.BLUETOOTH_ADMIN)) {
                } else {
                    ActivityCompat.requestPermissions(TeacherView.this,
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

            // Permissions should be granted, if not app will crash. Polish later
            // Now set the onclick listeners
            mScanBtn.setOnClickListener(new View.OnClickListener() {
                boolean scanning = false;

                @Override
                public void onClick(View arg0) {
                    if (!scanning) {
                        mScanBtn.setText(getString(R.string.stop_scan));
                        scanning = true;
                        scanDevices();
                    } else {
                        // Stop scanning
                        mBluetoothAdapter.disable();
                        scanning = false;
                        mScanBtn.setText(getString(R.string.start_teacher_scan));
                        // Send json object to server
                        Gson gson = new Gson();
                        payload = gson.toJson(mDeviceList);
                        checkMate = new checkIn();
                        checkMate.execute((Void) null);
                    }
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

            paths = new ArrayList<String>();
            paths.add("Select a course to check in");
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(TeacherView.this,
                    android.R.layout.simple_spinner_item, paths);

            mClassSpinner = (Spinner) findViewById(R.id.class_spinner);
            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            mClassSpinner.setAdapter(adapter);
            mClassSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    classNum = String.valueOf(position);
                    mClassSpinner.setSelection(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            progText.setText("0 " + getString(R.string.devFound));

            classGot = new getClasses();
            classGot.execute((Void) null);
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
        if (shouldDestroy) {
            unregisterReceiver(mReceiver);
        }

        shouldDestroy = false;
        super.onDestroy();
    }

    protected void removeDuplicates(ArrayList<String> input) {
        HashSet hs = new HashSet();
        hs.addAll(input);
        input.clear();
        input.addAll(hs);
    }

    public class checkIn extends AsyncTask<Void, Void, Boolean> {
        checkIn() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpsURLConnection test = (HttpsURLConnection) sub.openConnection();
                test.setRequestProperty("Cookie", encode(authcookie, getString(R.string.web_charset)));
                test.setRequestMethod("POST");
                test.setDoOutput(true);
                test.setDoInput(true);
                DataOutputStream out = new DataOutputStream(test.getOutputStream());
                out.writeBytes(String.format("classID=%s&addresses=%s&email=%s&password=%s", encode(classNum, getString(R.string.web_charset)), encode(payload, getString(R.string.web_charset)), encode(DataHolder.getData2().toString(), getString(R.string.web_charset)), encode(DataHolder.getData3().toString(), getString(R.string.web_charset))));
                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(test.getInputStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    paths.add(str);
                }
                test.getInputStream().close();

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                progText.setText("Students checked in, check the dashboard for more information!");
            } else {
            }
        }

        @Override
        protected void onCancelled() {
            checkMate = null;
        }
    }

    public class getClasses extends AsyncTask<Void, Void, Boolean> {
        getClasses() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Thread.sleep(200);
                HttpsURLConnection test = (HttpsURLConnection) url.openConnection();
                test.setRequestProperty("Cookie", encode(authcookie, getString(R.string.web_charset)));
                test.setRequestMethod("POST");
                test.setDoOutput(true);
                test.setDoInput(true);
                DataOutputStream out = new DataOutputStream(test.getOutputStream());
                out.writeBytes(String.format("email=%s&password=%s", encode(DataHolder.getData2().toString(), getString(R.string.web_charset)), encode(DataHolder.getData3().toString(), getString(R.string.web_charset))));
                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(test.getInputStream()));
                String str, response = "";
                while ((str = in.readLine()) != null) {
                    paths.add(str);
                }
                test.getInputStream().close();

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
            } else {
            }
        }

        @Override
        protected void onCancelled() {
            classGot = null;
        }
    }
}
