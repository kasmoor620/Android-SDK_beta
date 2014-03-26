package com.weemo.sdk.helper.connect;

import org.acra.ACRA;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

import com.weemo.sdk.Weemo;
import com.weemo.sdk.WeemoEngine;
import com.weemo.sdk.event.WeemoEventListener;
import com.weemo.sdk.event.global.AuthenticatedEvent;
import com.weemo.sdk.event.global.ConnectedEvent;
import com.weemo.sdk.helper.R;
import com.weemo.sdk.helper.contacts.ContactsActivity;
import com.weemo.sdk.helper.fragment.ChooseFragment;
import com.weemo.sdk.helper.fragment.ChooseFragment.ChooseListener;
import com.weemo.sdk.helper.fragment.ErrorFragment;
import com.weemo.sdk.helper.fragment.InputFragment;
import com.weemo.sdk.helper.fragment.InputFragment.InputListener;
import com.weemo.sdk.helper.fragment.LoadingDialogFragment;
import com.weemo.sdk.helper.util.ReportException;
import com.weemo.sdk.helper.util.UIUtils;

/**
 * This is the first activity being launched.
 * Its role is to handle connection and authentication of the user.
 */
public class ConnectActivity extends Activity implements InputListener, ChooseListener {

	/** The tag used to retrive all dialogs that this activity will launch */
	private static final String TAG_DIALOG = "dialog";

	/** Whether or not the current user has successfully logged in */
	private boolean hasLoggedIn; // = false;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		UIUtils.forceOverflowMenu(this);

		// Checks if Weemo is already initialized and authenticated.
		// If it is, it is probably because the user clicked on the service notification.
		// In which case, the user is redirected to the second screen
		final WeemoEngine weemo = Weemo.instance();
		if (weemo != null && weemo.isAuthenticated()) {
			this.hasLoggedIn = true;
			startActivity(
				new Intent(this, ContactsActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
				);
			finish();
			return ;
		}

		// This activity starts with a LoadingFragment (which makes the user wait while Weemo is connecting)
		// The connection is started in onStart, after registering the listener
		// (so that the ConnectedEvent can't be launched while we are not yet listening).
		if (savedInstanceState == null) {
			InputFragment.newInstance(getString(R.string.weemo_appId)).show(getFragmentManager(), TAG_DIALOG);
		}

		// Register the activity as event listener
		Weemo.eventBus().register(this);

		// Initialize Weemo, can be called multiple times
	}

	@Override
	protected void onDestroy() {
		// Unregister as event listener
		Weemo.eventBus().unregister(this);

		// If this activity is destroyed with hasLoggedIn is true,
		// this means it is destroyed after CallActivity being displayed.
		// However, if hasLoggedIn is false, it means that the activity is destroyed
		// because the user has got out of the application without logging in.
		// In this case we need to stop the Weemo engine.
		if (!this.hasLoggedIn) {
			Weemo.disconnect();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(R.string.core_dump)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override public boolean onMenuItemClick(final MenuItem item) {
					ACRA.getErrorReporter().handleSilentException(new ReportException());
					return true;
				}
			})
		;

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * This is called by the InputFragment when user clicks on "OK" button
	 * from the dialog that asks him for the appId
	 *
	 * @param appId The Application Identifier entered by the user
	 */
	@Override
	public void onInput(final String appId) {
		if (appId.isEmpty() || appId.contains(" ")) {
			return ;
		}

		final DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(TAG_DIALOG);
		if (dialog != null) {
			dialog.dismiss();
		}

		final LoadingDialogFragment loadingFragment = LoadingDialogFragment.newFragmentInstance(getString(R.string.connection_title), getString(R.string.connection_text), null);
		loadingFragment.setCancelable(false);
		loadingFragment.show(getFragmentManager(), TAG_DIALOG);

		Weemo.initialize(appId, this);
	}

	/**
	 * This is called by the ChoseFragment when user clicks on "login" button after selecting a login.
	 *
	 * @param userId The entered User Identifier
	 */
	@Override
	public void onChoose(final String userId) {
		final DialogFragment chooseDialog = (DialogFragment) getFragmentManager().findFragmentByTag(TAG_DIALOG);
		if (chooseDialog != null) {
			chooseDialog.dismiss();
		}

		final WeemoEngine weemo = Weemo.instance();
		if (weemo == null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, ErrorFragment.newInstance("Connection lost")).commit();
			return ;
		}

		final LoadingDialogFragment loadingDialog = LoadingDialogFragment.newFragmentInstance(userId, getString(R.string.authentication_title), null);
		loadingDialog.setCancelable(false);
		loadingDialog.show(getFragmentManager(), TAG_DIALOG);

		// Start authentication with the userId chosen by the user.
		ContactsActivity.currentUid = userId;
		weemo.authenticate(userId, WeemoEngine.UserType.INTERNAL);
	}

	/**
	 * This listener catches ConnectedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is ConnectedEvent
	 * 3. It's activity object has been registered with Weemo.getEventBus().register(this) in onStart()
	 *
	 * @param event The event
	 */
	@WeemoEventListener
	public void onConnected(final ConnectedEvent event) {
		final ConnectedEvent.Error error = event.getError();

		// Stop the loading dialog
		final DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(TAG_DIALOG);
		if (dialog != null) {
			dialog.dismiss();
		}

		// If there is an error, this means that connection failed
		// So we display the English description of the error
		if (error != null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, ErrorFragment.newInstance(error.description())).commit();
			return ;
		}

		// If there is no error, everything went normal, connection succeeded.

		final ChooseFragment chooseFragment = ChooseFragment.newInstance(getString(R.string.log_in));
		if (getResources().getBoolean(R.bool.isTablet)) {
			chooseFragment.show(getFragmentManager(), TAG_DIALOG);
		}
		else {
			getFragmentManager().beginTransaction().replace(android.R.id.content, ChooseFragment.newInstance(getString(R.string.log_in))).commit();
		}
	}

	/**
	 * This listener catches AuthenticatedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is AuthenticatedEvent
	 * 3. It's activity object has been registered with Weemo.getEventBus().register(this) in onStart()
	 *
	 * @param event The event
	 */
	@WeemoEventListener
	public void onAuthenticated(final AuthenticatedEvent event) {
		final AuthenticatedEvent.Error error = event.getError();

		// If there is an error, this means that authentication failed
		// So we display the English description of the error
		// We then go back to the login fragment so that authentication can be tried again
		if (error != null) {
			final DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(TAG_DIALOG);
			if (dialog != null) {
				dialog.dismiss();
			}

			if (error == AuthenticatedEvent.Error.BAD_APIKEY) {
				getFragmentManager().beginTransaction().replace(android.R.id.content, ErrorFragment.newInstance(error.description())).commit();
				return ;
			}

			Toast.makeText(this, error.description(), Toast.LENGTH_LONG).show();

			return ;
		}

		// If there is no error, everything went normal, go to call activity
		this.hasLoggedIn = true;
		startActivity(new Intent(this, ContactsActivity.class));
		finish();
	}
}
