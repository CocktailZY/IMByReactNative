package rnxmpp.service;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.UnparsedIQ;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.util.ArrayList;
import java.util.List;

import rnxmpp.utils.JSONSortUtil;
import rnxmpp.utils.Parser;

import static com.facebook.react.common.ReactConstants.TAG;

/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */

public class RNXMPPCommunicationBridge implements XmppServiceListener {

    public static final String RNXMPP_ERROR =       "RNXMPPError";
    public static final String RNXMPP_LOGIN_ERROR = "RNXMPPLoginError";
    public static final String RNXMPP_MESSAGE =     "RNXMPPMessage";
    public static final String RNXMPP_ROSTER =      "RNXMPPRoster";
    public static final String RNXMPP_IQ =          "RNXMPPIQ";
    public static final String RNXMPP_UnparsedIQ =  "RNXMPPUnparsedIQ";
    public static final String RNXMPP_PRESENCE =    "RNXMPPPresence";
    public static final String RNXMPP_CONNECT =     "RNXMPPConnect";
    public static final String RNXMPP_DISCONNECT =  "RNXMPPDisconnect";
    public static final String RNXMPP_LOGIN =       "RNXMPPLogin";
    ReactContext reactContext;

    public RNXMPPCommunicationBridge(ReactContext reactContext) {
        this.reactContext = reactContext;
    }

    @Override
    public void onError(Exception e) {
        sendEvent(reactContext, RNXMPP_ERROR, e.getLocalizedMessage());
    }

    @Override
    public void onLoginError(String errorMessage) {
        sendEvent(reactContext, RNXMPP_LOGIN_ERROR, errorMessage);
    }

    @Override
    public void onLoginError(Exception e) {
        this.onLoginError(e.getLocalizedMessage());
    }

    @Override
    public void onMessage(Message message) {
//        WritableMap params = Arguments.createMap();
//        params.putString("thread", message.getThread());
//        params.putString("subject", message.getSubject());
//        params.putString("body", message.getBody());
//        params.putString("from", message.getFrom());
//        params.putString("src", message.toXML().toString());
//        sendEvent(reactContext, RNXMPP_MESSAGE, params);

        String temp = message.toXML().toString();
        Log.e(TAG,"messageXML-------------------------");
        Log.e(TAG,temp);
        try{
            JSONObject obj = XML.toJSONObject(temp);
            Log.e(TAG,"message-------------------------");
            Log.e(TAG,obj.toString());
            WritableMap params = Arguments.createMap();
            params.putString("thread", message.getThread());
            params.putString("subject", message.getSubject());
            params.putString("body", message.getBody());
            params.putString("from", message.getFrom());
            params.putString("src", message.toXML().toString());
            obj=obj.getJSONObject("message");
            if(obj.has("x")){
                if(obj.getJSONObject("x").has("status")){
                    Object tempobj = obj.getJSONObject("x").get("status");
                    params.putString("code", tempobj.toString());
                    /*if(tempobj instanceof String){
                        presenceMap.putString("code", tempobj.toString());
                    }else if(tempobj instanceof JSONArray){
                        presenceMap.putString("code", tempobj.toString());
                    }*/
                }
            }else{
                params.putString("code", null);
            }
            sendEvent(reactContext, RNXMPP_MESSAGE, params);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onRosterReceived(Roster roster) {
        WritableArray rosterResponse = Arguments.createArray();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            WritableMap rosterProps = Arguments.createMap();
            rosterProps.putString("username", rosterEntry.getUser());
            rosterProps.putString("displayName", rosterEntry.getName());
            WritableArray groupArray = Arguments.createArray();
            for (RosterGroup rosterGroup : rosterEntry.getGroups()) {
                groupArray.pushString(rosterGroup.getName());
            }
            rosterProps.putArray("groups", groupArray);
            rosterProps.putString("subscription", rosterEntry.getType().toString());
            rosterResponse.pushMap(rosterProps);
        }
        sendEvent(reactContext, RNXMPP_ROSTER, rosterResponse);
    }

    @Override
    public void onIQ(IQ iq) {
        sendEvent(reactContext, RNXMPP_IQ, Parser.parse(iq.toString()));
    }

    @Override
    public void onUnparsedIQ(UnparsedIQ iq) {
        Log.e(TAG,iq.getContent().toString());
        try{
            JSONObject obj = XML.toJSONObject(iq.getContent().toString());
            JSONObject temp = new JSONObject();
            Log.e(TAG,"78945---------");
            Log.e(TAG,obj.toString());
            if(obj.has("chat")){
                List<JSONObject> list = JSONSortUtil.getJSONs(obj.getString("chat"));
                temp.put("set",obj.getJSONObject("chat").has("set") ? obj.getJSONObject("chat").get("set") : "");
                JSONSortUtil.bubbleSort(list,list.size());
                List<JSONObject> list1 = new ArrayList<JSONObject>();
                if(list.size() > 0){
                    list1 = JSONSortUtil.addTime(list,"1");
                }
                temp.put("list",list1);

                Log.e(TAG,"78945------jjjjjjjjjjjj---");
                Log.e(TAG,temp.toString());
                sendEvent(reactContext, RNXMPP_UnparsedIQ, temp.toString());
            }else{
                temp.put("list",new ArrayList<>());
                sendEvent(reactContext, RNXMPP_UnparsedIQ, temp.toString());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPresence(Presence presence){

        String temp = presence.toXML().toString();
        try{
            JSONObject obj = XML.toJSONObject(temp);
            Log.e(TAG,"presence----------12121-----------------------");
            Log.e(TAG,obj.toString());
            Log.e(TAG,obj.has("x")+"");
            WritableMap presenceMap = Arguments.createMap();
            presenceMap.putString("type", presence.getType().toString());
            presenceMap.putString("from", presence.getFrom());
            presenceMap.putString("status", presence.getStatus());
            presenceMap.putString("mode", presence.getMode().toString());
            obj=obj.getJSONObject("presence");
            if(obj.has("x")){
                if(obj.getJSONObject("x").has("status")){
                    Object tempobj = obj.getJSONObject("x").get("status");
                    presenceMap.putString("code", tempobj.toString());
                    /*if(tempobj instanceof String){
                        presenceMap.putString("code", tempobj.toString());
                    }else if(tempobj instanceof JSONArray){
                        presenceMap.putString("code", tempobj.toString());
                    }*/
                }
            }else{
                presenceMap.putString("code", null);
            }
            sendEvent(reactContext, RNXMPP_PRESENCE, presenceMap);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onConnnect(String username, String password) {
        WritableMap params = Arguments.createMap();
        params.putString("username", username);
        params.putString("password", password);
        sendEvent(reactContext, RNXMPP_CONNECT, params);
    }

    @Override
    public void onDisconnect(Exception e) {
        if (e != null) {
            sendEvent(reactContext, RNXMPP_DISCONNECT, e.getLocalizedMessage());
        } else {
            sendEvent(reactContext, RNXMPP_DISCONNECT, null);
        }
    }

    @Override
    public void onLogin(String username, String password) {
        WritableMap params = Arguments.createMap();
        params.putString("username", username);
        params.putString("password", password);
        sendEvent(reactContext, RNXMPP_LOGIN, params);
    }

    void sendEvent(ReactContext reactContext, String eventName, @Nullable Object params) {
        reactContext
                .getJSModule(RCTNativeAppEventEmitter.class)
                .emit(eventName, params);
    }
}
