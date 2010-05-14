package net.sf.taverna.t2.commandline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.invocation.WorkflowDataToken;
import net.sf.taverna.t2.reference.ErrorDocument;
import net.sf.taverna.t2.reference.IdentifiedList;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.reference.T2ReferenceType;

import org.apache.log4j.Logger;

public class SaveResultsHandler {

	private final Map<String, Integer> portsAndDepth;
	private HashMap<String, Integer> depthSeen;
	private final File rootDirectory;
	private static Logger logger = Logger
			.getLogger(CommandLineResultListener.class);
	private final File outputDocumentFile;

	public SaveResultsHandler(Map<String, Integer> portsAndDepth,
			File rootDirectory, File outputDocumentFile) {

		this.portsAndDepth = portsAndDepth;
		this.rootDirectory = rootDirectory;
		this.outputDocumentFile = outputDocumentFile;

		depthSeen = new HashMap<String, Integer>();
		for (String portName : portsAndDepth.keySet()) {
			depthSeen.put(portName, -1);
		}
	}

	public void tokenReceived(WorkflowDataToken token, String portName) {
		if (rootDirectory != null) { //only save individual results if a directory is specified
			if (portsAndDepth.containsKey(portName)) {
				int[] index = token.getIndex();
				if (depthSeen.get(portName) == -1)
					depthSeen.put(portName, index.length);
				if (index.length >= depthSeen.get(portName)) {
					storeToken(token, portName);
				}
			} else {
				logger
						.error("Result recieved for unexpected Port: "
								+ portName);
			}
		}
	}
	
	public void saveOutputDocument(Map<String,WorkflowDataToken> allResults) {
		if (outputDocumentFile!=null) {
			
		}
	}

	protected void storeToken(WorkflowDataToken token, String portName) {

		if (token.getData().getReferenceType() == T2ReferenceType.IdentifiedList) {
			saveList(token, portName);
		} else {
			File dataDirectory = rootDirectory;
			File dataFile = null;

			if (token.getIndex().length > 0) {
				dataDirectory = new File(rootDirectory, portName);
				for (int i = 0; i < token.getIndex().length - 1; i++) {
					dataDirectory = new File(dataDirectory, String
							.valueOf(token.getIndex()[i]));
				}
				dataFile = new File(dataDirectory, String.valueOf(token
						.getIndex()[token.getIndex().length - 1]));
			} else {
				dataFile = new File(dataDirectory, portName);
			}
			if (!dataDirectory.exists()) {
				dataDirectory.mkdirs();
			}

			if (dataFile.exists()) {
				System.err.println("There is already data saved to: "
						+ dataFile.getAbsolutePath());
				System.exit(-1);
			}
			saveIndividualDataFile(token.getData(), dataFile, token
					.getContext());
		}
	}

	private void saveList(WorkflowDataToken token, String portName) {
		File dataDirectory = null;
		int[] index = token.getIndex();

		if (token.getIndex().length > 0) {
			dataDirectory = new File(rootDirectory, portName);
			for (int i = 0; i < index.length - 1; i++) {
				dataDirectory = new File(dataDirectory, String.valueOf(token
						.getIndex()[i]));
			}
			dataDirectory = new File(dataDirectory, String.valueOf(token
					.getIndex()[index.length - 1]));
		} else {
			dataDirectory = new File(rootDirectory, portName);
		}
		if (!dataDirectory.exists()) {
			dataDirectory.mkdirs();
		}

		T2Reference reference = token.getData();
		IdentifiedList<T2Reference> list = token.getContext()
				.getReferenceService().getListService().getList(reference);
		int c = 0;
		for (T2Reference id : list) {
			File dataFile = new File(dataDirectory, String.valueOf(c));
			saveIndividualDataFile(id, dataFile, token.getContext());
			c++;
		}

	}

	protected void saveIndividualDataFile(T2Reference reference, File dataFile,
			InvocationContext context) {

		if (dataFile.exists()) {
			System.err.println("There is already data saved to: "
					+ dataFile.getAbsolutePath());
			System.exit(-1);
		}

		Object data = null;
		if (reference.containsErrors()) {
			data = describeErrorDocument(context.getReferenceService()
					.getErrorDocumentService().getError(reference));
		} else {
			// FIXME: this really should be done using a stream rather
			// than an instance of the object in memory
			data = context.getReferenceService().renderIdentifier(reference,
					Object.class, context);
		}

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(dataFile);
			if (data instanceof byte[]) {
				fos.write((byte[]) data);
				fos.flush();
				fos.close();
			} else {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(fos));
				out.print(data.toString());
				out.flush();
				out.close();
			}
		} catch (FileNotFoundException e) {
			logger.error("Unable to find the file: '"
					+ dataFile.getAbsolutePath() + "' for writing results", e);
		} catch (IOException e) {
			logger.error("IO Error writing resuts to: '"
					+ dataFile.getAbsolutePath(), e);
		}
	}

	private Object describeErrorDocument(ErrorDocument error) {
		// FIXME: obviously need to do more than this!
		return error.getMessage();
	}
}