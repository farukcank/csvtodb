package com.eclipseuzmani.csvtodb.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.eclipseuzmani.csvtodb.editors.CsvToSql;

public class CsvToSqlActionWizard extends Wizard{
	private CsvToSqlActionWizardPage page = new CsvToSqlActionWizardPage();
	private final CsvToSql csvToSql;
	public CsvToSqlActionWizard(CsvToSql csvToSql){
		this.csvToSql = csvToSql;
		setNeedsProgressMonitor(true);
	}
	@Override
	public boolean performFinish() {
		final File csvFile = new File(page.getCsvPath());
		final File sqlFile = new File(page.getSqlPath());
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(csvFile, sqlFile, monitor);
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	private void doFinish(File csvFile, File sqlFile, IProgressMonitor monitor) throws IOException {
		FileOutputStream fos = new FileOutputStream(sqlFile);
		try{
			fos.write(csvToSql.getPre().getBytes());
		}finally{
			fos.close();
		}
	}
	@Override
	public void addPages() {
		addPage(page);
	}

}
