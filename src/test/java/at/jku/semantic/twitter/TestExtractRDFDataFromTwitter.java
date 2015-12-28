package at.jku.semantic.twitter;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

public class TestExtractRDFDataFromTwitter extends TestCase {

	// set to false, if you want to extract all the data for each friend of
	// yours on twitter --> time consuming
	boolean ignore = true;

	public void testExtractRDFData() throws Exception {

		if (ignore)
			return;

		// read the OAuth keys/secrets from properties file
		Properties props = new Properties();
		props.load(TwitterRDFExtractor.class.getResourceAsStream("/twitter.properties"));
		String consumerKey = props.getProperty("twitter.oauth.consumerKey");
		String consumerSecret = props.getProperty("twitter.oauth.consumerSecret");
		String token = props.getProperty("twitter.oauth.token");
		String tokenSecret = props.getProperty("twitter.oauth.tokenSecret");

		File dir = new File("userModels");
		dir.mkdirs();

		new TwitterRDFExtractor(dir, consumerKey, consumerSecret, token, tokenSecret);
	}

}
