package at.jku.semantic.twitter.queries;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public abstract class TwitterQuery {

	protected static final String NEW_LINE = "\n";

	public static final Model loadModels(File dir) {
		Model model = ModelFactory.createDefaultModel();
		FileManager fileManager = FileManager.get();
		File[] xmlFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		});

		for (File modelFile : xmlFiles) {
			Model loadedModel = fileManager.loadModel(modelFile.toURI().toString());
			model.add(loadedModel);
		}

		return model;
	}

	public static void main(String[] args) throws Exception {

		final Model model = TwitterQuery.loadModels(new File("userModels"));

		List<Class<? extends TwitterQuery>> queries = new LinkedList<Class<? extends TwitterQuery>>();
		queries.add(TimezoneQuery.class);
		queries.add(TweeterStatisticQuery.class);
		queries.add(TweetsMentioningMeQuery.class);
		queries.add(GeoLocationQuery.class);

		for (Class<? extends TwitterQuery> queryClass : queries) {
			TwitterQuery twitterQuery = queryClass.newInstance();
			twitterQuery.executeQuery(model);
		}
	}

	public final void executeQuery(Model model) {
		String query = getQuery();
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);

		StringBuilder log = new StringBuilder("log for ");
		log.append(getDescription()).append(NEW_LINE);

		String before = beforeQueryProcessing();
		if (before != null)
			log.append(" ###").append(NEW_LINE).append(before).append(NEW_LINE);

		String processed = processQueryResult(queryExecution.execSelect());
		if (processed != null && processed.length() > 0)
			log.append(" ###").append(NEW_LINE).append(processed).append(NEW_LINE);

		String after = afterQueryProcessing();
		if (after != null)
			log.append(" ###").append(NEW_LINE).append(after);

		System.out.println(log.toString());

		queryExecution.close();
	}

	protected String getDescription() {
		return "example query";
	}

	protected String afterQueryProcessing() {
		return null;
	}

	protected String beforeQueryProcessing() {
		return null;
	}

	protected abstract String getQuery();

	protected String processQueryResult(ResultSet result) {
		StringBuilder log = new StringBuilder();
		while (result.hasNext()) {
			QuerySolution querySolution = result.next();
			String tanga = processQuerySolution(querySolution);
			if (tanga != null)
				log.append(tanga).append(NEW_LINE);
		}
		log.trimToSize();
		return log.toString();
	}

	protected String processQuerySolution(QuerySolution querySolution) {
		return querySolution.toString();
	}
}
