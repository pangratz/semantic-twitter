package at.jku.semantic.twitter.queries;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtil;

public abstract class FileTwitterQuery extends TwitterQuery {

	@Override
	protected String getQuery() {
		String fileName = getQueryFileName();
		if (!fileName.startsWith("/queries/")) {
			fileName = "/queries/" + fileName;
		}
		InputStream inStream = FileTwitterQuery.class.getResourceAsStream(fileName);
		try {
			return IOUtil.toString(inStream);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	protected abstract String getQueryFileName();

}
