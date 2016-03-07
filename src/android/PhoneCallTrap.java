package ru.twashtar.ignorecall;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONArray;


public class PhoneCallTrap extends CordovaPlugin {

    CallStateListener listener;

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        prepareListener();

        listener.setCallbackContext(callbackContext);

        return true;
    }

    private void prepareListener() {
        if (listener == null) {
            listener = new CallStateListener();
            TelephonyManager TelephonyMgr = (TelephonyManager) cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            TelephonyMgr.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }
}

class CallStateListener extends PhoneStateListener {

    private CallbackContext callbackContext;

    public void setCallbackContext(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        if (callbackContext == null) return;

        String msg = "";

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
            msg = "IDLE";
            break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
            msg = "OFFHOOK";
            break;

            case TelephonyManager.CALL_STATE_RINGING:
            msg = "RINGING";
//            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
//	    i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
//            KeyEvent.KEYCODE_HEADSETHOOK));
//	    context.sendOrderedBroadcast(i, null);
            ignoreCall();
            exitCleanly();
            break;
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, msg);
        result.setKeepCallback(true);

        callbackContext.sendPluginResult(result);
    }
}

private void ignoreCall() {
    if (USE_ITELEPHONY)
        ignoreCallAidl();
    else
        ignoreCallPackageRestart();
}
/**
 * AIDL/ITelephony technique for ignoring calls
 */
private void ignoreCallAidl() {
    try {
        // telephonyService.silenceRinger();

        telephonyService.endCall();
    } catch (RemoteException e) {
        e.printStackTrace();
        Log.d(tag, "ignoreCall: " + "Error: " + e.getMessage());

    } catch (Exception e) {
        e.printStackTrace();
        Log.d(tag, "ignoreCall" + "Error: " + e.getMessage());

    }
}
/**
 * package restart technique for ignoring calls
 */
private void ignoreCallPackageRestart() {
    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    am.restartPackage("com.android.providers.telephony");
    am.restartPackage("com.android.phone");
}
/**
 * cleanup and exit routine
 */
private void exitCleanly() {
    unHookReceiver();
    this.finish();

}