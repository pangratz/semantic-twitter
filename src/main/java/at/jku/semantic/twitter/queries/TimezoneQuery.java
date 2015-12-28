package at.jku.semantic.twitter.queries;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class TimezoneQuery extends FileTwitterQuery {

	private TreeMultiset<String> timezones;

	public TimezoneQuery() {
		super();

	}

	@Override
	protected String getQueryFileName() {
		return "TweeterLocation.txt";
	}

	@Override
	protected String getDescription() {
		return "query all tweets and check the different time zones of your friends";
	}

	@Override
	protected String beforeQueryProcessing() {
		timezones = TreeMultiset.create();
		return null;
	}

	@Override
	protected String processQuerySolution(QuerySolution querySolution) {
		RDFNode timezoneNode = querySolution.get("timezone");
		String timezone = (timezoneNode != null) ? timezoneNode.asLiteral().getString() : null;

		RDFNode nameNode = querySolution.get("name");
		String name = (nameNode != null) ? nameNode.asLiteral().getString() : null;

		if (timezone == null)
			timezone = "null";

		timezones.add(timezone);

		return null;
	}

	@Override
	protected String afterQueryProcessing() {
		StringBuilder log = new StringBuilder();

		Set<Entry<String>> entries = timezones.entrySet();
		Iterator<Entry<String>> it = entries.iterator();
		while (it.hasNext()) {
			Entry<String> entry = it.next();
			String timezone = entry.getElement();
			int count = entry.getCount();

			if ("null".equals(timezone))
				log.append(count).append(" users haven't defined a timezone").append(NEW_LINE);
			else
				log.append(count).append(" users are in ").append(timezone).append(NEW_LINE);
		}

		return log.toString();
	}

}
