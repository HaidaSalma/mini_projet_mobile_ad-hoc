package ma.ensias.mini_projet_mobile;



import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {





    TextView OutPut;
    Button DiscoverButton, InfoButton, InfoSendButton;
    ListView WifiP2Plist;
    EditText message;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;

    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNames;
    WifiP2pDevice[] devices;


    double Puissance;
    double NiveauDeBatteri;
    double debit;
    int receivedMessages;
    int transmittedMessages;


    Server Server;
    Client Client;
    //SendReceive sendReceive;
    String userType;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OutPut = findViewById(R.id.texte);
        DiscoverButton = findViewById(R.id.Button);
        WifiP2Plist = findViewById(R.id.list);
        InfoButton = findViewById(R.id.InfoButton);
        InfoSendButton=findViewById(R.id.InfoSendButton);
        message=findViewById(R.id.messageReçu);



        //this class provides the API for managing Wi-Fi peer-to-peer connectivity
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        //A channel that connects the application to the Wi-Fi P2P framework.
        //Most P2P operations require a Channel as an argument.

        /* The API is asynchronous and responses to requests from an application are on
         listener callbacks provided by the application. The application needs to
         do an initialization with initialize(Context, Looper, ChannelListener)
         before doing any p2p operation.
         */
        mChannel = mManager.initialize(this, getMainLooper(), null);
        //initialize object mReceiver (BroadcastReceiver)
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        //inisialize the intentFilter
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

///////////ETAPE 1 : DISCOVER//////////////////////////////////////////
        //lors de clique sur le button decouvrir
        DiscoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //operation de discovery commence
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        OutPut.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {


                        String errorMessage;
                        switch (i) {
                            case WifiP2pManager.BUSY:
                                errorMessage = "Busy";
                                break;
                            case WifiP2pManager.ERROR:
                                errorMessage = "Internal Error.";
                                break;
                            case WifiP2pManager.P2P_UNSUPPORTED:
                                errorMessage = "wifi direct not exisst ";
                                break;
                            default:
                                errorMessage = "unknown";
                                break;
                        }
                        OutPut.setText("Discovery Starting failed \n"+ errorMessage);
                    }
                });
            }
        });

        /////////////////////Etape2 : Connect to a peer///////////////////////

        //apres le clique sur un choix de la list des peers
        WifiP2Plist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //recouperer le device choisi sur object device
                final WifiP2pDevice device = devices[i];
                WifiP2pConfig config = new WifiP2pConfig();
                //The device MAC address uniquely identifies a Wi-Fi p2p device
                config.deviceAddress = device.deviceAddress;



                //The device name is a user friendly string to identify a Wi-Fi p2p device
                ////device.deviceName;
                //Device connection status
                ////device.status


                //Connection reussit ou failed?
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        ////////////Etape 3 : Affichage des info QoS//////////////////////////:

        //recouperer QoS of the device
        //batteri
        Intent batteryIntent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        NiveauDeBatteri = (int) (level * 100 / (float) scale);

        //puissance


        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Puissance = wifiInfo.getRssi();

        //DEbit
        //Debit:
        debit = (double) (TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes());


        InfoButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                OutPut.setText("QoS of your device :  Battery Level = " + NiveauDeBatteri +
                        " \n Puissance = "+ Puissance +
                        " \n Debit = "+ debit);

            }
        });
        //Button to send data
        InfoSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*String msg="QoS of your device :  Battery Level = " + NiveauDeBatteri +
                        " \n Puissance = "+ Puissance +
                        " \n Debit = "+ debit;
                //String msg = "Hello";*/
                String msg=message.getText().toString();
                byte[] bytes =msg.getBytes();
                InfoSendButton.setVisibility(View.INVISIBLE);
                if(userType.equals("server")) {
                    Server.writeData(bytes);
                } else {
                    Client.writeData(bytes);
                }

            }
        });
    }



    public WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            //au cas ou list des peers est renouvllé
            if(!peerList.getDeviceList().equals(peers)){
                //clear the list
                peers.clear();
                //renouvller list
                peers.addAll(peerList.getDeviceList());

                //inisialiser la size de array "deviceNames" et "devices"
                deviceNames=new String[peerList.getDeviceList().size()];
                devices=new WifiP2pDevice[peerList.getDeviceList().size()];

                int index=0;
                //remplire les deux listes par les peers decouvertes
                for(WifiP2pDevice device : peerList.getDeviceList()){
                    deviceNames[index]=device.deviceName;
                    devices[index]=device;
                    index++;
                }
                //inserer les devicesNames on the list qui va etre afficher à l'ecran
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);
                WifiP2Plist.setAdapter(adapter);
            }
            //au cas où aucune devices found
            if(peers.size()==0){
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
            }

        }
    };

    //get information about the device Host or Client
    public WifiP2pManager.ConnectionInfoListener ConnectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                OutPut.setText("Client");
                userType="Client";
                //start the server
                Server = new Server();
                //Server.start();
                //later
            } else if (wifiP2pInfo.groupFormed) {
                OutPut.setText("Server");
                userType="Server";
                //start the client
                Client = new Client(groupOwnerAddress);
                //Client.start();
                //Later
            }
        }
    };



    //register the broadcast receiver

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        mManager.discoverPeers(mChannel, null);
        mManager.requestPeers(mChannel, peerListListener);
    }

    //unregister the broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
        mManager.stopPeerDiscovery(mChannel, null);
    }
/*
//////////Etape 4 : PREPAR TO START COMMUNECATION////////////

    //add a inner class for server
    //Server Thread
    public class Server extends Thread{
        Socket socket;
        ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            serverSocket=new ServerSocket(8888);
            socket=serverSocket.accept();
            //when the socket will be accepted
            sendReceive=new SendReceive(socket);
            sendReceive.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

/*
    //add a inner class for client
    //Client Thread
    public class Client extends Thread{
        Socket socket;
        String hostAdd;

        public Client(InetAddress hostAddress){
            hostAdd=hostAddress.getHostAddress();
            socket=new Socket();
        }

        @Override
        public void run() {

            try {
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                //when the connection will be accepted
                SendReceive sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
//SEndReceive
    //Create handler
    static final int MessageRead=1;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch(message.what){
                case MessageRead:
                    byte[] readBuff=(byte[]) message.obj;
                    String tempMsg = new String(readBuff,0,message.arg1);
                    OutPut.setText(tempMsg);
                    break;

            }
            return true;
        }
    });

    //Create class SendReceive
    //SendReceive Thread
    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket sct){
            socket=sct;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //receive message
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while(socket!=null){
                try{
                    bytes=inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MessageRead,bytes,-1,buffer).sendToTarget();

                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        //send message
        public  void write(byte[] bytes){
            try{
                outputStream.write(bytes);
            }catch (IOException e){
                e.printStackTrace();
            }
        }


    }
    */
//////handler//////////////

    static final int MessageRead=1;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch(message.what){
                case MessageRead:
                    byte[] readBuff=(byte[]) message.obj;
                    String tempMsg = new String(readBuff,0,message.arg1);
                    OutPut.setText(tempMsg);
                    break;

            }
            return true;
        }
    });

    ////////////////SERVER//////////////////
    public class Server extends AsyncTask<String, Integer, Boolean> {
        Socket socket;
        ServerSocket serverSocket;
        InputStream inputStream;
        OutputStream outputStream;

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = true;
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
            } catch (IOException e) {
                result = false;
                e.printStackTrace();
            }
            return result;
        }

        public void writeData(final byte[] bytes) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            InfoSendButton.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //listener
                new Thread(new Runnable() {
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int x;
                        while (socket != null) {
                            try {
                                x = inputStream.read(buffer);
                                if (x > 0) {
                                    handler.obtainMessage(MessageRead, x, -1, buffer).sendToTarget();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                InfoSendButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(), "could not create sockets", Toast.LENGTH_SHORT).show();
                //restart socket assignment process
            }
        }
    }

    ///////////////::/Client////////////////////////:
    public class Client extends AsyncTask<String, Integer, Boolean> {
        Socket socket;
        String hostAdd;
        InputStream inputStream;
        OutputStream outputStream;

        public Client(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = false;
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 5000);
                result = true;
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                return result;
            }
        }

        public void writeData(final byte[] bytes) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            InfoSendButton.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Thread(new Runnable(){
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int x;
                        while (socket!=null) {
                            try {
                                x = inputStream.read(buffer);
                                if(x>0) {
                                    handler.obtainMessage(MessageRead,x,-1,buffer).sendToTarget();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                InfoSendButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(),"could not create sockets",Toast.LENGTH_SHORT).show();
                //restart socket assignment process
            }
        }
    }
}