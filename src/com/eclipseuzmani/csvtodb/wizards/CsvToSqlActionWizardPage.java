package com.eclipseuzmani.csvtodb.wizards;

import java.io.File;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class CsvToSqlActionWizardPage extends WizardPage {
	private Text csvText;
	private Text sqlText;

	protected CsvToSqlActionWizardPage() {
		super("wizardPage");
		setTitle("Csv to Sql");
		setDescription("This wizard creates a sql file from an csv input.");
	}

	@Override
	public void createControl(Composite parent) {
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
		csvText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse(csvText, SWT.OPEN);
			}
		});
		label = new Label(container, SWT.NULL);
		label.setText("&Sql file:");

		sqlText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		sqlText.setLayoutData(gd);
		sqlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse(sqlText, SWT.SAVE);
			}
		});
		initialize();
		dialogChanged();
		setControl(container);
	}

	private void initialize() {
	}

	private void dialogChanged() {
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
		if (sqlFile.exists()){
			if (sqlFile.isDirectory()) {
				updateStatus("Sql file must not be a directory");
				return;
			}
			if (!sqlFile.canWrite()){
				updateStatus("Sql file be must be writable");
				return;
			}
		}else{
			if (sqlFile.getParentFile()==null){
				updateStatus("Directory the sql file belongs must be exist");
				return;
			}
			if (!sqlFile.getParentFile().exists()){
				updateStatus("Directory the sql file belongs must be exist");
				return;
			}
			if (!sqlFile.getParentFile().canWrite()){
				updateStatus("Directory the sql file belongs must be writable");
				return;
			}
		}
		updateStatus(null);
	}

	private void handleBrowse(Text text,int flags) {
		FileDialog dialog = new FileDialog(getShell(), flags);
		String result = dialog.open();
		if (result!=null){
			text.setText(result);
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getCsvPath() {
		return csvText.getText();
	}

	public String getSqlPath() {
		return sqlText.getText();
	}
}
