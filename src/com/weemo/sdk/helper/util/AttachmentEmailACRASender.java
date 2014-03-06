package com.weemo.sdk.helper.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.acra.ACRA;
import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

/**
 * This is used by ACRA to generate a (crash) report
 */
public class AttachmentEmailACRASender implements ReportSender {

	/** The application context */
	private final Application ctx;

	/**
	 * Constructor
	 *
	 * @param ctx The application context
	 */
	public AttachmentEmailACRASender(final Application ctx) {
		this.ctx = ctx;
	}

	@Override
	public void send(final CrashReportData errorContent) throws ReportSenderException {
		final int labelRes = this.ctx.getApplicationInfo().labelRes;
		final String appName = labelRes == 0 ? "(Android app)" : this.ctx.getString(labelRes);
		String versionName = "";
		int versionCode = -1;
		try {
			final PackageInfo pkgInfo = this.ctx.getPackageManager().getPackageInfo(this.ctx.getPackageName(), 0);
			versionName = pkgInfo.versionName;
			versionCode = pkgInfo.versionCode;
		}
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { ACRA.getConfig().mailTo() });
		emailIntent.putExtra(
				Intent.EXTRA_SUBJECT, appName
			+	" " + versionName + " (" + versionCode + ")"
			+	" Crash: " + android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE
		);
		emailIntent.putExtra(Intent.EXTRA_TEXT,
				"Please provide details about the issue (what happened and how it happened) to assist outr team resolve your issue:\n"
		);

		try {
			final File outputFile = new File(this.ctx.getExternalCacheDir(), "crash_" + errorContent.get(ReportField.REPORT_ID) + ".log");
			final FileOutputStream outputStream = new FileOutputStream(outputFile);
			try {
				outputStream.write(buildBody(errorContent).getBytes("UTF-8"));
				outputStream.flush();
			}
			finally {
				outputStream.close();
			}
			final Uri uri = Uri.fromFile(outputFile);
			emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
		}
		catch (IOException e) {
			throw new ReportSenderException(e.getMessage(), e);
		}

		this.ctx.startActivity(emailIntent);
	}

	/**
	 * Build the body of the mail containing the crash report
	 *
	 * @param errorContent The crash report data
	 * @return The body of the mail
	 */
	private static String buildBody(final CrashReportData errorContent) {
		ReportField[] fields = ACRA.getConfig().customReportContent();
		if (fields.length == 0) {
			fields = ACRAConstants.DEFAULT_MAIL_REPORT_FIELDS;
		}

		final StringBuilder builder = new StringBuilder();
		for (final ReportField field : fields) {
			builder.append(field.toString()).append(":\n");
			builder.append(errorContent.get(field));
			builder.append("\n\n");
		}
		return builder.toString();
	}

}
