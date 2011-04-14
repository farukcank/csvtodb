package com.eclipseuzmani.csvtodb.wizards;

import java.io.File;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.WorkbenchEncoding;

public class CsvToSqlActionWizardPage extends WizardPage {
	private Text csvText;
	private Text sqlText;
	private Combo csvEncodingCombo;
	private Combo sqlEncodingCombo;
	private String csvPath;
	private String sqlPath;
	private String sqlEncoding;
	private String csvEncoding;

	protected CsvToSqlActionWizardPage() {
		super("wizardPage");
		setTitle("Csv to Sql");
		setDescription("This wizard creates a sql file from an csv input.");
	}

	@Override
	public void createControl(Composite parent) {
		List<String> definedEncodings = WorkbenchEncoding.getDefinedEncodings();
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Csv file:");

		csvText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		csvText.setLayoutData(gd);

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse(csvText, SWT.OPEN);
			}
		});
		
		label = new Label(container, SWT.NULL);
		label.setText("Csv encoding:");
		csvEncodingCombo = new Combo(container, SWT.DROP_DOWN);
		for(String enc : definedEncodings){
			csvEncodingCombo.add(enc);
		}
		GridDataFactory.defaultsFor(csvEncodingCombo).grab(true, false).applyTo(csvEncodingCombo);
		new Label(container, SWT.NULL);
		
		
		label = new Label(container, SWT.NULL);
		label.setText("&Sql file:");

		sqlText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		sqlText.setLayoutData(gd);
		button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse(sqlText, SWT.SAVE);
			}
		});
		label = new Label(container, SWT.NULL);
		label.setText("Sql encoding:");
		sqlEncodingCombo = new Combo(container, SWT.DROP_DOWN);
		for(String enc : definedEncodings){
			sqlEncodingCombo.add(enc);
		}
		GridDataFactory.defaultsFor(sqlEncodingCombo).grab(true, false).applyTo(sqlEncodingCombo);
		new Label(container, SWT.NULL);
		
		initialize();
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		csvText.addModifyListener(modifyListener);
		sqlText.addModifyListener(modifyListener);
		csvEncodingCombo.addModifyListener(modifyListener);
		sqlEncodingCombo.addModifyListener(modifyListener);
		dialogChanged();
		setControl(container);
	}

	private void initialize() {
		csvText.setText(csvPath == null ? "" : csvPath);
		sqlText.setText(sqlPath == null ? "" : sqlPath);
		csvEncodingCombo.setText(csvEncoding==null?WorkbenchEncoding.getWorkbenchDefaultEncoding():csvEncoding);
		sqlEncodingCombo.setText(sqlEncoding==null?WorkbenchEncoding.getWorkbenchDefaultEncoding():sqlEncoding);
	}

	private void dialogChanged() {
		csvPath = csvText.getText();
		sqlPath = sqlText.getText();
		csvEncoding = csvEncodingCombo.getText();
		sqlEncoding = sqlEncodingCombo.getText();
		if (getCsvPath().length() == 0) {
			updateStatus("A csv file must be specified");
			return;
		}
		if (getSqlPath().length() == 0) {
			updateStatus("A sql file must be specified");
			return;
		}

		File csvFile = new File(getCsvPath());
		File sqlFile = new File(getSqlPath());

		if (!csvFile.exists() || !csvFile.isFile()) {
			updateStatus("Csv file be must exist");
			return;
		}
		if (!csvFile.canRead()) {
			updateStatus("Csv file be must be readable");
			return;
		}
		if (sqlFile.exists()) {
			if (sqlFile.isDirectory()) {
				updateStatus("Sql file must not be a directory");
				return;
			}
			if (!sqlFile.canWrite()) {
				updateStatus("Sql file be must be writable");
				return;
			}
		} else {
			if (sqlFile.getParentFile() == null) {
				updateStatus("Directory the sql file belongs must be exist");
				return;
			}
			if (!sqlFile.getParentFile().exists()) {
				updateStatus("Directory the sql file belongs must be exist");
				return;
			}
			if (!sqlFile.getParentFile().canWrite()) {
				updateStatus("Directory the sql file belongs must be writable");
				return;
			}
		}
		updateStatus(null);
	}

	private void handleBrowse(Text text, int flags) {
		FileDialog dialog = new FileDialog(getShell(), flags);
		String result = dialog.open();
		if (result != null) {
			text.setText(result);
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getSqlEncoding() {
		return sqlEncoding;
	}

	public void setSqlEncoding(String sqlEncoding) {
		this.sqlEncoding = sqlEncoding;
	}

	public String getCsvEncoding() {
		return csvEncoding;
	}

	public void setCsvEncoding(String csvEncoding) {
		this.csvEncoding = csvEncoding;
	}

	public void setCsvPath(String csvPath) {
		this.csvPath = csvPath;
	}

	public void setSqlPath(String sqlPath) {
		this.sqlPath = sqlPath;
	}

	public String getCsvPath() {
		return csvPath;
	}

	public String getSqlPath() {
		return sqlPath;
	}
}
