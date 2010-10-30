package at.jku.semantic.twitter;

import java.util.Properties;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.http.AccessToken;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String args[]) throws Exception {
		Properties props = new Properties();
		props.load(App.class.getResourceAsStream("/twitter.properties"));
		String consumerKey = props.getProperty("twitter.oauth.consumerKey");
		String consumerSecret = props.getProperty("twitter.oauth.consumerSecret");
		String token = props.getProperty("twitter.oauth.token");
		String tokenSecret = props.getProperty("twitter.oauth.tokenSecret");

		AccessToken accessToken = new AccessToken(token, tokenSecret);
		Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);

		IDs friendsIDs = twitter.getFriendsIDs();
		int[] iDs = friendsIDs.getIDs();
		for (int id : iDs) {
			User user = twitter.showUser(id);
			System.out.println("user: " + user.getName());
		}
	}
}
