/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.example.android.common.logger.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.List;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final int CMD_RECORD_START = 0;
    private static final int CMD_ECHO_OFF = 1;
    private static final int CMD_INIT = 2;
    private static final int FIVE_MINUTE_LOOP_INDEX_0 = 3;
    private static final int FINISH_ONE_CYCLE = 11;

    public boolean complete = false;
    private static final String TAG = "BluetoothFragment";

    private static int cmdIndex = 0;
    private static String nextCmd = "";
    private static int cmdDelay = -1;

    public static boolean isProcessing = false;
    public static boolean continueProcess = true;
    public static boolean isBtnStart = true;


    private static File folderToCheck;
    private static File currentProcessingFile;
    private static final String UPLOAD_FOLDER = "Upload_To_AWS_S3";
    public static UploadTask currentUploadTask;

    public static FileWriter currentFileWriter;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String SUFFIX = "/";

    // Layout Views
    private ListView mConversationView;
    private static EditText mOutEditText;
    private Button mSendButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private static ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    private StringBuilder commandBuilder;
    public static Handler obdHandler = new Handler();
    public static Handler UIHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        commandBuilder = new StringBuilder();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

        //Create Folder
        folderToCheck = new File(getActivity().getFilesDir() + "/" + UPLOAD_FOLDER);

        //appendToConsole( "Folder Exists ? " + String.valueOf(folderToCheck.exists()));

        Util.DeleteRecursive(folderToCheck);
        folderToCheck.delete();
        //appendToConsole("Folder Exists After Delete ? " + String.valueOf(folderToCheck.exists()));
        try {
            folderToCheck.mkdir();
            createNewFile();
//            for (int i = 0 ; i < 1000 ; i++){
//                currentFileWriter.
// ("test line: " + (i+1));
//                currentFileWriter.write(System.getProperty("line.separator"));
//                currentFileWriter.flush();
//            }

//                if (folderToCheck.exists() && folderToCheck.isDirectory()) {
//                    mConversationArrayAdapter.add(folderToCheck + "exists , .isDirectory");
//                }
//                File[] folders = folderToCheck.listFiles();
//                File[] filesInDir = folders[0].listFiles();
//                mConversationArrayAdapter.add(filesInDir.length + " files found in : " + folders[0].getName());
//                if (filesInDir.length > 0) {
//                    currentProcessingFile = filesInDir[0];
//                }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);

        mOutEditText.setFocusable(false);
        mOutEditText.setClickable(false);

    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
//        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

//                 Send a message using content of the edit text widget
//                View view = getView();
//                if (null != view) {
//                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
//                    String message = textView.getText().toString();
//                    sendBtMessage(message);
//                }
                if (isBtnStart) {
                    mSendButton.setText("STOP");
                    isBtnStart = false;
                    continueProcess = true;
                    if (!isProcessing) {
                        sendNextCmd();
                        isProcessing = true;
                        appendToStatus("Cycle Start");
                    } else {
                        appendToStatus("Cycle in progress");
                    }

                } else {
                    mSendButton.setText("START");
                    isBtnStart = true;
                    continueProcess = false;
                    appendToStatus("Cycle will Stop after finish");
                }

//                mSendButton.setEnabled(false);

            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), btHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }


    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
//    private void sendMessage(String message) {
//        message = message + '\r';
//        // Check that we're actually connected before trying anything
//        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check that there's actually something to send
//        if (message.length() > 0) {
//            // Get the message bytes and tell the BluetoothChatService to write
//            byte[] send = message.getBytes();
//            mChatService.write(send);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
//        }
//    }
    public void sendBtMessage(String message) {
        message = message + '\r';
//         Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }

        mConversationArrayAdapter.add("Send:  " + message);
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
//    private TextView.OnEditorActionListener mWriteListener
//            = new TextView.OnEditorActionListener() {
//        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//            // If the action is a key-up event on the return key, send the message
//            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//
//                //This gets the raw text from the box
////                String message = view.getText().toString();
////                //Append a CR to every message sent.  The added string is defined as a literal,
////                // so Java parses is as a single CR character.
////                sendBtMessage(message);
//            }
//            return true;
//        }
//
//    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler btHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Write:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    // Read in string from message, display to mainText for user
                    String readMessage = (String) msg.obj;
                    if (msg.arg1 > 0) {
                        commandBuilder.append(readMessage);

                        if (readMessage.contains(">")) {
                            String result = commandBuilder.toString();
                            mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + result);
                            try {
                                result = result.substring(0, result.length() - 1);
                                result = result.replace("\r", "");
                                String[] resarr = result.trim().split(" ");

//                                StringBuilder arrContain = new StringBuilder();
//                                for(int i = 0; i < resarr.length; i ++){
//                                    arrContain.append("(" + i + ")" + " " + resarr[i] + ",");
//                                }
//                                mConversationArrayAdapter.add("Respond Array: " + arrContain.toString());

                                if (resarr.length > 2) {
                                    StringBuilder toFile = new StringBuilder();
                                    String code = resarr[0];
                                    String PID = resarr[1];

//                                    code = code.replace("\r", "");
//                                    PID = PID.replace("\r", "");

                                    if (code.contains("SEARCHING...")) {
                                        code = code.substring(code.lastIndexOf(".") + 1, code.length());
                                    }

                                    String obdString = OBD.getResponseString(code, PID);

                                    if (!obdString.equals(OBD.UNDEFINED)) {
                                        toFile.append(obdString + " ");
                                        toFile.append(code + " ");
                                        toFile.append(PID + " ");
                                        for (int i = 2; i < resarr.length; i++) {
                                            toFile.append(resarr[i] + " ");
                                        }
                                        long currentTime = System.currentTimeMillis();
                                        toFile.append("T" + currentTime);
                                        toFile.append(",");
                                        currentFileWriter.write(toFile.toString());
                                        currentFileWriter.write(System.getProperty("line.separator"));
                                        currentFileWriter.flush();
                                        mConversationArrayAdapter.add("Write to File: " + toFile.toString());
                                    } else {
                                        mConversationArrayAdapter.add("OBD UND: " + result + " Code: -" + code + "- PID: -" + PID + "-");
                                    }

                                }


                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            commandBuilder = new StringBuilder();
                            sendNextCmd();
                        }
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    public class UploadTask extends AsyncTask<Void, Void, Void> {

        //  https://mobile.awsblog.com/post/Tx2KF0YUQITA164/AWS-SDK-for-Android-Transfer-Manager-to-Transfer-Utility-Migration-Guide
        TransferUtility transferUtility;
        TransferObserver observer;


        @Override
        protected Void doInBackground(Void... params) {

            complete = false;
            try {


                AmazonS3Client s3Client = Util.getS3Client(getActivity());

//                String bucketName = "kuocb";
            String bucketName = "obdcloudbucket";

                List<Bucket> buckets = s3Client.listBuckets();
                boolean targetBucketExist = false;
                for (Bucket bucket : buckets) {
                    if (bucket.getName().equals(bucketName)) {
                        targetBucketExist = true;
                    }
                    appendToConsole("Bucket Found: " + bucket.getName());
                }
                if (!targetBucketExist) {
                    s3Client.createBucket(bucketName);
                }
//            SimpleDateFormat dataSdf = new SimpleDateFormat("yyyy_MM_dd");
//            String currentDate = dataSdf.format(new Date());

//            createFolder(bucketName, currentDate, s3Client);

//            SimpleDateFormat timeSdf = new SimpleDateFormat("HH");
//            String currentDateAndTime = timeSdf.format(new Date());


                String fileName = currentProcessingFile.getName();
                Log.d(TAG, "File : " + fileName);


                transferUtility = Util.getTransferUtility(getActivity());

//            List<TransferObserver> observers = transferUtility.getTransfersWithType(TransferType.UPLOAD);

                observer = transferUtility.upload(bucketName, fileName, currentProcessingFile.getAbsoluteFile());
//            observers.add(observer);
//            HashMap<String, Object> map = new HashMap<String, Object>();
//            Util.fillMap(map, observer, false);
//            transferRecordMaps.add(map);
                observer.setTransferListener(new UploadListener());


                Log.d(TAG, "Uploading in progress");
                Log.d(TAG, "AWS S3 ID: " + observer.getId());
                Log.d(TAG, "FilePath:\n" + observer.getAbsoluteFilePath());


                appendToStatus("State: " + observer.getState());


//            PutObjectRequest por = new PutObjectRequest(bucketName, fileName, testFile.getAbsoluteFile());
//            por.withCannedAcl(CannedAccessControlList.PublicRead);
//            PutObjectResult putResponse = s3Client.putObject(por);
//            appendToConsole("Upload with Etag: " + putResponse.getETag());

//            GetObjectRequest getRequest = new GetObjectRequest(bucketName, fileName);
//            S3Object getResponse = s3Client.getObject(getRequest);
//            InputStream myObjectBytes = getResponse.getObjectContent();

//             Do what you want with the object

//            try {
//                myObjectBytes.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            } catch (Exception s3e) {
                appendToConsole(s3e.toString());
                Log.e(TAG, s3e.toString());
                isProcessing = false;
                complete = true;
                setButtonToStart();
            }

            return null;
        }

//        public void createFolder(String bucketName, String folderName, AmazonS3 client) {
//            // create meta-data for your folder and set content-length to 0
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentLength(0);
//            // create empty content
//            InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
//            // create a PutObjectRequest passing the folder name suffixed by /
//            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName + SUFFIX, emptyContent, metadata);
//            // send request to S3 to create folder
//            client.putObject(putObjectRequest);
//            Log.d(TAG, "Folder: " + folderName);
//        }

        private class UploadListener implements TransferListener {

            //            http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/s3transferutility.html#resume-a-transfer
            private String errtag = "UPLOADERROR";

            // Simply updates the UI list when notified.
            @Override
            public void onError(int id, Exception e) {
                Log.e(errtag, "Error during upload: " + id, e);
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                updateGUI();
            }

            @Override
            public void onStateChanged(int id, TransferState newState) {
                Log.d(TAG, "Transfer state: " + observer.getState());
                if (observer.getState().equals(TransferState.COMPLETED)) {

                    Log.d(TAG, "Transfer Finished\ndelete file: " + currentProcessingFile.getName());
                    try {
                        currentFileWriter.close();
                        currentProcessingFile.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    appendToConsole("Upload Complete: " + currentProcessingFile.getName());
                    isProcessing = false;
                    createNewFile();
                    appendToConsole("New File Created: " + currentProcessingFile.getName());
                    complete = true;
                    cmdIndex = FIVE_MINUTE_LOOP_INDEX_0;
                    if (continueProcess) {
                        sendNextCmd();
                    }
                }
            }
        }

        public void updateGUI() {
            if (!complete) {
                String progressBytes = "bytes: " + Util.getBytesString(observer.getBytesTransferred()) + "/" + Util.getBytesString(observer.getBytesTotal());
                int progress = (int) ((double) observer.getBytesTransferred() * 100 / observer.getBytesTotal());
                appendToStatus("Progress: " + "\n" + progressBytes + " => " + progress + "%");
            }
        }


    }

    private void setButtonToStart() {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                mSendButton.setText("START");
                isBtnStart = true;
                continueProcess = false;
            }
        });
    }


    public static void appendToConsole(final String toAppend) {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                mConversationArrayAdapter.add(toAppend);
            }
        });
    }

    public static void appendToStatus(final String statusString) {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                mOutEditText.setText(statusString);
            }
        });
    }

    private void createNewFile() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Calendar cal = Calendar.getInstance();
        String fileName = dateFormat.format(cal.getTime()) + ".txt";

        currentProcessingFile = new File(folderToCheck.getAbsolutePath(), fileName);

        try {
            currentProcessingFile.createNewFile();
            currentFileWriter = new FileWriter(currentProcessingFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void sendNextCmd() {

        if (cmdIndex == CMD_RECORD_START) {
            nextCmd = OBD.VERSION_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == CMD_ECHO_OFF) {
            nextCmd = OBD.ECHOOFF_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == CMD_INIT) {
            nextCmd = OBD.INIT_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == FIVE_MINUTE_LOOP_INDEX_0) {
            nextCmd = OBD.P_ERR_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == FIVE_MINUTE_LOOP_INDEX_0 + 1) {
            nextCmd = OBD.ERR_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == FIVE_MINUTE_LOOP_INDEX_0 + 2) {
            nextCmd = OBD.FSS_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == FIVE_MINUTE_LOOP_INDEX_0 + 3) {
            nextCmd = OBD.ELV_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == FIVE_MINUTE_LOOP_INDEX_0 + 4) {
            nextCmd = OBD.EC_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else if (cmdIndex == FIVE_MINUTE_LOOP_INDEX_0 + 5) {
            nextCmd = OBD.AT_CMD;
            cmdDelay = 100;
            cmdIndex++;
        } else {
            if (cmdIndex % 2 == 0) {
                nextCmd = OBD.SPD_CMD;
                cmdDelay = 500;
                cmdIndex++;
            } else {
                nextCmd = OBD.RPM_CMD;
                cmdDelay = 500;
                cmdIndex++;
            }
            if (cmdIndex > FINISH_ONE_CYCLE) {
                currentUploadTask = new UploadTask();
                currentUploadTask.execute();
                appendToConsole("Finish one cycle");
                cmdIndex = -1;
            }

        }

        if (cmdIndex != -1) {
            if (!nextCmd.equals("") && cmdDelay != -1) {
                obdHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBtMessage(nextCmd);
                    }
                }, cmdDelay);
            }
        }
    }

}


