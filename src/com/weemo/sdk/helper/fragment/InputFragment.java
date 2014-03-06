package com.weemo.sdk.helper.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.weemo.sdk.helper.R;

/**
 * This is a very simple fragment that displays a message and an input
 */
public class InputFragment extends DialogFragment {

	/** Fragment required string argument key: the text of the dialog */
	private static final String ARG_TEXT = "text";

	/**
	 * Factory (best practice for fragments)
	 *
	 * @param text The text of the dialog
	 * @return The created fragment
	 */
	public static InputFragment newInstance(final String text) {
		final InputFragment fragment = new InputFragment();
		final Bundle args = new Bundle();
		args.putString(ARG_TEXT, text);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Interface that activities using this fragment must implements
	 */
	public static interface InputListener {
		/**
		 * Used by this fragment to tell its activity that the user has entered and validated an input
		 *
		 * @param text The entered text
		 */
		public void onInput(String text);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.weemo_appid);
		final View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_input, null);
		final EditText input = (EditText) root.findViewById(R.id.input);
		input.setText(getArguments().getString(ARG_TEXT));
		input.selectAll();
		builder.setView(root);

		builder.setPositiveButton(android.R.string.ok, null);

		final AlertDialog dialog = builder.create();

		// We use this instead of setPositiveButtonlistener to be able to prevent dismissal
		dialog.setOnShowListener(new OnShowListener() {
			@Override public void onShow(final DialogInterface dialogInterface) {
				final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
				button.setOnClickListener(new OnClickListener() {
					@Override public void onClick(final View view) {
						final String txt = input.getText().toString();
						((InputListener) getActivity()).onInput(txt);
					}
				});
			}
		});

//		setCancelable(false);
//		dialog.setCancelable(false);

		dialog.setCanceledOnTouchOutside(false);

		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		return dialog;
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		super.onCancel(dialog);
		getActivity().onBackPressed();
	}
}
