package at.jku.semantic.twitter.queries;

import java.util.Set;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.hp.hpl.jena.query.QuerySolution;

public class GeoLocationQuery extends FileTwitterQuery {

	private TreeMultiset<String> tweeters;

	@Override
	protected String getDescription() {
		return "query all tweets with geo location and gather statistics";
	}

	@Override
	protected String getQueryFileName() {
		return "TweetGeoLocation.txt";
	}

	@Override
	protected String beforeQueryProcessing() {
		tweeters = TreeMultiset.create();
		return null;
	}

	@Override
	protected String processQuerySolution(QuerySolution querySolution) {

		String nick = querySolution.getLiteral("nick").getString();
		String location = querySolution.getLiteral("loc").getString();

		tweeters.add(nick);

		return null;
	}

	@Override
	protected String afterQueryProcessing() {
		StringBuilder log = new StringBuilder();

		Set<Entry<String>> entries = tweeters.entrySet();
		for (Entry<String> entry : entries) {
			int count = entry.getCount();
			String nick = entry.getElement();

			log.append(nick).append(" has ").append(count).append(" tweets with geo location").append(NEW_LINE);
		}

		return log.toString();
	}
}
