package at.jku.semantic.twitter;

import java.io.File;

import junit.framework.TestCase;
import at.jku.semantic.twitter.queries.GeoLocationQuery;
import at.jku.semantic.twitter.queries.TimezoneQuery;
import at.jku.semantic.twitter.queries.TweeterStatisticQuery;
import at.jku.semantic.twitter.queries.TweetsMentioningMeQuery;
import at.jku.semantic.twitter.queries.TwitterQuery;

import com.hp.hpl.jena.rdf.model.Model;

public class TestQueries extends TestCase {

	private Model model;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		model = TwitterQuery.loadModels(new File("userModels"));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		model = null;
	}

	public void testTweeterStatisticQuery() throws Exception {
		new TweeterStatisticQuery().executeQuery(model);
	}

	public void testTweetsMentioningMeQuery() throws Exception {
		new TweetsMentioningMeQuery().executeQuery(model);
	}

	public void testGeoLocationQuery() throws Exception {
		new GeoLocationQuery().executeQuery(model);
	}

	public void testTimezoneQuery() throws Exception {
		new TimezoneQuery().executeQuery(model);
	}
}
