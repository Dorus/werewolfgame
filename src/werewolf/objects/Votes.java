package werewolf.objects;

import java.util.Arrays;
import java.util.Vector;

/**
 * Handels player voting and elects victem.
 * 
 * @author Dorus Peelen
 * 
 */
public class Votes {
	private int[] votes;
	private final int players;
	private int[] notVoted;

	public Votes(int players) {
		this.players = players;
		votes = new int[players];
		Arrays.fill(votes, -1);
		notVoted = new int[players];
	}

	/**
	 * Reset the votes back to -1 and increase the counter for players that did
	 * not vote last round.
	 * 
	 * Will reset count non-voters if they voted this round.
	 */
	public void reset() {
		votes = new int[players];

		for (int i = 0; i < votes.length; i++) {
			if (votes[i] == -1) {
				notVoted[i]++;
			} else {
				votes[i] = -1;
				notVoted[i] = 0;
			}
		}
	}

	/**
	 * Check if player has voted this round.
	 * 
	 * @param player
	 *          in question.
	 * @return if player has voted.
	 */
	public boolean hasVoted(int player) {
		return votes[player] != -1;
	}

	/**
	 * Set vote from sender to target.
	 * 
	 * @param sender
	 *          who cast the vote.
	 * @param target
	 *          who receives the vote.
	 */
	public void addVote(int sender, int target) {
		votes[sender] = target;
	}

	/**
	 * Remove votes from and to player. For example when the player leaves the
	 * game.
	 * 
	 * @param player
	 *          that can no longer receive or cast votes.
	 */
	public void removeVote(int player) {
		votes[player] = -1;
		for (int i = 0; i < votes.length; i++) {
			if (votes[i] == player) {
				votes[i] = -1;
			}
		}
	}

	/**
	 * Find the target who receiver most votes. Can return multiple targets on
	 * tie.
	 * 
	 * @return the list of target(s).
	 */
	public Vector<Integer> getTarget() {
		int[] voteCount = new int[players];
		Vector<Integer> victem = new Vector<Integer>(1, 1);
		int max = -1;
		for (int i : votes) {
			if (i != -1) {
				max = Math.max(++voteCount[i], max);
			}
		}
		if (max == -1) {
			return victem;
		}
		for (int i = 0; i < players; i++) {
			if (voteCount[i] == max) {
				victem.add(i);
			}
		}
		return victem;
	}

	/**
	 * Return a list of players that did not vote last times rounds.
	 * 
	 * @param times
	 *          the players did not vote in constructive rounds.
	 * @return a list of players.
	 */
	public Vector<Integer> notVoted(int times) {
		Vector<Integer> hasNotVoted = new Vector<Integer>(1, 1);
		for (int i = 0; i < notVoted.length; i++) {
			if (notVoted[i] >= times) {
				hasNotVoted.add(i);
			}
		}
		return hasNotVoted;
	}
}
