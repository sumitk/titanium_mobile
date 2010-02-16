package ti.modules.titanium.ui;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.view.ITiWindowHandler;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class TiTabActivity extends ActivityGroup
	implements ITiWindowHandler
{
	private static final String LCAT = "TiTabActivity";
	private static final boolean DBG = TiConfig.LOGD;

	protected TiCompositeLayout layout;
	protected TabGroupProxy proxy;
	protected Handler handler;

	public TiTabActivity() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		handler = new Handler();

		layout = new TiCompositeLayout(this);

        Intent intent = getIntent();

        boolean fullscreen = false;
        boolean navbar = false;
        Messenger messenger = null;
        Integer messageId = null;

        if (intent != null) {
        	if (intent.hasExtra("fullscreen")) {
        		fullscreen = intent.getBooleanExtra("fullscreen", fullscreen);
        	}
        	if (intent.hasExtra("navBarHidden")) {
        		navbar = intent.getBooleanExtra("navBarHidden", navbar);
        	}
        	if (intent.hasExtra("messenger")) {
        		messenger = (Messenger) intent.getParcelableExtra("messenger");
        		messageId = intent.getIntExtra("messageId", -1);
        	}
        }

        if (fullscreen) {
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (navbar) {
        	this.requestWindowFeature(Window.FEATURE_LEFT_ICON); // TODO Keep?
	        this.requestWindowFeature(Window.FEATURE_RIGHT_ICON);
	        this.requestWindowFeature(Window.FEATURE_PROGRESS);
	        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } else {
           	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(layout);

        //Notify caller that onCreate is done. Use post
        // to prevent deadlock.
        final TiTabActivity me = this;
        final Messenger fMessenger = messenger;
        final int fMessageId = messageId;
        handler.post(new Runnable(){
			@Override
			public void run() {
		        if (fMessenger != null) {
		        	try {
			        	Message msg = Message.obtain();
			        	msg.what = fMessageId;
			        	msg.obj = me;
			        	fMessenger.send(msg);
			        	Log.w(LCAT, "Notifying TiTabGroup, activity is created");
		        	} catch (RemoteException e) {
		        		Log.e(LCAT, "Unable to message creator. finishing.");
		        		me.finish();
		        	} catch (RuntimeException e) {
		        		Log.w(LCAT, "Run-time exception: " + e.getMessage(), e);
		        	}
		        }
			}
		});
	}

    public TiApplication getTiApp() {
    	return (TiApplication) getApplication();
    }

    public TiCompositeLayout getLayout() {
    	return layout;
    }


	@Override
	public void addWindow(View v, LayoutParams params) {
		layout.addView(v, params);
	}

	@Override
	public void removeWindow(View v) {
		layout.removeView(v);
	}

	@Override
	public void finish()
	{
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.getBooleanExtra("finishRoot", false)) {
				((TiApplication) getApplication()).getRootActivity().finish();
			}
		}
		super.finish();
	}

	@Override
	protected void onPause() {
		super.onPause();

		((TiApplication) getApplication()).setWindowHandler(null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		((TiApplication) getApplication()).setWindowHandler(this);
	}

}