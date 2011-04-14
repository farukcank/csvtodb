package com.eclipseuzmani.csvtodb.editors;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.ide.IDE;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * An example showing how to create a multi-page editor. This example has 3
 * pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
public class CSVToSQLMultiPageEditor extends EditorPart {
	private static final ExecutorService executorService = Executors
			.newCachedThreadPool();
	private static final String SCHEMA_URI = "http://www.eclipseuzmani.com/csvtosql";
	private Text pre;
	private Text detail;
	private Text post;
	private boolean dirty;
	private final ModifyListener modifyListener = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent e) {
			setDirty(true);
		}
	};
	private final IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {

		@Override
		public void resourceChanged(final IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						IWorkbenchPage[] pages = getSite().getWorkbenchWindow()
								.getPages();
						for (int i = 0; i < pages.length; i++) {
							if (((FileEditorInput) getEditorInput()).getFile()
									.getProject().equals(event.getResource())) {
								IEditorPart editorPart = pages[i]
										.findEditor(getEditorInput());
								pages[i].closeEditor(editorPart, true);
							}
						}
					}
				});
			}
		}
	};

	public CSVToSQLMultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener);
	}

	@Override
	public void setFocus() {
		detail.setFocus();
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		layout.numColumns = 1;

		Label preLabel = new Label(composite, SWT.NONE);
		preLabel.setText("Pre detail section");
		GridDataFactory.defaultsFor(preLabel).grab(true, false)
				.applyTo(preLabel);
		pre = new Text(composite, SWT.MULTI | SWT.BORDER);
		GridDataFactory.defaultsFor(pre).grab(true, true).applyTo(pre);

		Label detailLabel = new Label(composite, SWT.NONE);
		detailLabel.setText("Detail section");
		GridDataFactory.defaultsFor(detailLabel).grab(true, false)
				.applyTo(detailLabel);
		detail = new Text(composite, SWT.MULTI | SWT.BORDER);
		GridDataFactory.defaultsFor(detail).grab(true, true).applyTo(detail);

		Label postLabel = new Label(composite, SWT.NONE);
		postLabel.setText("Post detail section");
		GridDataFactory.defaultsFor(postLabel).grab(true, false)
				.applyTo(postLabel);
		post = new Text(composite, SWT.MULTI | SWT.BORDER);
		GridDataFactory.defaultsFor(post).grab(true, true).applyTo(post);

		pre.addModifyListener(modifyListener);
		detail.addModifyListener(modifyListener);
		post.addModifyListener(modifyListener);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	private void setDirty(boolean dirty) {
		if (this.dirty != dirty) {
			this.dirty = dirty;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			final String pre = this.pre.getText();
			final String detail = this.detail.getText();
			final String post = this.post.getText();
			IStorageEditorInput editorInput = (IStorageEditorInput) getEditorInput()
					.getAdapter(IStorageEditorInput.class);
			IFile file = (IFile) editorInput.getStorage();
			PipedInputStream pis = new PipedInputStream();
			final PipedOutputStream pos = new PipedOutputStream(pis);
			Callable<Void> c = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						internalSave(pos, pre, detail, post);
						return null;
					} finally {
						pos.close();
					}
				}
			};
			Future<Void> future = executorService.submit(c);
			file.setContents(pis, true, true, monitor);
			future.get();
			setDirty(false);
		} catch (TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void internalSave(OutputStream out, String pre, String detail,
			String post) throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, SAXException {
		StreamResult streamResult = new StreamResult(out);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
				.newInstance();
		// SAX2.0 ContentHandler.
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		hd.setResult(streamResult);
		hd.startDocument();
		AttributesImpl atts = new AttributesImpl();
		atts.clear();
		hd.startElement(SCHEMA_URI, "", "csvtosql", atts);

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "pre", atts);
		hd.characters(pre.toCharArray(), 0, pre.length());
		hd.endElement(SCHEMA_URI, "", "pre");

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "detail", atts);
		hd.characters(detail.toCharArray(), 0, detail.length());
		hd.endElement(SCHEMA_URI, "", "detail");

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "post", atts);
		hd.characters(post.toCharArray(), 0, post.length());
		hd.endElement(SCHEMA_URI, "", "post");

		hd.endElement(SCHEMA_URI, "", "csvtosql");
		hd.endDocument();
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
		super.dispose();
	}
}
