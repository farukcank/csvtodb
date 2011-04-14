package com.eclipseuzmani.csvtodb.editors;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import com.eclipseuzmani.csvtodb.Activator;
import com.eclipseuzmani.csvtodb.wizards.CsvToSqlActionWizard;

/**
 * Manages the installation/deinstallation of global actions for multi-page
 * editors. Responsible for the redirection of global actions to the active
 * editor. Multi-page contributor replaces the contributors for the individual
 * editors in the multi-page editor.
 */
public class CSVToSQLMultiPageEditorContributor extends
		EditorActionBarContributor {
	private CSVToSQLMultiPageEditor activeEditorPart;
	private Action sampleAction;

	/**
	 * Creates a multi-page contributor.
	 */
	public CSVToSQLMultiPageEditorContributor() {
		super();
		createActions();
	}

	/*
	 * (non-JavaDoc) Method declared in
	 * AbstractMultiPageEditorActionBarContributor.
	 */

	public void setActiveEditor(IEditorPart targetEditor) {
		if (activeEditorPart == targetEditor)
			return;
		if (!(targetEditor instanceof CSVToSQLMultiPageEditor)) {
			activeEditorPart = null;
			return;
		}

		activeEditorPart = (CSVToSQLMultiPageEditor) targetEditor;
	}

	private void createActions() {
		sampleAction = new Action() {
			public void run() {
				CsvToSqlActionWizard wizard = new CsvToSqlActionWizard(activeEditorPart.getCsvToSql());
				wizard.setDialogSettings(Activator.getDefault().getDialogSettings());
				WizardDialog dialog = new WizardDialog(activeEditorPart.getSite().getShell(), wizard);
				dialog.create();
				dialog.open();
				// CsvToSql csvToSql = activeEditorPart.getCsvToSql();
				// System.out.println("Executed : "+csvToSql);
			}
		};
		sampleAction.setText("Generate Sql");
		sampleAction.setToolTipText("Generate sql for given csv input");
		sampleAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
	}

	public void contributeToMenu(IMenuManager manager) {
		IMenuManager menu = new MenuManager("Editor &Menu");
		manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
		menu.add(sampleAction);
	}

	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(sampleAction);
	}
}
