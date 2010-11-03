package at.jku.semantic.twitter;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import twitter4j.GeoLocation;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.XSD;

public class TwitterRDFExtractor implements Constants {

	public TwitterRDFExtractor(String consumerKey, String consumerSecret, String token, String tokenSecret) throws Exception {
		super();

		AccessToken accessToken = new AccessToken(token, tokenSecret);
		Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);

		createModels(twitter);
	}

	public Model extractModel(Twitter twitter, User user) throws TwitterException {
		OntModel model = createOntologyModel();

		Resource userIdRes = createTweeterResource(model, user);

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
		int statusesCount = user.getStatusesCount();
		if (statusesCount > 0) {
			ResponseList<Status> latestTweets = twitter.getUserTimeline(user.getId(), new Paging(1, statusesCount));
			Iterator<Status> statusIt = latestTweets.iterator();
			while (statusIt.hasNext()) {
				Status status = statusIt.next();
				Resource statusRes = createTweetResource(model, status);

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
					addTweetProperty(model, statusRes, SEMANTIC_TWEET_MENTIONED_USER, createTweeterResource(model, mentionedUser));
				}

				Date createdAt = status.getCreatedAt();
				Calendar tweetCal = Calendar.getInstance();
				tweetCal.setTime(createdAt);
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_YEAR, tweetCal.get(Calendar.YEAR));
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_MONTH, tweetCal.get(Calendar.MONTH));
				addTweetProperty(model, statusRes, SEMANTIC_TWEET_DAY_OF_WEEK, tweetCal.get(Calendar.DAY_OF_WEEK));
			}
		}

		return model;
	}

	public void createModels(Twitter twitter) throws Exception {
		// iterate over each friend and create a RDF model for each one
		IDs friendsIDs = twitter.getFriendsIDs();
		int[] iDs = friendsIDs.getIDs();
		for (int id : iDs) {

			boolean ok = false;
			do {
				try {

					User user = twitter.showUser(id);
					File userFile = new File("userModels2", user.getScreenName() + ".xml");
					if (userFile.exists()) {
						System.out.println("skipping user " + user.getScreenName() + " because the model already exists");
						ok = true;
					} else {
						System.out.println("creating model for " + user.getScreenName());
						Model model = extractModel(twitter, user);
						System.out.println(" model created");

						model.write(new FileOutputStream(userFile));
						System.out.println(" model written to file " + userFile.getName());
						ok = true;
					}
				} catch (Exception ex) {
					System.out.println(" exception --> wait for 30 seconds");
					Thread.sleep(1000 * 30);
				}
			} while (!ok);

		}
	}

	private void add(OntModel model, Resource res, String ns, String localName, Object val, String typeURI) {
		if (val != null) {
			Property prop = model.getProperty(ns, localName);
			Literal lit = model.createTypedLiteral(val, typeURI);
			model.add(res, prop, lit);
		}
	}

	private ObjectProperty addOntProperty(OntModel model, String ns, String localName, Resource domain, Resource range) {
		ObjectProperty objProperty = model.createObjectProperty(createUri(ns, localName));
		objProperty.addDomain(domain);
		objProperty.addRange(range);
		return objProperty;
	}

	private void addTweeterProperty(OntModel model, Resource res, String localName, Object val) {
		String typeURI = createUri(SEMANTIC_TWEETER_NS, localName);
		add(model, res, SEMANTIC_TWEETER_NS, localName, val, typeURI);
	}

	private void addTweetProperty(OntModel model, Resource res, String localName, Object val) {
		String typeURI = createUri(SEMANTIC_TWEET_NS, localName);
		add(model, res, SEMANTIC_TWEETER_NS, localName, val, typeURI);
	}

	private OntModel createOntologyModel() {
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

	private String createUri(String ns, String localName) {
		return ns + localName;
	}

	private Resource createTweeterResource(OntModel model, User user) {
		Resource clazz = model.getResource(createUri(SEMANTIC_TWEETER_NS, SEMANTIC_TWEETER_CLASS));
		return model.createResource(Constants.SEMANTIC_TWEETER_NS + user.getScreenName(), clazz);
	}

	private Resource createTweetResource(OntModel model, Status status) {
		Resource clazz = model.getResource(createUri(SEMANTIC_TWEET_NS, SEMANTIC_TWEET_CLASS));
		return model.createResource(Constants.SEMANTIC_TWEET_NS + status.getId(), clazz);
	}
}
