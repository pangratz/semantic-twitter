package at.jku.semantic.twitter.queries;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.hp.hpl.jena.query.QuerySolution;

public class TweetsMentioningMeQuery extends FileTwitterQuery {

	private TreeMultiset<String> tweetersMentioningMe;

	@Override
	protected String getQueryFileName() {
		return "TweetsMentioningMe.txt";
	}

	@Override
	protected String getDescription() {
		return "get statistics of all tweeters who tweeted about you";
	}

	@Override
	protected String beforeQueryProcessing() {
		tweetersMentioningMe = TreeMultiset.create();
		return null;
	}

	@Override
	protected String processQuerySolution(QuerySolution querySolution) {

		// nick of the tweeter, who tweeted about me
		String tweeterNick = querySolution.get("tweeterNick").asLiteral().getString();
		tweetersMentioningMe.add(tweeterNick);

		return null;
	}

	@Override
	protected String afterQueryProcessing() {
		StringBuilder log = new StringBuilder();

		Set<Entry<String>> entries = tweetersMentioningMe.entrySet();
		Iterator<Entry<String>> it = entries.iterator();
		while (it.hasNext()) {
			Entry<String> entry = it.next();
			log.append(entry.getElement()).append(" tweeted ");
			log.append(entry.getCount()).append(" times about you");
			log.append(NEW_LINE);
		}

		return log.toString();
	}
}
