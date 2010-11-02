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

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Hello world!
 * 
 */
public class TwitterRDFExtractor implements Constants {

	private static void add(OntModel model, Resource res, String ns, String localName, Object val, String typeURI) {
		if (val != null) {
			Property prop = model.getProperty(ns, localName);
			Literal lit = model.createTypedLiteral(val, typeURI);
			model.add(res, prop, lit);
		}
	}

	private static void addTweeterProperty(OntModel model, Resource res, String localName, Object val) {
		String typeURI = createUri(SEMANTIC_TWEETER_NS, localName);
		add(model, res, SEMANTIC_TWEETER_NS, localName, val, typeURI);
	}

	private static void addTweetProperty(OntModel model, Resource res, String localName, Object val) {
		String typeURI = createUri(SEMANTIC_TWEET_NS, localName);
		add(model, res, SEMANTIC_TWEETER_NS, localName, val, typeURI);
	}

	private static ObjectProperty addOntProperty(OntModel model, String ns, String localName, Resource domain, Resource range) {
		ObjectProperty objProperty = model.createObjectProperty(createUri(ns, localName));
		objProperty.addDomain(domain);
		objProperty.addRange(range);
		return objProperty;
	}

	private static OntModel createOntologyModel() {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF);
		model.setNsPrefix("foaf", Constants.FOAF_NS);
		model.setNsPrefix("stweeter", Constants.SEMANTIC_TWEETER_NS);
		model.setNsPrefix("stweet", Constants.SEMANTIC_TWEET_NS);

		// Tweeter class
		OntClass tweeterClass = model.createClass(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CLASS));
		tweeterClass.setSuperClass(FOAF.Person);
		tweeterClass.addLabel("a tweeter is a user on Twitter", "en");
		{
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_NICK, tweeterClass, XSD.xstring);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_STATUS_COUNT, tweeterClass, XSD.xlong);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_LOCATION, tweeterClass, XSD.xstring);

			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_FOLLOWER_COUNT, tweeterClass, XSD.xlong);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_FRIENDS_COUNT, tweeterClass, XSD.xlong);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_FAVOURITES_COUNT, tweeterClass, XSD.xlong);

			// time properties
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_UTC_OFFSET, tweeterClass, XSD.xlong);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_TIMEZONE, tweeterClass, XSD.xstring);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CREATED_AT_YEAR, tweeterClass, XSD.xint);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CREATED_AT_MONTH, tweeterClass, XSD.xint);
			addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CREATED_AT_DAY_OF_WEEK, tweeterClass, XSD.xint);
		}

		// Tweet properties
		OntClass tweetClass = model.createClass(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_CLASS));
		tweetClass.addLabel("a tweet is a status message from a tweeter on Twitter", "en");

		// has tweet
		ObjectProperty hasTweetProperty = addOntProperty(model, SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_HAS_TWEET, tweeterClass, tweetClass);
		hasTweetProperty.addLabel("a tweeter can have many tweets", "en");

		{
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_IS_RETWEET, tweetClass, XSD.xboolean);
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_STATUS_LENGHT, tweetClass, XSD.xint);

			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_HAS_URLS, tweetClass, XSD.xboolean);
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_URL, tweetClass, XSD.xstring);

			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_HASH_TAG, tweetClass, XSD.xstring);
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_GEO_LOCATION, tweetClass, XSD.xstring);

			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_HAS_USER_MENTIONS, tweetClass, XSD.xboolean);
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_MENTIONED_USER, tweetClass, tweeterClass);

			// time properties
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_YEAR, tweetClass, XSD.xint);
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_MONTH, tweetClass, XSD.xint);
			addOntProperty(model, SEMANTIC_TWEET_NS, SEMANTIC_TWEET_DAY_OF_WEEK, tweetClass, XSD.xint);
		}

		return model;
	}

	private static String createUri(String ns, String localName) {
		return ns + localName;
	}

	private static Resource createUserResource(OntModel model, User user) {
		Resource clazz = model.getResource(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CLASS));
		return model.createResource(Constants.SEMANTIC_TWEETER_NS + user.getScreenName(), clazz);
	}

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

			OntModel model = createOntologyModel();

			Resource userIdRes = createUserResource(model, user);

			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_NICK, user.getScreenName());
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_STATUS_COUNT, user.getStatusesCount());

			String location = user.getLocation();
			if (location != null && !"".equals(location)) {
				addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_LOCATION, location);
			}

			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_FOLLOWER_COUNT, user.getFollowersCount());
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_FRIENDS_COUNT, user.getFriendsCount());
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_FAVOURITES_COUNT, user.getFavouritesCount());

			Date createdDate = user.getCreatedAt();
			Calendar cal = Calendar.getInstance();
			cal.setTime(createdDate);
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_CREATED_AT_YEAR, cal.get(Calendar.YEAR));
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_CREATED_AT_MONTH, cal.get(Calendar.MONTH));
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_CREATED_AT_DAY_OF_WEEK, cal.get(Calendar.DAY_OF_WEEK));

			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_UTC_OFFSET, user.getUtcOffset());
			addTweeterProperty(model, userIdRes, SEMANTIC_TWEETER_TIMEZONE, user.getTimeZone());

			// add all the tweets
			ResponseList<Status> latestTweets = twitter.getUserTimeline(user.getId(), new Paging(1, user.getStatusesCount()));
			Iterator<Status> statusIt = latestTweets.iterator();
			while (statusIt.hasNext()) {
				Status status = statusIt.next();
				long statusId = status.getId();
				Resource statusRes = ResourceFactory.createResource(Constants.SEMANTIC_TWEET_NS + statusId);

				Property hasTweetProp = ResourceFactory.createProperty(Constants.SEMANTIC_TWEETER_NS, "hasTweet");
				model.add(userIdRes, hasTweetProp, statusRes);

				addTweetProperty(model, statusRes, SEMANTIC_TWEET_IS_RETWEET, status.isRetweet());
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_STATUS_LENGHT, status.getText().length());

				URL[] urls = status.getURLs();
				boolean hasUrls = (urls != null) && (urls.length > 0);
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_HAS_URLS, hasUrls);
				for (URL url : urls) {
					addTweetProperty(model, statusRes, SEMANTIC_TWEET_URL, url);
				}

				String[] hashtags = status.getHashtags();
				for (String hashTag : hashtags) {
					addTweetProperty(model, statusRes, SEMANTIC_TWEET_HASH_TAG, hashTag);
				}

				GeoLocation geoLocation = status.getGeoLocation();
				boolean hasGeoLocation = (geoLocation != null);
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_HAS_GEO_LOCATION, hasGeoLocation);
				if (hasGeoLocation) {
					addTweetProperty(model, statusRes, SEMANTIC_TWEET_GEO_LOCATION, geoLocation);
				}

				User[] userMentions = status.getUserMentions();
				boolean hasUserMentions = (userMentions != null) && (userMentions.length > 0);
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_HAS_USER_MENTIONS, hasUserMentions);
				for (User mentionedUser : userMentions) {
					addTweetProperty(model, statusRes, SEMANTIC_TWEET_MENTIONED_USER, createUserResource(model, mentionedUser));
				}

				Date createdAt = status.getCreatedAt();
				Calendar tweetCal = Calendar.getInstance();
				tweetCal.setTime(createdAt);
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_YEAR, tweetCal.get(Calendar.YEAR));
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_MONTH, tweetCal.get(Calendar.MONTH));
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_DAY_OF_WEEK, tweetCal.get(Calendar.DAY_OF_WEEK));
			}

			File userFile = new File("userModels", user.getScreenName() + ".xml");
			model.write(new FileOutputStream(userFile));
			// model.write(System.out);

			count++;

			if (count >= 2)
				break;
		}
	}
}
