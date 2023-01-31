package com.nzme.nielsen;

import android.content.Context;
import android.util.Log;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.nielsen.app.sdk.AppSdk;
import com.nielsen.app.sdk.IAppNotifier;
import com.nielsen.app.sdk.AppLaunchMeasurementManager;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Date;
import java.util.HashMap;

/**
 * NielsenNzme is the class briding between react JS code and the Nielsen App
 * SDK.
 */
public class NielsenNzme extends ReactContextBaseJavaModule implements IAppNotifier {

    /**
    */
    Context mContext;
    private static final String NIELSEN_TAG = "NielsenAppApiBridge";
    private static final String INSTANCE_PREFIX = "Instance_";
    private HashMap<String, AppSdk> mAppSdkInstances = new HashMap();

    private static final String ID_KEY = "id";
    private static final String OPTOUT_URL_KEY = "optouturl";
    private static final String METER_VERSION_KEY = "meter_version";
    private static final String APP_DISABLE_KEY = "app_disable";
    private static final String OPTOUT_STATUS_KEY = "user_optout";
    private static final String DEVICE_ID_KEY = "demographic_id";
    private static final String NIELSEN_ID_KEY = "nielsen_id";

    // Event Names emitted from native android code
    public static final String EVENT_INIT = "EVENT_INIT"; // {"id", "instance_id"}
    public static final String EVENT_OPTOUT_URL = "EVENT_OPTOUT_URL"; // {"optouturl", "optout page url"}
    public static final String EVENT_METER_VERSION = "EVENT_METER_VERSION"; // {"optouturl", "optout page url"}
    public static final String EVENT_APP_DISABLE = "EVENT_APP_DISABLE"; // {"optouturl", "optout page url"}
    public static final String EVENT_OPTOUT_STATUS = "EVENT_OPTOUT_STATUS"; // {"optouturl", "optout page url"}
    public static final String EVENT_DEVICE_ID = "EVENT_DEMOGRAPHIC_ID"; // {"optouturl", "optout page url"}
    public static final String EVENT_NIELSEN_ID = "EVENT_NIELSEN_ID";

    /**
     * Constructor for the NielsenNzme Adds the new instance as
     * LifeCycleEventListener for conttex
     * 
     * @param context The react application context
     */
    public NielsenNzme(ReactApplicationContext context) {
        super(context);
        mContext = context;
    }

    /**
     * Implements getName method from ReactContextBaseJavaModule
     * 
     * @return The constant string 'NielsenNzme'
     */
    @Override
    public String getName() {
        return "NielsenNzme";
    }

    /**
     * Implements getName method from IAppNotifier
     */
    @Override
    public void onAppSdkEvent(long l, int i, String s) {}

    /**
     * Initializes the module for use
     * 
     * @param obj An instance of ReadableMap (mapped JS object) containing
     *            initialization meta data
     */
    @ReactMethod
    public void createInstance(final ReadableMap obj) {
        try {
            JSONObject appSdkConfig = readableMapToJSONObject(obj);
            AppSdk mAppSdk = new AppSdk(getReactApplicationContext(), appSdkConfig, this);
            if (!mAppSdk.isValid()) {
                Log.e(NIELSEN_TAG, "SDK instance creation failed");
            } else {
                Log.d(NIELSEN_TAG, "SDK instance created successfully");
                String id = INSTANCE_PREFIX + mAppSdkInstances.size();
                mAppSdkInstances.put(id, mAppSdk);
                WritableMap params = Arguments.createMap();
                params.putString(ID_KEY, id);
                emitEvent(EVENT_INIT, params);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "SDK Init failed : " + ex.getLocalizedMessage());
        }
    }

    /**
     * Remove the sdk instance
     * 
     * @param id An instance id of object to be removed
     */
    @ReactMethod
    public void removeInstance(final String id) {
        try {
            if (id != null && mAppSdkInstances.containsKey(id)) {
                mAppSdkInstances.get(id).close();
                mAppSdkInstances.remove(id);
                Log.d(NIELSEN_TAG, "SDK instance closed successfully");
            } else {
                Log.e(NIELSEN_TAG, "SDK instance not exists for close operation.");
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "SDK instance close failed : " + ex.getLocalizedMessage());
        }
    }

    /**
     * Emit's the event to React-native code Needs to be catched in JS
     */
    public void emitEvent(String eventName, WritableMap params) {
        if (null != params) {

            getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    /**
     * Static method to convert ReadableMap instances to JSONObject instances (used
     * by the Nielsen SDK)
     * 
     * @param obj The ReadableMap instance to be converted
     * @return An instance of JSONObject containing the mapped JS object values (if
     *         successful, empty otherwise)
     */
    static private JSONObject readableMapToJSONObject(final ReadableMap obj) {
        JSONObject ret = new JSONObject();
        try {
            ReadableMapKeySetIterator it = obj.keySetIterator();
            while (it.hasNextKey()) {
                String key = it.nextKey();
                ret.put(key, obj.getDynamic(key).asString());
            }
        } catch (JSONException ex) {
            Log.e(NIELSEN_TAG, "Exception in readableMapToJSONObject " + ex.getMessage());
        }
        return ret;
    }

    /**
     * This function returns NielsenEventTracker instance for id from map
     * 
     * @param id An instance id of object
     */
    AppSdk getInstance(String id) {
        AppSdk result = null;
        try {
            if (id != null && mAppSdkInstances.containsKey(id)) {
                result = mAppSdkInstances.get(id);
            } else {
                Log.e(NIELSEN_TAG, "SDK instance not exists for id = " + id);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in getInstance() : " + ex.getLocalizedMessage());
        }
        return result;
    }

    /**
     * Wrapper for the Nielsen SDK play method. Simply forwards calls to the SDK.
     * 
     * @param obj ReadableMap instance containing the mapped JS meta data object,
     *            e.g. {channelName: 'channel name here'}
     */
    @ReactMethod
    public void play(final String id, final ReadableMap obj) {
        try {
            if (null != id && getInstance(id) != null) {
                JSONObject playObject = readableMapToJSONObject(obj);
                getInstance(id).play(playObject);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in play " + ex.getMessage());
        }
    }

    /**
     * Wrapper for the Nielsen SDK loadMetadata method. Simply forwards calls to the
     * SDK.
     * 
     * @param obj ReadableMap instance containing the mapped JS meta data object
     */
    @ReactMethod
    public void loadMetadata(final String id, final ReadableMap obj) {
        try {
            if (null != id && getInstance(id) != null) {
                JSONObject contentMetadata = readableMapToJSONObject(obj);
                getInstance(id).loadMetadata(contentMetadata);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in loadMetadata " + ex.getMessage());
        }
    }

    /**
     * Wrapper for the Nielsen SDK loadMetadata method. Simply forwards calls to the
     * SDK.
     * 
     * @param obj ReadableMap instance containing the mapped JS meta data object
     */
    @ReactMethod
    public void sendID3(final String id, final String payload) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).sendID3(payload);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in sendID3 " + ex.getMessage());
        }
    }

    /**
     * Wrapper for the Nielsen SDK loadMetadata method. Simply forwards calls to the
     * SDK.
     * 
     * @param obj ReadableMap instance containing the mapped JS meta data object
     */
    @ReactMethod
    public void appDisableApi(final String id, final boolean disabled) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).appDisableApi(disabled);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in appDisableApi " + ex.getMessage());
        }
    }

    /**
     * Wrapper for the Nielsen SDK setPlayheadPosition method. Simply forwards calls
     * to the SDK.
     * 
     * @param ph The current playhead position
     */
    @ReactMethod
    public void setPlayheadPosition(final String id, final Double ph) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).setPlayheadPosition(ph.longValue());
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in setPlayheadPosition " + ex.getMessage());
        }
    }

    /**
     * Wrapper for the Nielsen SDK stop method. Simply forwards calls to the SDK.
     */
    @ReactMethod
    public void stop(final String id) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).stop();
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in stop " + ex.getMessage());
        }
    }

    /**
     * Wrapper for the Nielsen SDK end method. Simply forwards calls to the SDK.
     */
    @ReactMethod
    public void end(String id) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).end();
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in end " + ex.getMessage());
        }
    }

    /**
     * Wrapper to retrieve the optOutUrl from the Nielsen SDK. Emits a "OptOutUrl"
     * event with the url as payload. Needs to be catched in JS
     */
    @ReactMethod
    public void userOptOutURLString(String id) {
        WritableMap params = Arguments.createMap();
        try {
            if (null != id && getInstance(id) != null) {
                params.putString(OPTOUT_URL_KEY, getInstance(id).userOptOutURLString());
                emitEvent(EVENT_OPTOUT_URL, params);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in optOutUrl " + ex.getMessage());
        }
    }

    /**
     * Wrapper to retrieve the optOutUrl from the Nielsen SDK. Emits a "OptOutUrl"
     * event with the url as payload. Needs to be catched in JS
     */
    @ReactMethod
    public void getOptOutStatus(String id) {
        WritableMap params = Arguments.createMap();
        try {
            if (null != id && getInstance(id) != null) {
                params.putString(OPTOUT_STATUS_KEY, "" + getInstance(id).getOptOutStatus());
                emitEvent(EVENT_OPTOUT_STATUS, params);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in getOptOutStatus " + ex.getMessage());
        }
    }

    /**
     * Wrapper to retrieve the optOutUrl from the Nielsen SDK. Emits a "OptOutUrl"
     * event with the url as payload. Needs to be catched in JS
     */
    @ReactMethod
    public void getNielsenId(String id) {
        WritableMap params = Arguments.createMap();
        try {
            if (null != id && getInstance(id) != null) {
                params.putString(NIELSEN_ID_KEY, getInstance(id).getNielsenId());
                emitEvent(EVENT_NIELSEN_ID, params);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in optOutUrl " + ex.getMessage());
        }
    }

    /**
     * Wrapper to retrieve the optOutUrl from the Nielsen SDK. Emits a "OptOutUrl"
     * event with the url as payload. Needs to be catched in JS
     */
    @ReactMethod
    public void getDemographicId(String id) {
        WritableMap params = Arguments.createMap();
        try {
            if (null != id && getInstance(id) != null) {
                params.putString(DEVICE_ID_KEY, getInstance(id).getDemographicId());
                emitEvent(EVENT_DEVICE_ID, params);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in getDemographicId " + ex.getMessage());
        }
    }

    @ReactMethod
    public void getMeterVersion(String id) {
        WritableMap params = Arguments.createMap();
        try {

            params.putString(METER_VERSION_KEY, AppSdk.getMeterVersion());
            emitEvent(EVENT_METER_VERSION, params);
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in getMeterVersion " + ex.getMessage());
        }
    }

    @ReactMethod
    public void getAppDisable(String id) {
        WritableMap params = Arguments.createMap();
        try {
            if (null != id && getInstance(id) != null) {
                params.putString(APP_DISABLE_KEY, "" + getInstance(id).getAppDisable());
                emitEvent(EVENT_APP_DISABLE, params);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in getAppDisable " + ex.getMessage());
        }
    }

    @ReactMethod
    public void setDebug(char debugState) {
        try {
            AppSdk.setDebug(debugState);
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in setDebug " + ex.getMessage());
        }
    }

    @ReactMethod
    public void suspend(String id) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).suspend();
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in suspend " + ex.getMessage());
        }
    }

    @ReactMethod
    public void appInBackground(String id) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).appInBackground(mContext);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in appInBackground" + ex.getMessage());
        }
    }

    @ReactMethod
    public void appInForeground(String id) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).appInForeground(mContext);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in appInForeground " + ex.getMessage());
        }
    }

    @ReactMethod
    public void updateOTT(String id, final ReadableMap obj) {
        try {
            if (null != id && getInstance(id) != null) {
                JSONObject ottData = readableMapToJSONObject(obj);
                getInstance(id).updateOTT(ottData);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in updateOTT " + ex.getMessage());
        }
    }

    @ReactMethod
    public void free(String id) {
        try {
            if (null != id && getInstance(id) != null) {
                getInstance(id).close();
                mAppSdkInstances.remove(id);
            }
        } catch (Exception ex) {
            Log.e(NIELSEN_TAG, "Exception in appInForeground " + ex.getMessage());
        }
    }

    // Required for rn built in EventEmitter Calls.
    @ReactMethod
    public void addListener(String eventName) {

    }

    @ReactMethod
    public void removeListeners(Integer count) {

    }
}