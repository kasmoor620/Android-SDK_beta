package com.weemo.sdk.helper.fragment;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.weemo.sdk.helper.DemoAccounts;
import com.weemo.sdk.helper.R;

/**
 * This is a simple fragment that allows the user of the application to chose from a list
 * or to directly enter a value.
 * It is used  multiple times in this project
 * This is a simple util and does not contain Weemo SDK specific code
 */
public class ChooseFragment extends DialogFragment {

	/** Fragment required string argument key: the text of the validation button */
	private static final String ARG_BUTTONTEXT = "buttonText";

	/** Fragment optional string argument key: the account to remove from choice */
	private static final String ARG_REMOVEID = "removeID";

	/** The input */
	protected @Nullable EditText input;

	/** The validation button */
	protected @Nullable Button goBtn;

	/** Proposed accounts */
	protected Map<String, String> accounts = new LinkedHashMap<String, String>(DemoAccounts.ACCOUNTS);

	/** The string that should be hidden*/
	private String removeID;

	/**
	 * Factory (best practice for fragments)
	 *
	 * @param buttonText The text of the validation button
	 * @param removeID The account to remove from choice
	 * @return The created fragment
	 */
	public static ChooseFragment newInstance(final String buttonText, final @CheckForNull String removeID) {
		final ChooseFragment fragment = new ChooseFragment();
		final Bundle args = new Bundle();
		args.putString(ARG_BUTTONTEXT, buttonText);
		args.putString(ARG_REMOVEID, removeID);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Factory (best practice for fragments)
	 *
	 * @param buttonText The text of the validation button
	 * @return The created fragment
	 */
	public static ChooseFragment newInstance(final String buttonText) {
		return newInstance(buttonText, null);
	}

	/**
	 * Interface that activities using this fragment must implements
	 */
	public static interface ChooseListener {
		/**
		 * Used by this fragment to tell its activity that the user has chosen a user ID
		 *
		 * @param chose The chosen userID
		 */
		public void onChoose(String chose);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.removeID = getArguments().getString(ARG_REMOVEID);
		if (this.removeID != null) {
			this.accounts.remove(this.removeID);
		}
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		super.onCancel(dialog);
		getActivity().finish();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View root = inflater.inflate(R.layout.fragment_choose, container, false);

		this.input = (EditText) root.findViewById(R.id.input);
		if (TextUtils.isEmpty(this.removeID)) {
			this.input.setText(DemoAccounts.getDeviceName(true));
			this.input.selectAll();
		}

		this.goBtn = (Button) root.findViewById(R.id.go);

		this.goBtn.setText(getArguments().getString(ARG_BUTTONTEXT));

		final String[] ids = this.accounts.keySet().toArray(new String[0]);

		final ListView list = (ListView) root.findViewById(R.id.list);
		list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_2, ids) {
			@Override public View getView(final int position, View row, final ViewGroup parent) {
				if (row == null) {
					row = inflater.inflate(android.R.layout.simple_list_item_2, null);
				}

				final String itemId = getItem(position);

				((TextView) row.findViewById(android.R.id.text1)).setText(ChooseFragment.this.accounts.get(itemId));
				((TextView) row.findViewById(android.R.id.text2)).setText(itemId);

				return row;
			}
		});

		list.setOnItemClickListener(new OnItemClickListener() {
			@Override public void onItemClick(final AdapterView<?> adapter, final View view, final int position, final long itemId) {
				ChooseFragment.this.input.setText(ids[position]);
				ChooseFragment.this.input.setSelection(ids[position].length());
			}
		});

		this.goBtn.setOnClickListener(new OnClickListener() {
			@Override public void onClick(final View arg0) {
				final String txt = ChooseFragment.this.input.getText().toString();

				if (txt.isEmpty()) {
					return ;
				}

				final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(ChooseFragment.this.goBtn.getWindowToken(), 0);

				((ChooseListener) getActivity()).onChoose(txt);
			}
		});

		if (savedInstanceState != null) {
			boolean enabled = savedInstanceState.getBoolean("enabled", true);
			setEnabled(enabled);
		}

		return root;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("enabled", this.goBtn.isEnabled());
		super.onSaveInstanceState(outState);
	}

	/**
	 * Make this fragment usable or not
	 *
	 * @param enabled Whether or not to enable interaction with this fragment
	 */
	public void setEnabled(final boolean enabled) {
		this.goBtn.setEnabled(enabled);

		this.input.setEnabled(enabled);
		this.input.setFocusable(enabled);
		if (enabled) {
			this.input.setFocusableInTouchMode(enabled);
		}
	}

	/**
	 * Changes the text of the selection button
	 *
	 * @param text The new text of the button
	 */
	public void setButtonText(final String text) {
		this.goBtn.setText(text);
	}
}
