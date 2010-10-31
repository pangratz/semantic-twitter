package at.jku.semantic.twitter;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import twitter4j.GeoLocation;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.http.AccessToken;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Hello world!
 * 
 */
public class TwitterRDFExtractor {

	public static void main(String args[]) throws Exception {
		Properties props = new Properties();
		props.load(TwitterRDFExtractor.class.getResourceAsStream("/twitter.properties"));
		String consumerKey = props.getProperty("twitter.oauth.consumerKey");
		String consumerSecret = props.getProperty("twitter.oauth.consumerSecret");
		String token = props.getProperty("twitter.oauth.token");
		String tokenSecret = props.getProperty("twitter.oauth.tokenSecret");

		AccessToken accessToken = new AccessToken(token, tokenSecret);
		Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);

		// iterate over each friend and create a RDF model for each one
		IDs friendsIDs = twitter.getFriendsIDs();
		int[] iDs = friendsIDs.getIDs();
		int count = 0;
		for (int id : iDs) {

			User user = twitter.showUser(id);

			Model userModel = ModelFactory.createDefaultModel();
			userModel.setNsPrefix("foaf", Constants.FOAF_NS);
			userModel.setNsPrefix("stweeter", Constants.SEMANTIC_TWEETER_NS);
			userModel.setNsPrefix("stweet", Constants.SEMANTIC_TWEET_NS);

			Resource userIdRes = createUserResource(user);
			Property nickProp = ResourceFactory.createProperty(Constants.FOAF_NS, "nick");
			Literal nickLit = ResourceFactory.createTypedLiteral(user.getScreenName());
			userModel.add(userIdRes, nickProp, nickLit);

			addTweeterPropertyNode(userModel, userIdRes, "statusCount", user.getStatusesCount());

			String location = user.getLocation();
			if (location != null && !"".equals(location))
				addTweeterPropertyNode(userModel, userIdRes, "location", location);

			addTweeterPropertyNode(userModel, userIdRes, "followerCount", user.getFollowersCount());
			addTweeterPropertyNode(userModel, userIdRes, "friendsCount", user.getFriendsCount());
			addTweeterPropertyNode(userModel, userIdRes, "favoritesCount", user.getFavouritesCount());

			Date createdDate = user.getCreatedAt();
			Calendar cal = Calendar.getInstance();
			cal.setTime(createdDate);
			addTweeterPropertyNode(userModel, userIdRes, "createdAtYear", cal.get(Calendar.YEAR));
			addTweeterPropertyNode(userModel, userIdRes, "createdAtMonth", cal.get(Calendar.MONTH));
			addTweeterPropertyNode(userModel, userIdRes, "createdAtDay", cal.get(Calendar.DAY_OF_MONTH));
			addTweeterPropertyNode(userModel, userIdRes, "createdAtDayOfWeek", cal.get(Calendar.DAY_OF_WEEK));
			addTweeterPropertyNode(userModel, userIdRes, "createdAtTimestamp", cal.getTimeInMillis());

			addTweeterPropertyNode(userModel, userIdRes, "utcOffset", user.getUtcOffset());
			addTweeterPropertyNode(userModel, userIdRes, "timezone", user.getTimeZone());

			// add all the tweets
			ResponseList<Status> latestTweets = twitter.getUserTimeline(user.getId(), new Paging(1, user.getStatusesCount()));
			Iterator<Status> statusIt = latestTweets.iterator();
			while (statusIt.hasNext()) {
				Status status = statusIt.next();
				long statusId = status.getId();
				Resource statusRes = ResourceFactory.createResource(Constants.SEMANTIC_TWEET_NS + statusId);

				Property hasTweetProp = ResourceFactory.createProperty(Constants.SEMANTIC_TWEETER_NS, "hasTweet");
				userModel.add(userIdRes, hasTweetProp, statusRes);

				addTweetPropertyNode(userModel, statusRes, "isRetweet", status.isRetweet());
				addTweetPropertyNode(userModel, statusRes, "statusLength", status.getText().length());

				URL[] urls = status.getURLs();
				boolean hasUrls = (urls != null) && (urls.length > 0);
				addTweetPropertyNode(userModel, statusRes, "hasUrls", hasUrls);
				for (URL url : urls) {
					addTweetPropertyNode(userModel, statusRes, "url", url);
				}

				String[] hashtags = status.getHashtags();
				for (String hashTag : hashtags) {
					addTweetPropertyNode(userModel, statusRes, "hasHashTag", hashTag);
				}

				GeoLocation geoLocation = status.getGeoLocation();
				boolean hasGeoLocation = (geoLocation != null);
				addTweetPropertyNode(userModel, statusRes, "hasGeoLocation", hasGeoLocation);
				if (hasGeoLocation) {
					addTweetPropertyNode(userModel, statusRes, "geoLocation", geoLocation);
				}

				User[] userMentions = status.getUserMentions();
				boolean hasUserMentions = (userMentions != null) && (userMentions.length > 0);
				addTweetPropertyNode(userModel, statusRes, "hasUserMentions", hasUserMentions);
				for (User mentionedUser : userMentions) {
					addTweetPropertyNode(userModel, statusRes, "mentionedUser", createUserResource(mentionedUser));
				}

				Date createdAt = status.getCreatedAt();
				Calendar tweetCal = Calendar.getInstance();
				tweetCal.setTime(createdAt);
				addTweetPropertyNode(userModel, statusRes, "tweetYear", tweetCal.get(Calendar.YEAR));
				addTweetPropertyNode(userModel, statusRes, "tweetMonth", tweetCal.get(Calendar.MONTH));
				addTweetPropertyNode(userModel, statusRes, "tweetDay", tweetCal.get(Calendar.DAY_OF_MONTH));
				addTweetPropertyNode(userModel, statusRes, "tweetHour", tweetCal.get(Calendar.HOUR_OF_DAY));
				addTweetPropertyNode(userModel, statusRes, "tweetMinute", tweetCal.get(Calendar.MINUTE));
				addTweetPropertyNode(userModel, statusRes, "tweetTimestamp", tweetCal.getTimeInMillis());
			}

			File userFile = new File("userModels", user.getScreenName() + ".xml");
			userModel.write(new FileOutputStream(userFile));

			count++;

			if (count >= 1)
				break;
		}
	}

	private static Resource createUserResource(User user) {
		return ResourceFactory.createResource(Constants.FOAF_NS + user.getId());
	}

	private static void addPropertyNode(Model model, String ns, Resource res, String property, Object value) {
		Property prop = ResourceFactory.createProperty(ns, property);
		if (value instanceof Resource) {
			addPropertyNode(model, res, prop, (Resource) value);
		} else {
			Literal node = ResourceFactory.createTypedLiteral(value);
			addPropertyNode(model, res, prop, node);
		}
	}

	private static void addTweeterPropertyNode(Model model, Resource res, String property, Object value) {
		addPropertyNode(model, Constants.SEMANTIC_TWEETER_NS, res, property, value);
	}

	private static void addTweetPropertyNode(Model model, Resource res, String property, Object value) {
		addPropertyNode(model, Constants.SEMANTIC_TWEET_NS, res, property, value);
	}

	private static void addPropertyNode(Model model, Resource res, Property prop, RDFNode node) {
		model.add(res, prop, node);
	}
}
