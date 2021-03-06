/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network;

import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDict;
import org.appcelerator.titanium.TiModule;
import org.appcelerator.titanium.TiProxy;
import org.appcelerator.titanium.kroll.KrollCallback;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NetworkModule extends TiModule {

	private static final String LCAT = "TiNetwork";
	private static final boolean DBG = TiConfig.LOGD;

	public static final String EVENT_CONNECTIVITY = "change";
	public static final String NETWORK_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 1.1; en-us; generic) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2";

	public static final int NETWORK_NONE = 0;
	public static final int NETWORK_WIFI = 1;
	public static final int NETWORK_MOBILE = 2;
	public static final int NETWORK_LAN = 3;
	public static final int NETWORK_UNKNOWN = 4;

    public enum State {
        UNKNOWN,

        /** This state is returned if there is connectivity to any network **/
        CONNECTED,
        /**
         * This state is returned if there is no connectivity to any network. This is set
         * to true under two circumstances:
         * <ul>
         * <li>When connectivity is lost to one network, and there is no other available
         * network to attempt to switch to.</li>
         * <li>When connectivity is lost to one network, and the attempt to switch to
         * another network fails.</li>
         */
        NOT_CONNECTED
    }

	class NetInfo {
		public State state;
		public boolean failover;
		public String typeName;
		public int type;
		public String reason;

		public NetInfo() {
			state = State.UNKNOWN;
			failover = false;
			typeName = "NONE";
			type = -1;
			reason = "";
		}
	};

	private NetInfo lastNetInfo;

	private boolean isListeningForConnectivity;
	private TiNetworkListener networkListener;
	private ConnectivityManager connectivityManager;

	private Handler messageHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Bundle b = msg.getData();

			boolean connected = b.getBoolean(TiNetworkListener.EXTRA_CONNECTED);
			int type = b.getInt(TiNetworkListener.EXTRA_NETWORK_TYPE);
			String typeName = b.getString(TiNetworkListener.EXTRA_NETWORK_TYPE_NAME);
			boolean failover = b.getBoolean(TiNetworkListener.EXTRA_FAILOVER);
			String reason = b.getString(TiNetworkListener.EXTRA_REASON);

			// Set last state
			synchronized(lastNetInfo) {
				if (connected) {
					lastNetInfo.state = State.CONNECTED;
				} else {
					lastNetInfo.state = State.NOT_CONNECTED;
				}
				lastNetInfo.type = type;
				lastNetInfo.typeName = typeName;
				lastNetInfo.failover = failover;
				lastNetInfo.reason = reason;
			}

			TiDict data = new TiDict();
			data.put("online", connected);
			int titaniumType = networkTypeToTitanium(connected, type);
			data.put("networkType", titaniumType);
			data.put("networkTypeName", networkTypeToTypeName(titaniumType));
			data.put("reason", reason);
			fireEvent(EVENT_CONNECTIVITY, data);
		}
	};

	public NetworkModule(TiContext tiContext)
	{
		super(tiContext);

		this.lastNetInfo = new NetInfo();
		this.isListeningForConnectivity = false;
		//TODO set titanium version
		setDynamicValue("userAgent", NETWORK_USER_AGENT + " Titanium/0.9.0");

		tiContext.addOnLifecycleEventListener(this);
		tiContext.addOnEventChangeListener(this);
	}


	@Override
	public void listenerAdded(String type, int count, TiProxy proxy)
	{
		//super.listenerAdded(type, count, proxy);

		if ("change".equals(type)) {
			if (!isListeningForConnectivity) {
				manageConnectivityListener(true);
			}
		}
	}


	@Override
	public void listenerRemoved(String type, int count, TiProxy proxy) {
		//super.listenerRemoved(type, count, proxy);

		if ("change".equals(type) && count == 0) {
			manageConnectivityListener(false);
		}
	}

	public boolean getOnline()
	{
		boolean result = false;

		NetworkInfo ni = getConnectivityManager().getActiveNetworkInfo();

		if(ni != null && ni.isAvailable() && ni.isConnected()) {
			result = true;
		}
		return result;
	}

	protected int networkTypeToTitanium(boolean online, int androidType) {
		int type = NetworkModule.NETWORK_UNKNOWN;
		if (online) {
			switch(androidType) {
			case ConnectivityManager.TYPE_WIFI :
				type = NetworkModule.NETWORK_WIFI;
				break;
			case ConnectivityManager.TYPE_MOBILE :
				type = NetworkModule.NETWORK_MOBILE;
				break;
			default : type = NetworkModule.NETWORK_UNKNOWN;
			}
		} else {
			type = NetworkModule.NETWORK_NONE;
		}
		return type;
	}

	public int getNetworkType() {
		int type = NETWORK_UNKNOWN;

		// start event needs network type. So get it if we don't have it.
		if (connectivityManager == null) {
			connectivityManager = (ConnectivityManager)
				getTiContext().getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		}

		try {
			NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
			if(ni != null && ni.isAvailable() && ni.isConnected()) {
				type = networkTypeToTitanium(true, ni.getType());
			} else {
				type = NetworkModule.NETWORK_NONE;
			}
		} catch (SecurityException e) {
			Log.w(LCAT, "Permission has been removed. Cannot determine network type: " + e.getMessage());
		}
		return type;
	}

	public String getNetworkTypeName()
	{
		return networkTypeToTypeName(getNetworkType());
	}

	private String networkTypeToTypeName(int type)
	{
		switch(type)
		{
			case 0 : return "NONE";
			case 1 : return "WIFI";
			case 2 : return "MOBILE";
			case 3 : return "LAN";
			default : return "UNKNOWN";
		}
	}
	protected void manageConnectivityListener(boolean attach) {
		if (attach) {
			if (!isListeningForConnectivity) {
				if (hasListeners(EVENT_CONNECTIVITY)) {
					if (networkListener == null) {
						networkListener = new TiNetworkListener(messageHandler);
					}
					networkListener.attach(getTiContext().getActivity().getApplicationContext());
					isListeningForConnectivity = true;
					if (DBG) {
						Log.d(LCAT, "Resuming: adding connectivity listener");
					}
				}
			}
		} else {
			if (isListeningForConnectivity) {
				networkListener.detach();
				isListeningForConnectivity = false;
				if (DBG) {
					Log.d(LCAT, "Pausing: removing connectivity listener.");
				}
			}
		}
	}

	private ConnectivityManager getConnectivityManager() {
		return (ConnectivityManager) getTiContext().getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	@Override
	public void onResume() {
		super.onResume();
		connectivityManager = getConnectivityManager();
		manageConnectivityListener(true);
	}

	@Override
	public void onPause() {
		manageConnectivityListener(false);
		connectivityManager = null;
	}
}
