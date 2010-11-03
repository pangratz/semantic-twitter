package at.jku.semantic.twitter.queries;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class TweeterStatisticQuery extends FileTwitterQuery {

	private long userCount;
	private long followersCount, friendsCount, favouritesCount, statusCount;
	private long minFollowersCount, minFriendsCount, minFavouritesCount, minStatusCount;
	private long maxFollowersCount, maxFriendsCount, maxFavouritesCount, maxStatusCount;

	@Override
	protected String getQueryFileName() {
		return "TweeterStatistic.txt";
	}

	@Override
	protected String getDescription() {
		return "get statistics (tweet count, follower count, ...) of your friends";
	}

	@Override
	protected String processQuerySolution(QuerySolution querySolution) {

		// get all attributes
		long userFollowerCount = parseLong(querySolution.get("followerCount"));
		long userFriendsCount = parseLong(querySolution.get("friendsCount"));
		long userFavouritesCount = parseLong(querySolution.get("favoritesCount"));
		long userStatusCount = parseLong(querySolution.get("statusCount"));

		minFollowersCount = Math.min(minFollowersCount, userFollowerCount);
		minFriendsCount = Math.min(minFriendsCount, userFriendsCount);
		minFavouritesCount = Math.min(minFavouritesCount, userFavouritesCount);
		minStatusCount = Math.min(minStatusCount, userStatusCount);

		maxFollowersCount = Math.max(maxFollowersCount, userFollowerCount);
		maxFriendsCount = Math.max(maxFriendsCount, userFriendsCount);
		maxFavouritesCount = Math.max(maxFavouritesCount, userFavouritesCount);
		maxStatusCount = Math.max(maxStatusCount, userStatusCount);

		followersCount += userFollowerCount;
		friendsCount += userFriendsCount;
		favouritesCount += userFavouritesCount;
		statusCount += userStatusCount;
		userCount++;

		return null;
	}

	private long parseLong(RDFNode rdfNode) {
		String s = rdfNode.asLiteral().getString();
		return Long.parseLong(s);
	}

	@Override
	protected String afterQueryProcessing() {
		long avgFollowersCount = followersCount / userCount;
		long avgFriendsCount = friendsCount / userCount;
		long avgFavouritesCount = favouritesCount / userCount;
		long avgStatusCount = statusCount / userCount;

		StringBuilder log = new StringBuilder();
		log.append("there are following statistics for a tweeter: ").append(NEW_LINE);

		appendStats(log, "followers", minFollowersCount, avgFollowersCount, maxFollowersCount);
		log.append(NEW_LINE);
		appendStats(log, "friends", minFriendsCount, avgFriendsCount, maxFriendsCount);
		log.append(NEW_LINE);
		appendStats(log, "favourites", minFavouritesCount, avgFavouritesCount, maxFavouritesCount);
		log.append(NEW_LINE);
		appendStats(log, "statuses", minStatusCount, avgStatusCount, maxStatusCount);

		return log.toString();
	}

	private void appendStats(StringBuilder sb, String name, long min, long avg, long max) {
		sb.append(name).append(" (min/avg/max) = (").append(min).append("/");
		sb.append(avg).append("/").append(max).append(")");
	}
}
