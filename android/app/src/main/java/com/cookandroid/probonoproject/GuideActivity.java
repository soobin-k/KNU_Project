package com.cookandroid.probonoproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cookandroid.probonoproject.R;

import static android.speech.tts.TextToSpeech.ERROR;


public class GuideActivity extends AppCompatActivity {

    String TAG = "GuideActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView logCatcher;
    Button btnParied, btnSearch, replay;
    ListView listView;

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    private TextToSpeech tts;
    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothSocket btSocket = null;


   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.guidelayout);
       setTitle("Voice Compass");

       //다시 읽어주기 버튼
       replay = (Button) findViewById(R.id.replay);
       replay.setVisibility(View.INVISIBLE);
       replay.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               tts.setPitch(1.0f);
               tts.speak(logCatcher.getText().toString(),TextToSpeech.QUEUE_FLUSH, null);
           }
       });

       tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
           @Override
           public void onInit(int status) {
               if(status != ERROR) {
                   // 언어를 선택한다.
                   tts.setLanguage(Locale.KOREAN);
               }
               //사용안내
               tts.setPitch(1.0f);
               tts.speak("디바이스와 블루투스 연결해주세요", TextToSpeech.QUEUE_FLUSH, null);
           }
       });

       // Get permission
               String[] permission_list = {
                       Manifest.permission.ACCESS_FINE_LOCATION,
                       Manifest.permission.ACCESS_COARSE_LOCATION
               };
               ActivityCompat.requestPermissions(GuideActivity.this, permission_list, 1);

               // Enable bluetooth
               btAdapter = BluetoothAdapter.getDefaultAdapter();
               if (!btAdapter.isEnabled()) {
                   Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                   startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
               }

               // variables
               btnParied = (Button) findViewById(R.id.btn_paired);
               btnSearch = (Button) findViewById(R.id.btn_search);
               listView = (ListView) findViewById(R.id.listview);
               logCatcher = (TextView) findViewById(R.id.logCatcher);

               // Show paired devices
               btArrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1);
               deviceAddressArray = new ArrayList<>();
               listView.setAdapter(btArrayAdapter);

               listView.setOnItemClickListener(new myOnItemClickListener());

           }
           public void onClickButtonPaired(View view){
               btArrayAdapter.clear();
               if(deviceAddressArray!=null && !deviceAddressArray.isEmpty()){ deviceAddressArray.clear(); }
               pairedDevices = btAdapter.getBondedDevices();
               if (pairedDevices.size() > 0) {
                   // There are paired devices. Get the name and address of each paired device.
                   for (BluetoothDevice device : pairedDevices) {
                       String deviceName = device.getName();
                       String deviceHardwareAddress = device.getAddress(); // MAC address
                       btArrayAdapter.add(deviceName);
                       deviceAddressArray.add(deviceHardwareAddress);
                   }
               }
           }

           public void onClickButtonSearch(View view){
               // Check if the device is already discovering
               if(btAdapter.isDiscovering()){
                   btAdapter.cancelDiscovery();
               } else {
                   if (btAdapter.isEnabled()) {
                       btAdapter.startDiscovery();
                       btArrayAdapter.clear();
                       if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
                           deviceAddressArray.clear();
                       }
                       IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                       registerReceiver(receiver, filter);
                   } else {
                       Toast.makeText(getApplicationContext(), "bluetooth not on", Toast.LENGTH_SHORT).show();
                   }
               }
           }

           // Create a BroadcastReceiver for ACTION_FOUND.
           private final BroadcastReceiver receiver = new BroadcastReceiver() {
               public void onReceive(Context context, Intent intent) {
                   String action = intent.getAction();
                   if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                       // Discovery has found a device. Get the BluetoothDevice
                       // object and its info from the Intent.
                       BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                       String deviceName = device.getName();
                       String deviceHardwareAddress = device.getAddress(); // MAC address
                       btArrayAdapter.add(deviceName);
                       deviceAddressArray.add(deviceHardwareAddress);
                       btArrayAdapter.notifyDataSetChanged();
                   }
               }
           };
           ThreadConnectBTdevice myThreadConnectBTdevice;
           ThreadConnected myThreadConnected;

           class myOnItemClickListener implements AdapterView.OnItemClickListener {

               @Override
               public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                   Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

                   final String name = btArrayAdapter.getItem(position); // get name
                   final String address = deviceAddressArray.get(position); // get address
                   boolean flag = true;

                   BluetoothDevice device = btAdapter.getRemoteDevice(address);

                   //추가
                   myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                   myThreadConnectBTdevice.start();

               }
           }


           //추가
           private void startThreadConnected(BluetoothSocket socket) {
               myThreadConnected = new ThreadConnected(socket);
               myThreadConnected.start();
           }

            class ThreadConnectBTdevice extends Thread {
               private final BluetoothDevice bluetoothDevice;
               private BluetoothSocket bluetoothSocket;

               private ThreadConnectBTdevice(BluetoothDevice device) {
                   bluetoothSocket = null;
                   bluetoothDevice = device;
                   try {
                       this.bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }

               public void run() {
                   boolean success = false;
                   try {
                       bluetoothSocket.connect();
                   } catch (IOException e) {
                       Log.e("", e.getMessage());
                       try {
                           bluetoothSocket = (BluetoothSocket) bluetoothDevice.getClass().getMethod("createRfcommSocket", Integer.TYPE).invoke(this.bluetoothDevice, 1);
                           bluetoothSocket.connect();
                           success = true;
                       } catch (Exception e2) {
                           Log.e("", "Couldn't establish Bluetooth connection!");
                           try {
                               bluetoothSocket.close();
                           } catch (IOException e1) {
                               e1.printStackTrace();
                           }
                       }
                   }
                   if (success) {
                       final String msgconnected = "Connected to: " + bluetoothDevice.getName();
                       //Toast.makeText(getApplicationContext(), msgconnected, Toast.LENGTH_SHORT).show();

                       //runOnUiThread 내용 필요없음... 연결상태 출력 부... 토스트메시지로 대체


                       runOnUiThread(new Runnable() {
                           public void run() {
                               logCatcher.setText(msgconnected);
                               listView.setVisibility(View.INVISIBLE);
                               btnParied.setVisibility(View.INVISIBLE);
                               btnSearch.setVisibility(View.INVISIBLE);

                           }
                       });

                       startThreadConnected(bluetoothSocket);
                   }
               }

               public void cancel() {
                   Toast.makeText(getApplicationContext(), "Bluetooth Socket closed!!!", Toast.LENGTH_SHORT).show();
                   try {
                       bluetoothSocket.close();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }
            class ThreadConnected extends Thread {
               private final BluetoothSocket connectedBluetoothSocket;
               private final InputStream connectedInputStream;
               private final OutputStream connectedOutputStream;


               public ThreadConnected(BluetoothSocket socket) {
                   connectedBluetoothSocket = socket;
                   InputStream in = null;
                   OutputStream out = null;
                   try {
                       in = socket.getInputStream();
                       out = socket.getOutputStream();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                   connectedInputStream = in;
                   connectedOutputStream = out;
               }

               public void run() {
                   byte[] buffer = new byte[1024];
                   while (true) {
                       try {
                           final String msgReceived = new String(buffer, 0, connectedInputStream.read(buffer));
                           //Toast.makeText(getApplicationContext(),msgReceived,Toast.LENGTH_SHORT).show();

                           //runOnUiThread 내용 필요없음...

                           runOnUiThread(new Runnable() {
                               public void run() {

                                   //**********************************************************************
                                   //********************logCatcher 에 블루투스로 전달받은 값이 출력*********
                                    //Toast.makeText(getApplicationContext(),msgReceived,Toast.LENGTH_SHORT).show();
                                   String temp = "";
                                   replay.setVisibility(View.VISIBLE);
                                   temp = logCatcher.getText().toString();
                                   logCatcher.setText(msgReceived);
                                   tts.setPitch(1.0f);
                                   tts.speak(msgReceived,TextToSpeech.QUEUE_FLUSH, null);
                                   //**********************************************************************
                               }
                           });

                       } catch (IOException e) {
                           e.printStackTrace();
                           final String msgConnectionLost = "Connection lost:\n" + e.getMessage();
                           Toast.makeText(getApplicationContext(),msgConnectionLost,Toast.LENGTH_SHORT).show();
                    /*
                    runOnUiThread(new Runnable() {
                        public void run() {
                            textStatus.setText(msgConnectionLost);
                        }
                    });
                    */
                       }
                   }
               }

               public void write(byte[] buffer) {
                   try {
                       connectedOutputStream.write(buffer);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }

               public void cancel() {
                   try {
                       connectedBluetoothSocket.close();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }


           }
   }

