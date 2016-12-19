import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Each instance of this class will generate two threads reading the stdout and
 * stderr of the given process. The reading may use the default charset or a
 * specified one.
 * 
 * @author Zhen Chen
 *
 */

public final class ProcessReader {
	private ProcessReaderThread outputReader;
	private ProcessReaderThread errorReader;

	private static final class ProcessReaderThread extends Thread {
		private BufferedReader reader;
		private StringBuilder builder = new StringBuilder();

		ProcessReaderThread(InputStream is) {
			this.reader = new BufferedReader(new InputStreamReader(is));
		}

		ProcessReaderThread(InputStream is, String charset) throws UnsupportedEncodingException {
			this.reader = new BufferedReader(new InputStreamReader(is, charset));
		}

		@Override
		public void run() {
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.lineSeparator());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public final String getResult() {
			return builder.toString();
		}

	}

	public ProcessReader(Process p) {
		outputReader = new ProcessReaderThread(p.getInputStream());
		errorReader = new ProcessReaderThread(p.getErrorStream());
		startReader();
	}

	public ProcessReader(Process p, String charset) throws UnsupportedEncodingException {
		outputReader = new ProcessReaderThread(p.getInputStream(), charset);
		errorReader = new ProcessReaderThread(p.getErrorStream(), charset);
		startReader();
	}

	private final void startReader() {
		outputReader.start();
		errorReader.start();
	}

	public final String getOutput() {
		try {
			outputReader.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return outputReader.getResult();
	}

	public final String getError() {
		try {
			errorReader.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return errorReader.getResult();
	}

}
