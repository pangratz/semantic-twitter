package at.jku.semantic.twitter.queries;

public class GeoLocationQuery extends FileTwitterQuery {

	@Override
	protected String getDescription() {
		return "query all tweets with geo location and gather statistics";
	}

	@Override
	protected String getQueryFileName() {
		return "TweetGeoLocation.txt";
	}

}
