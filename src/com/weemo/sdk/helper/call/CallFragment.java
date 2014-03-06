package com.weemo.sdk.helper.call;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.weemo.sdk.Weemo;
import com.weemo.sdk.WeemoCall;
import com.weemo.sdk.WeemoCall.VideoSource;
import com.weemo.sdk.WeemoEngine;
import com.weemo.sdk.event.WeemoEventListener;
import com.weemo.sdk.event.call.ReceivingVideoChangedEvent;
import com.weemo.sdk.helper.R;
import com.weemo.sdk.view.WeemoVideoInFrame;
import com.weemo.sdk.view.WeemoVideoOutPreviewFrame;

/**
 * This is the fragment that controls a call and display it's video views.
 *
 * It uses the Weemo API to control everything that relates to the call, the video and the audio OUT.
 *
 * Weemo does not exposes api to control audio IN.
 * This fragment uses Android's AudioManager to control everything that relates to audio IN.
 */
@SuppressWarnings("deprecation")
public class CallFragment extends Fragment {

	/** Fragment required int argument key: for the call ID */
	private static final String ARG_CALLID = "callId";

	/** Fragment required boolean argument key: whether the orientation is locked */
	private static final String ARG_LOCKED = "locked";

	/** Fragment required int argument key: the video out correction */
	private static final String ARG_CORRECTION = "correction";

	/** The rotation property on the view to animate */
	private static final String PROP_ROTATION = "rotation";

	/** Button */
	private @Nullable ImageView toggleIn;
	/** Button */
	protected @Nullable ImageView muteOut;
	/** Button */
	protected @Nullable ImageView video;
	/** Button */
	protected @Nullable ImageView videoToggle;
	/** Button */
	private @Nullable ImageView hangup;

	/** The current call */
	protected @Nullable WeemoCall call;

	/**
	 * This is the correction for the OrientationEventListener.
	 * It allows portrait devices (like phones) and landscape devices (like tablets)
	 * to have the same orientation result.
	 */
	protected int correction;

	/** Whether or not the speakerphone is on. */
	protected boolean isSpeakerphoneOn;

	/** The audio manager used to control audio */
	protected @Nullable AudioManager audioManager;

	/** Used to rotate UI elements according to device orientation */
	private @CheckForNull OrientationEventListener oel;

	/** Used to receive Intent.ACTION_HEADSET_PLUG, which is when the headset is (un)plugged */
	private @Nullable BroadcastReceiver headsetPlugReceiver;

	/** Frame (declared in the XML) that will contain video OUT */
	protected @Nullable WeemoVideoOutPreviewFrame videoOutFrame;


	/**
	 * Factory (best practice for fragments)
	 *
	 * @param callId The ID of the call
	 * @param locked Whether this fragment is in a orientation locked window
	 * @param correction The correction (should be 90 for a small tablet, -1 otherwise)
	 * @return The fragment
	 */
	public static CallFragment newInstance(final int callId, final boolean locked, final int correction) {
		final CallFragment fragment = new CallFragment();
		final Bundle args = new Bundle();
		args.putInt(ARG_CALLID, callId);
		args.putBoolean(ARG_LOCKED, locked);
		args.putInt(ARG_CORRECTION, correction);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

		final WeemoEngine weemo = Weemo.instance();
		// This check should has been done by the activity
		final int callId = getArguments().getInt(ARG_CALLID);
		assert weemo != null;
		this.call = weemo.getCall(callId);

		// Register as event listener
		Weemo.eventBus().register(this);
	}

	/**
	 * Initializes call & audio related buttons
	 *
	 * @param root The root view containing the buttons
	 */
	private void initCallButtons(final View root) {
		// Button that toggles audio route
		this.toggleIn = (ImageView) root.findViewById(R.id.toggle_in);
		this.toggleIn.setOnClickListener(new OnClickListener() {
			@Override public void onClick(final View view) {
				setSpeakerphoneOn(!CallFragment.this.isSpeakerphoneOn);
			}
		});

		// Button that toggles audio OUT mute
		this.muteOut = (ImageView) root.findViewById(R.id.mute_out);
		this.muteOut.setOnClickListener(new OnClickListener() {
			private boolean mute; // = false;
			@Override public void onClick(final View view) {
				this.mute = !this.mute;
				if (this.mute) {
					CallFragment.this.call.audioMute();
					CallFragment.this.muteOut.setImageResource(R.drawable.weemo_mic_muted);
				}
				else {
					CallFragment.this.call.audioUnMute();
					CallFragment.this.muteOut.setImageResource(R.drawable.weemo_mic_on);
				}
			}
		});

		// Button that hangs up the call
		this.hangup = (ImageView) root.findViewById(R.id.hangup);
		this.hangup.setOnClickListener(new OnClickListener() {
			@Override public void onClick(final View view) {
				CallFragment.this.call.hangup();
			}
		});
	}

	/**
	 * Initializes video related buttons
	 *
	 * @param root The root view containing the buttons
	 */
	private void initVideoButtons(final View root) {
		// Button that toggles sending video
		// Note that we also toggle the videoOutFrame visibility
		this.video = (ImageView) root.findViewById(R.id.video);
		this.video.setOnClickListener(new OnClickListener() {
			boolean videoRunning = true;
			@Override public void onClick(final View view) {
				this.videoRunning = !this.videoRunning;
				if (this.videoRunning) {
					CallFragment.this.video.setImageResource(R.drawable.weemo_video_on);
					CallFragment.this.videoOutFrame.setVisibility(View.VISIBLE);
					CallFragment.this.call.videoStart();
					if (Camera.getNumberOfCameras() > 1) {
						CallFragment.this.videoToggle.setVisibility(View.VISIBLE);
					}
				}
				else {
					CallFragment.this.video.setImageResource(R.drawable.weemo_video_off);
					CallFragment.this.videoOutFrame.setVisibility(View.GONE);
					CallFragment.this.call.videoStop();
					CallFragment.this.videoToggle.setVisibility(View.GONE);
				}
			}
		});

		// Button that toggles sending video source
		this.videoToggle = (ImageView) root.findViewById(R.id.video_toggle);
		if (Camera.getNumberOfCameras() <= 1) {
			this.videoToggle.setVisibility(View.GONE);
		}
		this.videoToggle.setOnClickListener(new OnClickListener() {
			boolean front = true;
			@Override public void onClick(final View view) {
				this.front = !this.front;
				CallFragment.this.call.setVideoSource(this.front ? VideoSource.FRONT : VideoSource.BACK);
			}
		});
	}

	/**
	 * Computes the closest capped orientation, meaning 0, 90, 180 or 270
	 *
	 * @param orientation The real orientation
	 * @return The capped orientation
	 */
	protected static int getCappedOrientation(final int orientation) {
		if (orientation > 45 && orientation <= 135) { return 270; }
		else if (orientation > 135 && orientation <= 225) { return 180; }
		else if (orientation > 225 && orientation <= 315) { return 90; }
		else if (orientation > 315 || orientation <= 45) { return 0; }
		return 0;
	}

	/**
	 * Get the orienation in degrees according from the surface rotation.
	 *
	 * @param rotation The surface rotation, should be one of Surface.ROTATION_*
	 * @return The corresponding orientation
	 */
	protected static int getSurfaceOrientation(final int rotation) {
		switch (rotation) {
		case Surface.ROTATION_90:         return 90;
		case Surface.ROTATION_180:        return 180;
		case Surface.ROTATION_270: 	      return 270;
		case Surface.ROTATION_0: default: return 0;
		}
	}

	/**
	 * Orientation listener that will call {@link CallFragment#setOrientation(int)} only
	 * when the capped orientation has changed
	 */
	private class CallOrientationEventListener extends OrientationEventListener {

		/**
		 * Previous capped orientation.
		 * Used to call {@link CallFragment#setOrientation(int)} only when the capped orientation changes.
		 */
		private int lastOrientation = -1;

		/**
		 * Constructor
		 */
		public CallOrientationEventListener() {
			super(CallFragment.this.getActivity(), SensorManager.SENSOR_DELAY_NORMAL);
		}

		@Override public void onOrientationChanged(int orientation) {
			orientation = getCappedOrientation(orientation);
			orientation = (orientation + 360 - CallFragment.this.correction) % 360;
			if (this.lastOrientation != orientation) {
				setOrientation(orientation);
				this.lastOrientation = orientation;
			}
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View root = inflater.inflate(R.layout.weemo_fragment_call, container, false);

		final boolean locked = getArguments().getBoolean(ARG_LOCKED);

		// Get the OUT frame from the inflated view and set the call to use it
		this.videoOutFrame = (WeemoVideoOutPreviewFrame) root.findViewById(R.id.video_out);
		this.videoOutFrame.setUseDeviceOrientation(locked);
		int vOutCorrection = getArguments().getInt(ARG_CORRECTION);
		if (!locked && vOutCorrection > 0)
			this.videoOutFrame.setDeviceCorrection(vOutCorrection);
		this.call.setVideoOut(this.videoOutFrame);

		// Get the IN frame from the inflated view and set the call to use it
		// We set its display to follow device orientation because we have blocked the device rotation
		final WeemoVideoInFrame videoInFrame = (WeemoVideoInFrame) root.findViewById(R.id.video_in);
		videoInFrame.setDisplayFollowDeviceOrientation(locked);
		this.call.setVideoIn(videoInFrame);

		if (locked) {
			// This will call setOrientation each time the device orientation have changed
			// This allows us to display the control ui buttons in the correct orientation
			this.oel = new CallOrientationEventListener();
		}

		// Simple brodcast receiver that will call setSpeakerphoneOn when receiving an intent
		// It will be registered for Intent.ACTION_HEADSET_PLUG intents
		this.headsetPlugReceiver = new BroadcastReceiver() {
			@Override public void onReceive(final Context context, final Intent intent) {
				setSpeakerphoneOn(!CallFragment.this.audioManager.isWiredHeadsetOn());
			}
		};

		initCallButtons(root);
		initVideoButtons(root);

		// By default, we set the audio IN route according to isWiredHeadsetOn
		setSpeakerphoneOn(!this.audioManager.isWiredHeadsetOn());

		// Get the correction for the OrientationEventListener
		this.correction = getSurfaceOrientation(getActivity().getWindowManager().getDefaultDisplay().getRotation());

		// Sets the camera preview dimensions according to whether or not the remote contact has started his video
		setVideoOutFrameDimensions(this.call.isReceivingVideo());

		return root;
	}

	@Override
	public void onDestroy() {
		// Unregister as event listener
		Weemo.eventBus().unregister(this);

		super.onDestroy();
	}

	/**
	 * Uses AudioManager to route audio to Speakerphone or not
	 *
	 * @param isOn Whether to set speakers (true) or internals (false)
	 */
	protected void setSpeakerphoneOn(final boolean isOn) {
		this.audioManager.setSpeakerphoneOn(isOn);
		this.toggleIn.setImageResource(isOn ? R.drawable.weemo_volume_on : R.drawable.weemo_volume_muted);
		this.isSpeakerphoneOn = isOn;
	}

	/**
	 * Sets the dimension preview dimensions according to whether or not we are receiving video.
	 * If we are, we need to have a small preview.
	 * If we are not, the preview needs to fill the space.
	 *
	 * @param isReceivingVideo Whether or not we are receiving video
	 */
	private void setVideoOutFrameDimensions(final boolean isReceivingVideo) {
		final DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

		final LayoutParams params = this.videoOutFrame.getLayoutParams();
		if (isReceivingVideo) {
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32 * 4, metrics);
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32 * 4, metrics);
		}
		else {
			params.width = LayoutParams.MATCH_PARENT;
			params.height = LayoutParams.MATCH_PARENT;
		}

		this.videoOutFrame.setLayoutParams(params);
	}

	/**
	 * Animate the property of an object.
	 * may first add or remove 360 degrees to the property
	 * to ensure that the property will rotate in the right direction
	 *
	 * @param view The view to animate
	 * @param property The property of the view to animate
	 * @param current The current value of the property
	 * @param angle The target value of the property
	 */
	private static void animate(final View view, final String property, final float current, final int angle) {
		if (angle - current > 180) {
			ObjectAnimator.ofFloat(view, property, current + 360).setDuration(0).start();
		}
		else if (current - angle > 180) {
			ObjectAnimator.ofFloat(view, property, current - 360).setDuration(0).start();
		}

		ObjectAnimator.ofFloat(view, property, angle).start();
	}

	/**
	 * Sets orientation of all UI elements
	 * This is called by the OrientationEventListener
	 *
	 * @param orientation The new orientation to set
	 */
	protected void setOrientation(final int orientation) {
		animate(this.toggleIn,    PROP_ROTATION, this.toggleIn.getRotation(),    orientation);
		animate(this.muteOut,     PROP_ROTATION, this.muteOut.getRotation(),     orientation);
		animate(this.video,       PROP_ROTATION, this.video.getRotation(),       orientation);
		animate(this.videoToggle, PROP_ROTATION, this.videoToggle.getRotation(), orientation);
		animate(this.hangup,      PROP_ROTATION, this.hangup.getRotation(),      orientation);
	}

	@Override
	public void onStart() {
		super.onStart();

		// Start listening for orientation changes
		if (this.oel != null && this.oel.canDetectOrientation()) {
			this.oel.enable();
		}

		// Register the BrodcastReceiver to detect headset connection change
		final IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		filter.setPriority(0);
		getActivity().registerReceiver(this.headsetPlugReceiver, filter);
	}

	@Override
	public void onStop() {
		// We do not need to listen for orientation change while we are in the background
		// Beside, not stoping this will generate a leak when the fragment is destroyed
		if (this.oel != null) {
			this.oel.disable();
		}

		// Same as the line above
		getActivity().unregisterReceiver(this.headsetPlugReceiver);

		super.onStop();
	}

	/**
	 * This listener catches ReceivingVideoChangedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is ReceivingVideoChangedEvent
	 * 3. It's fragment object has been registered with Weemo.getEventBus().register(this) in onCreate()
	 *
	 * @param event The event
	 */
	@WeemoEventListener
	public void onReceivingVideoChanged(final ReceivingVideoChangedEvent event) {
		// First, we check that this event concerns the call we are monitoring
		if (event.getCall().getCallId() != this.call.getCallId()) {
			return ;
		}

		// Sets the camera preview dimensions according to whether or not the remote contact has started his video
		setVideoOutFrameDimensions(event.isReceivingVideo());
	}

}
