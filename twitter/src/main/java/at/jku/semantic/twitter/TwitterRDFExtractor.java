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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
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

	@Deprecated
	private static void addPropertyNode(Model model, Resource res, Property prop, RDFNode node) {
		model.add(res, prop, node);
	}

	@Deprecated
	private static void addPropertyNode(Model model, String ns, Resource res, String property, Object value) {
		Property prop = ResourceFactory.createProperty(ns, property);
		if (value instanceof Resource) {
			addPropertyNode(model, res, prop, (Resource) value);
		} else {
			Literal node = ResourceFactory.createTypedLiteral(value);
			addPropertyNode(model, res, prop, node);
		}
	}

	private static void addTweeterProp(OntModel model, Resource res, String localName, Object val) {
		String typeURI = createUri(SEMANTIC_TWEETER_NS, localName);
		add(model, res, SEMANTIC_TWEETER_NS, localName, val, typeURI);
	}

	@Deprecated
	private static void addTweeterPropertyNode(Model model, Resource res, String property, Object value) {
		addPropertyNode(model, Constants.SEMANTIC_TWEETER_NS, res, property, value);
	}

	private static void addTweetProp(OntModel model, Resource res, String localName, Object val) {
		String typeURI = createUri(SEMANTIC_TWEET_NS, localName);
		add(model, res, SEMANTIC_TWEETER_NS, localName, val, typeURI);
	}

	@Deprecated
	private static void addTweetPropertyNode(Model model, Resource res, String property, Object value) {
		addPropertyNode(model, Constants.SEMANTIC_TWEET_NS, res, property, value);
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

		// nick property
		ObjectProperty nickProperty = model.createObjectProperty(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_NICK), true);
		nickProperty.addDomain(tweeterClass);
		nickProperty.addRange(XSD.xstring);
		nickProperty.addLabel("nick is the username of the tweeter on Twitter", "en");

		// status count
		ObjectProperty statusCountProperty = model.createObjectProperty(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_STATUS_COUNT));
		statusCountProperty.addDomain(tweeterClass);
		statusCountProperty.addRange(XSD.xlong);
		statusCountProperty.addLabel("number of statuses on Twitter", "en");

		// time properties
		ObjectProperty createdAtYearProperty = model.createObjectProperty(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CREATED_AT_YEAR));
		createdAtYearProperty.addDomain(tweeterClass);
		createdAtYearProperty.addDomain(XSD.xint);

		ObjectProperty createdAtMonthProperty = model
				.createObjectProperty(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CREATED_AT_MONTH));
		createdAtMonthProperty.addDomain(tweeterClass);
		createdAtMonthProperty.addDomain(XSD.xint);

		ObjectProperty createdAtDayOfWeekProperty = model.createObjectProperty(createUri(SEMANTIC_TWEETER_NS,
				SEMANTIC_TWEETER_CREATED_AT_DAY_OF_WEEK));
		createdAtDayOfWeekProperty.addDomain(tweeterClass);
		createdAtDayOfWeekProperty.addDomain(XSD.xint);

		// Tweet properties
		OntClass tweetClass = model.createClass(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_CLASS));
		tweetClass.addLabel("a tweet is a status message from a tweeter on Twitter", "en");

		// has tweet
		ObjectProperty hasTweetProperty = model.createObjectProperty(createUri(SEMANTIC_TWEETER_NS, Constants.SEMANTIC_TWEETER_HAS_TWEET));
		hasTweetProperty.addDomain(tweeterClass);
		hasTweetProperty.addRange(tweetClass);
		hasTweetProperty.addLabel("a tweeter can have many tweets", "en");

		// is retweet
		ObjectProperty isRetweetProperty = model.createObjectProperty(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_IS_RETWEET));
		isRetweetProperty.addDomain(tweetClass);
		isRetweetProperty.addRange(XSD.xboolean);

		// status length
		ObjectProperty statusLengthProperty = model.createObjectProperty(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_STATUS_LENGHT));
		statusLengthProperty.addDomain(tweetClass);
		statusLengthProperty.addRange(XSD.xint);

		// has urls
		ObjectProperty hasUrlsProperty = model.createObjectProperty(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_HAS_URLS));
		hasUrlsProperty.addDomain(tweetClass);
		hasUrlsProperty.addRange(XSD.xboolean);

		// url
		ObjectProperty urlProperty = model.createObjectProperty(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_URL));
		urlProperty.addDomain(tweetClass);
		urlProperty.addRange(XSD.xstring);

		// hash tag
		ObjectProperty hashTagProperty = model.createObjectProperty(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_HASH_TAG));
		hashTagProperty.addDomain(tweetClass);
		hashTagProperty.addRange(XSD.xstring);

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

			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_NICK, user.getScreenName());
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_STATUS_COUNT, user.getStatusesCount());

			String location = user.getLocation();
			if (location != null && !"".equals(location)) {
				addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_LOCATION, location);
			}

			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_FOLLOWER_COUNT, user.getFollowersCount());
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_FRIENDS_COUNT, user.getFriendsCount());
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_FAVOURITES_COUNT, user.getFavouritesCount());

			Date createdDate = user.getCreatedAt();
			Calendar cal = Calendar.getInstance();
			cal.setTime(createdDate);
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_CREATED_AT_YEAR, cal.get(Calendar.YEAR));
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_CREATED_AT_MONTH, cal.get(Calendar.MONTH));
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_CREATED_AT_DAY_OF_WEEK, cal.get(Calendar.DAY_OF_WEEK));

			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_UTC_OFFSET, user.getUtcOffset());
			addTweeterProp(model, userIdRes, SEMANTIC_TWEETER_TIMEZONE, user.getTimeZone());

			// add all the tweets
			ResponseList<Status> latestTweets = twitter.getUserTimeline(user.getId(), new Paging(1, user.getStatusesCount()));
			Iterator<Status> statusIt = latestTweets.iterator();
			while (statusIt.hasNext()) {
				Status status = statusIt.next();
				long statusId = status.getId();
				Resource statusRes = ResourceFactory.createResource(Constants.SEMANTIC_TWEET_NS + statusId);

				Property hasTweetProp = ResourceFactory.createProperty(Constants.SEMANTIC_TWEETER_NS, "hasTweet");
				model.add(userIdRes, hasTweetProp, statusRes);

				addTweetProp(model, statusRes, SEMANTIC_TWEET_IS_RETWEET, status.isRetweet());
				addTweetProp(model, statusRes, SEMANTIC_TWEET_STATUS_LENGHT, status.getText().length());

				URL[] urls = status.getURLs();
				boolean hasUrls = (urls != null) && (urls.length > 0);
				addTweetProp(model, statusRes, SEMANTIC_TWEET_HAS_URLS, hasUrls);
				for (URL url : urls) {
					addTweetProp(model, statusRes, SEMANTIC_TWEET_URL, url);
				}

				String[] hashtags = status.getHashtags();
				for (String hashTag : hashtags) {
					addTweetProp(model, statusRes, SEMANTIC_TWEET_HASH_TAG, hashTag);
				}

				GeoLocation geoLocation = status.getGeoLocation();
				boolean hasGeoLocation = (geoLocation != null);
				addTweetProp(model, statusRes, SEMANTIC_TWEET_HAS_GEO_LOCATION, hasGeoLocation);
				if (hasGeoLocation) {
					addTweetProp(model, statusRes, SEMANTIC_TWEET_GEO_LOCATION, geoLocation);
				}

				User[] userMentions = status.getUserMentions();
				boolean hasUserMentions = (userMentions != null) && (userMentions.length > 0);
				addTweetProp(model, statusRes, SEMANTIC_TWEET_HAS_USER_MENTIONS, hasUserMentions);
				for (User mentionedUser : userMentions) {
					addTweetProp(model, statusRes, SEMANTIC_TWEET_MENTIONED_USER, createUserResource(model, mentionedUser));
				}

				Date createdAt = status.getCreatedAt();
				Calendar tweetCal = Calendar.getInstance();
				tweetCal.setTime(createdAt);
				addTweetProp(model, statusRes, SEMANTIC_TWEET_YEAR, tweetCal.get(Calendar.YEAR));
				addTweetProp(model, statusRes, SEMANTIC_TWEET_MONTH, tweetCal.get(Calendar.MONTH));
				addTweetProp(model, statusRes, SEMANTIC_TWEET_DAY_OF_WEEK, tweetCal.get(Calendar.DAY_OF_WEEK));
			}

			File userFile = new File("userModels", user.getScreenName() + ".xml");
			model.write(new FileOutputStream(userFile));
			model.write(System.out);

			count++;

			if (count >= 2)
				break;
		}
	}
}
