package com.rice.momentocostruzionisolari;

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.companion.TaskCallback;

import java.util.Random;


public class BuddyController extends BuddyActivity {
    public Boolean finished_yes;
    public Boolean finished_no;
    public Boolean active=true;
    public Boolean yes_active=false;
    public Boolean positive=false;

    private final TaskCallback behaviorCallback = new TaskCallback() {
        @Override
        public void onStarted() {
            Log.i("BI","Started" );
        }

        @Override
        public void onSuccess(@NonNull String s) {
            Log.i("BI", "[BI][TASK] success "+s);

        }

        @Override
        public void onCancel() {
            Log.i("BI", "[BI][TASK] cancelled");

        }

        @Override
        public void onError(@NonNull String s) {
            Log.e("BI", "[BI][TASK] error "+s);

        }

        @Override
        public void onIntermediateResult(@NonNull String s) {
            Log.d("BI", "[BI][TASK] intermediate result "+s);
        }
    };

    //Run Buddy Emotional Behaviours
    public void runBehaviour(String bi){
        Log.i("BI", "Try running behaviour: "+bi);
        BuddySDK.Companion.createBICategoryTask(bi).start(behaviorCallback);
    }


    public int extract_random_angle(int min, int max){
        Random random = new Random();
        return  random.nextInt((max - min) + 1) + min; }

    public void enable_yes(Boolean yes){
        yes_active = yes;
    }

    public void start_autonomous_head_movements(){
        new Thread(() -> {
            active=true;
            finished_yes=false;
            finished_no=false;

            EnableYesMotor(1);
            EnableNoMotor(1);
            enableWheels(1,1);
            finished_yes=false;
            finished_no=false;

            while(active) {
                if(yes_active) HeadSayYes(20,extract_random_angle(-15,20));
                else finished_yes=true;
                int no_angle=extract_random_angle(-10,10);
                //Log.i("No angle", no_angle+"");
                HeadSayNo(20, no_angle);
                while (!finished_no || !finished_yes) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.i("main", "Thread interrupted", e);
                    }

                }

                finished_yes=false;
                finished_no=false;
                if (yes_active){
                    if(positive){
                        rotateBuddy(15,-10 );
                        positive=false;}
                    else{
                        rotateBuddy(15,10 );
                        positive=true;
                    }

                }
            }

            //Log.i("head","Publish HOME pose");
            //Log.i("head yes",BuddySDK.Actuators.getYesPosition()+"");
            //Log.i("head no",BuddySDK.Actuators.getNoPosition()+"");
            HeadSayYes(20,0);
            HeadSayNo(20,0);
            finished_yes=false;
            finished_no=false;
            while (!finished_no || !finished_yes) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.i("main", "Thread interrupted", e);
                }
            }
            Log.i("head yes post",BuddySDK.Actuators.getYesPosition()+"");
            Log.i("head no post",BuddySDK.Actuators.getNoPosition()+"");
        }).start();

    }


    public void stop_autonomous_movement(){
        active=false;
        /*
        StopNo();
        StopYes();
        EnableYesMotor(0);
        EnableNoMotor(0);*/
    }
    public void enableWheels(int turnOnLeftWheel, int turnOnRightWheel) {
        BuddySDK.USB.enableWheels(turnOnLeftWheel, turnOnRightWheel, new IUsbCommadRsp.Stub() {

            @Override
            public void onSuccess(String s) throws RemoteException {
                Log.i("W", "Wheels are enabled");
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                Log.i("W", "Wheels enable failed: " + s);
            }
        });
    }

    // Function to move Buddy
    public void moveBuddy(float speed, float distance) {
        BuddySDK.USB.moveBuddy(speed, distance, new IUsbCommadRsp.Stub() {

            @Override
            public void onSuccess(String s) throws RemoteException {
                Log.i("TAG", "Move: success");
                if ("WHEEL_MOVE_FINISHED".equals(s)) {
                    Log.i("W", "Move completed successfully");
                }
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                Log.i("TAG", "Move: failed : " + s);
            }
        });
    }



    // Function to rotate Buddy
    public void rotateBuddy(float speed, float degree) {
        BuddySDK.USB.rotateBuddy(speed, degree, new IUsbCommadRsp.Stub() {

            @Override
            public void onSuccess(String s) throws RemoteException {
                Log.i("RotateBuddy", s);
                if ("WHEEL_MOVE_FINISHED".equals(s)) {
                    Log.i("W", "Rotation completed successfully");
                }
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                Log.i("TAG", "Rotation failed : " + s);
            }
        });
    }


    public void StopNo() {
        BuddySDK.USB.buddyStopNoMove(new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {

            }

            @Override
            public void onFailed(String s) throws RemoteException {
            }
        });

    }

    public void StopYes() {
        BuddySDK.USB.buddyStopYesMove(new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {

            }

            @Override
            public void onFailed(String s) throws RemoteException {
            }
        });

    }

    public void EnableNoMotor(int state){
        BuddySDK.USB.enableNoMove(state, new IUsbCommadRsp.Stub() {
            @Override
            //if the motor succeeded to be enabled,we display motor is enabled
            public void onSuccess(String success) throws RemoteException {
                Log.i("TAG", "Motor Enabled");
                finished_no=true;
            }

            @Override
            //if the motor did not succeed to be enabled,we display motor failed to be enabled
            public void onFailed(String error) throws RemoteException {
                Log.i("Motor No", "No motor Enabled Failed");
            }
        });
    }

    public void EnableYesMotor(int state){
        BuddySDK.USB.enableYesMove(state, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i("TAG", "YES Motor Enabled");
                finished_yes=true;
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.i("Motor No", "Yes motor Enabled Failed");
            }
        });
    }

    public void HeadSayNo(float speed, float angle) {
        BuddySDK.USB.buddySayNo(speed, angle, new IUsbCommadRsp.Stub() { //function with speed, angle and stub callback

            @Override
            public void onSuccess(String s) throws RemoteException {
                //Log.i("No head success", s);
                if ("NO_MOVE_FINISHED".equals(s)) {
                    //Log.i("head movement finished", s);
                    finished_no=true;
                }
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                Log.i("No head fail", s);
            }
        });
    }


    public void HeadSayYes(float speed, float angle) {
        BuddySDK.USB.buddySayYes(speed, angle, new IUsbCommadRsp.Stub() { //function with speed, angle and stub callback
            @Override
            public void onSuccess(String s) throws RemoteException {
                //Log.i("No head success", s);
                if ("YES_MOVE_FINISHED".equals(s)) {
                    finished_yes=true;
                }
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                Log.i("CIPPA LIPPA", s);
            }

        });
    }


}

