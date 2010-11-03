package at.jku.semantic.twitter;

import java.util.Properties;

public class Main {

	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.load(TwitterRDFExtractor.class.getResourceAsStream("/twitter.properties"));
		String consumerKey = props.getProperty("twitter.oauth.consumerKey");
		String consumerSecret = props.getProperty("twitter.oauth.consumerSecret");
		String token = props.getProperty("twitter.oauth.token");
		String tokenSecret = props.getProperty("twitter.oauth.tokenSecret");

		new TwitterRDFExtractor(consumerKey, consumerSecret, token, tokenSecret);
	}

}
