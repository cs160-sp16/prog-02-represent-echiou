package com.echiou.represent;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by ethan on 3/2/16.
 */
public class PhoneToWatchService extends Service {
    private static final String REP = "/rep";
    private static final String OB_V_ROM = "/2012";

    private GoogleApiClient mApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        //initialize the googleAPIClient for message passing
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Which page are we starting? Grab this info from INTENT
        // which was passed over when we called startService
        Log.d("PhoneToWatch", "Starting");
        final Bundle extras = intent.getExtras();
        final String activityType = extras.getString("ACTIVITY_TYPE");

        if(activityType.contentEquals(REP)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //first, connect to the apiclient
                    mApiClient.connect();
                    //now that you're connected, send a massage with the cat name
                    String[] data = extras.getStringArray("NAME_AND_PARTY_MESSAGE");
                    sendMessage(activityType, data);
                }
            }).start();
        }
        else if (activityType.contentEquals(OB_V_ROM)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //first, connect to the apiclient
                    mApiClient.connect();
                    //now that you're connected, send a massage with the cat name
                    String[] data = {
                            extras.getString("OBAMA_MESSAGE"),
                            extras.getString("ROMNEY_MESSAGE"),
                            extras.getString("DIST_MESSAGE")
                    };
                    sendMessage(activityType, data);
                }
            }).start();
        }

        return START_STICKY;
    }

    @Override //remember, all services need to implement an IBiner
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage( final String path, final String[] data ) {
        //one way to send message: start a new thread and call .await()
        //see watchtophoneservice for another way to send a message
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    //we find 'nodes', which are nearby bluetooth devices (aka emulators)
                    //send a message for each of these nodes (just one, for an emulator)
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, stringArrToByteArr(data) ).await();
                    //4 arguments: api client, the node ID, the path (for the listener to parse),
                    //and the message itself (you need to convert it to bytes.)
                }
            }
        }).start();
    }

    public byte[] stringArrToByteArr(String[] stringArr) {
        byte[] byteArr = {};

        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final ObjectOutputStream objectOutputStream =
                    new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(stringArr);
            objectOutputStream.flush();
            objectOutputStream.close();
            byteArr = byteArrayOutputStream.toByteArray();
        } catch(Exception e) {
            Log.d("Error", e.toString());
        }

        return byteArr;
    }

}