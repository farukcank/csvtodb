package com.eclipseuzmani.csvtodb.wizards;

import org.eclipse.jface.wizard.Wizard;

public class CsvToSqlActionWizard extends Wizard{
	private CsvToSqlActionWizardPage page = new CsvToSqlActionWizardPage();
	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addPages() {
		addPage(page);
	}

}
