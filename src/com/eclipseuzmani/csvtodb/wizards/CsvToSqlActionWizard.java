package com.eclipseuzmani.csvtodb.wizards;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import au.com.bytecode.opencsv.CSVReader;

import com.eclipseuzmani.csvtodb.editors.CsvToSql;

public class CsvToSqlActionWizard extends Wizard {
	private CsvToSqlActionWizardPage page = new CsvToSqlActionWizardPage();
	private final CsvToSql csvToSql;

	private static class RowStrLookup extends StrLookup {
		private final Pattern patternCell = Pattern
				.compile("\\Acolumn(\\d)\\Z");
		private final Pattern patternCellSqlString = Pattern
				.compile("\\Acolumn(\\d).string\\Z");
		private String[] row;

		public String[] getRow() {
			return row;
		}

		public void setRow(String[] row) {
			this.row = row;
		}

		@Override
		public String lookup(String arg) {
			Matcher matcher = patternCell.matcher(arg);
			if (matcher.find()) {
				int i = Integer.parseInt(matcher.group(1)) - 1;
				if (i >= 0 && i < row.length) {
					return row[i];
				}
			}
			matcher = patternCellSqlString.matcher(arg);
			if (matcher.find()) {
				int i = Integer.parseInt(matcher.group(1)) - 1;
				if (i >= 0 && i < row.length) {
					return toString(row[i]);
				}
			}
			return null;
		}

		private String toString(String s) {
			return "'" + s.replace("'", "''") + "'";
		}
	}

	public CsvToSqlActionWizard(CsvToSql csvToSql) {
		this.csvToSql = csvToSql;
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		final File csvFile = new File(page.getCsvPath());
		final File sqlFile = new File(page.getSqlPath());
		final String csvEncoding = page.getCsvEncoding();
		final String sqlEncoding = page.getSqlEncoding();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(csvFile, sqlFile, csvEncoding, sqlEncoding,
							monitor);
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
			IDialogSettings dialogSettings = getDialogSettings();
			if (dialogSettings != null) {
				dialogSettings.put("csvPath", page.getCsvPath());
				dialogSettings.put("sqlPath", page.getSqlPath());
				dialogSettings.put("csvEncoding", page.getCsvEncoding());
				dialogSettings.put("sqlEncoding", page.getSqlEncoding());
			}
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}
		return true;
	}

	private void doFinish(File csvFile, File sqlFile, String csvEncoding,
			String sqlEncoding, IProgressMonitor monitor) throws IOException {
		Reader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(csvFile), csvEncoding));
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(sqlFile), sqlEncoding));
			try {
				CSVReader reader = new CSVReader(in);
				out.write(csvToSql.getPre());
				RowStrLookup lookup = new RowStrLookup();
				StrSubstitutor strSubst = new StrSubstitutor(lookup);
				while (true) {
					String[] row = reader.readNext();
					if (row == null)
						break;
					lookup.setRow(row);
					out.write(strSubst.replace(csvToSql.getDetail()));
				}
				out.write(csvToSql.getPost());
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}

	@Override
	public void setDialogSettings(IDialogSettings settings) {
		super.setDialogSettings(settings);
		page.setCsvPath(settings.get("csvPath"));
		page.setSqlPath(settings.get("sqlPath"));
		page.setSqlEncoding(settings.get("sqlEncoding"));
		page.setCsvEncoding(settings.get("csvEncoding"));
	}

	@Override
	public void addPages() {
		addPage(page);
	}

}
