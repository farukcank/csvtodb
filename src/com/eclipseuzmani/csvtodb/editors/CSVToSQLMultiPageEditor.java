package com.eclipseuzmani.csvtodb.editors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

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

	private static class CsvToSql {
		private String pre;
		private String post;
		private String detail;

		public String getPre() {
			return pre;
		}

		public void setPre(String pre) {
			this.pre = pre;
		}

		public String getPost() {
			return post;
		}

		public void setPost(String post) {
			this.post = post;
		}

		public String getDetail() {
			return detail;
		}

		public void setDetail(String detail) {
			this.detail = detail;
		}

		public CsvToSql(String pre, String detail, String post) {
			this.pre = pre;
			this.detail = detail;
			this.post = post;
		}

		public CsvToSql() {
		}
	}

	private static class CsvToSqlHandler extends DefaultHandler {
		public final CsvToSql csvToSql = new CsvToSql();
		private StringBuilder sb = new StringBuilder();

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			sb.delete(0, sb.length());
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			sb.append(ch, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (localName.equals("pre")) {
				csvToSql.setPre(sb.toString());
			} else if (localName.equals("detail")) {
				csvToSql.setDetail(sb.toString());
			} else if (localName.equals("post")) {
				csvToSql.setPost(sb.toString());
			}
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}

	}

	private static final String SCHEMA_URI = "http://www.eclipseuzmani.com/csvtosql";
	private Text pre;
	private Text detail;
	private Text post;
	private CsvToSql csvToSql;
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

		pre.setText(csvToSql.pre);
		detail.setText(csvToSql.detail);
		post.setText(csvToSql.post);

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
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		IStorageEditorInput storageEditorInput = (IStorageEditorInput) getEditorInput()
				.getAdapter(IStorageEditorInput.class);
		try {
			this.csvToSql = loadInternal(storageEditorInput.getStorage()
					.getContents());
			if (pre != null)
				pre.setText(csvToSql.pre);
			if (detail != null)
				detail.setText(csvToSql.detail);
			if (post != null)
				post.setText(csvToSql.post);
			setDirty(false);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			final CsvToSql csvToSql = new CsvToSql(this.pre.getText(),
					this.detail.getText(), this.post.getText());
			IStorageEditorInput editorInput = (IStorageEditorInput) getEditorInput()
					.getAdapter(IStorageEditorInput.class);
			IFile file = (IFile) editorInput.getStorage();
			PipedInputStream pis = new PipedInputStream();
			final PipedOutputStream pos = new PipedOutputStream(pis);
			Callable<Void> c = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						internalSave(pos, csvToSql);
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

	private CsvToSql loadInternal(InputStream in) throws SAXException,
			IOException {
		String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
		SchemaFactory factory = SchemaFactory.newInstance(language);
		Schema schema = factory.newSchema(CSVToSQLMultiPageEditor.class
				.getResource("CsvToSql.xsd"));
		XMLReader xr = XMLReaderFactory.createXMLReader();
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		CsvToSqlHandler csvToSqlHandler = new CsvToSqlHandler();
		validatorHandler.setContentHandler(csvToSqlHandler);
		xr.setContentHandler(validatorHandler);
		xr.setErrorHandler(csvToSqlHandler);
		xr.parse(new InputSource(in));
		return csvToSqlHandler.csvToSql;
	}

	private void internalSave(OutputStream out, CsvToSql csvToSql)
			throws TransformerFactoryConfigurationError,
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
		hd.characters(csvToSql.getPre().toCharArray(), 0, csvToSql.getPre()
				.length());
		hd.endElement(SCHEMA_URI, "", "pre");

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "detail", atts);
		hd.characters(csvToSql.getDetail().toCharArray(), 0, csvToSql
				.getDetail().length());
		hd.endElement(SCHEMA_URI, "", "detail");

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "post", atts);
		hd.characters(csvToSql.getPost().toCharArray(), 0, csvToSql.getPost()
				.length());
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
