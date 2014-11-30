package werewolf.objects;

import java.util.Arrays;
import java.util.Vector;

import werewolf.Werewolf;

/**
 * @author User
 * 
 */
public class Players {
	Vector<String> players;
	private int seer; // index of the player that has been nominated seer
	private int alive;

	/**
	 * Vector to store players for the next round, one the current game is full.
	 */
	private Vector<String> priority;
	/**
	 * Vector to store wolves.
	 */
	private Vector<Integer> wolves;
	/**
	 * array for checking if a player is a wolf.
	 */
	private boolean[] wolf;

	/**
	 * array for checking if a player is dead
	 */
	private boolean[] dead;

	public Players() {
		dead = new boolean[Werewolf.MAXPLAYERS];
		wolf = new boolean[Werewolf.MAXPLAYERS];
		players = new Vector<String>(Werewolf.MAXPLAYERS, 1);
		priority = new Vector<String>(Werewolf.MAXPLAYERS, 1);
		wolves = new Vector<Integer>(Werewolf.MAXPLAYERS, 1);
	}

	/**
	 * Returns the name of the player.
	 * 
	 * @param target
	 *          the player number
	 * @return the name
	 */
	public String get(int target) {
		return players.get(target);
	}
	
	public int getWolve(int target) {
		return wolves.get(target);
	}
	
	public String getWolveName(int target) {
		return get(wolves.get(target));
	}

	/**
	 * Returns the name(s) of the other wolve(s).
	 * 
	 * @param number
	 *          the number of the current wolf
	 * @return a string with the names of the other wolves
	 */
	public String getOtherWolve(int number) {
		String result = "";
		boolean first = true;
		for (int i = 0; i < wolves.size(); i++) {
			if (wolves.get(i) == number) {
				continue;
			}
			if (!first) {
				if (i == wolves.size() - 2) {
					result += " and ";
				} else {
					result += ", ";
				}
			}
			first = false;
			result += wolves.get(i);
		}
		return result;
	}

	public String getWolves() {
		String result = "";
		boolean first = true;
		for (int i = 0; i < wolves.size(); i++) {
			if (!first) {
				if (i == wolves.size() - 1) {
					result += " and ";
				} else {
					result += ", ";
				}
			}
			result += get(wolves.get(i));
			first = false;
		}
		return result;
	}

	public int numPlayers() {
		return players.size();
	}

	public int numAlive() {
		return alive;
	}

	public int numPriority() {
		return priority.size();
	}

	public boolean hasPriority() {
		return !priority.isEmpty();
	}

	public int numWolves() {
		return wolves.size();
	}

	/**
	 * Tries to add the player to the player list. Succeed it not full yet.
	 * 
	 * @param Player the player name.
	 * @return If player is added.
	 */
	public boolean addPlayer(String player) {
		if (players.size() > Werewolf.MAXPLAYERS) {
			return false;
		}
		alive++;
		return players.add(player);
	}

	public boolean addPriority(String player) {
		if (priority.size() > Werewolf.MAXPLAYERS) {
			return false;
		}
		return priority.add(player);
	}

	public boolean removePriority(String sender) {
		return priority.remove(sender);
	}

	public void remove(int sender) {
		alive--;
		players.remove(sender);
	}

	/**
	 * Replace a current player with a new player from the priority list.
	 * 
	 * @param sender
	 *          the current player number.
	 * @throws ArrayIndexOutOfBoundsException
	 *           if priority list is empty.
	 */
	public void replace(int sender) {
		players.set(sender, priority.remove(0));
	}

	public boolean isDead(int i) {
		return dead[i];
	}

	public boolean isWolf(int i) {
		return wolf[i];
	}

	public boolean isSeer(int i) {
		return seer == i;
	}

	public boolean isSeerDead() {
		return dead[seer];
	}

	public void kill(int sender) {
		alive--;
		dead[sender] = true;
		if (wolf[sender]) {
			wolves.remove(players.get(sender));
		}
	}

	public void reset() {
		players.clear();
		Arrays.fill(dead, false);
		wolves.clear();
		Arrays.fill(wolf, false);
		randList = null;
		alive = 0;
	}

	public void start(int maxPlayers) {
		for (int i = 0; i < maxPlayers; i++) {
			if (priority.isEmpty()) {
				break;
			}
			addPlayer(priority.remove(0));
		}
	}

	// method to go through the player and priority lists, to check if the player
	// has already joined the game
	public boolean isAdded(String aName) {
		if (priority.contains(aName)) {
			return true;
		}
		return isPlaying(aName);
	}

	// go through the player list, check the player is in the current game
	public boolean isPlaying(String aName) {
		return getPlayerNumber(aName) != -1;
	}

	/**
	 * Returns the number of the player. Returns -1 if player does not exists.
	 * 
	 * @param aName
	 *          the name of the player
	 * @return the number of the player
	 */
	public int getPlayerNumber(String aName) {
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).equalsIgnoreCase(aName)) {
				return i;
			}
		}
		return -1;
	}

	public int getSeer() {
		return seer;
	}

	/**
	 * Changes the nickname of a player.
	 * 
	 * @param oldNick
	 *          the old nickname.
	 * @param newNick
	 *          the new nickname.
	 * @return False if the old nick is not found. True if a nickname is changed.
	 */
	public boolean changeNick(String oldNick, String newNick) {
		int loc;
		if ((loc = players.indexOf(oldNick)) != -1) {
			players.set(loc, newNick);
			return true;
		}
		if ((loc = priority.indexOf(oldNick)) != -1) {
			priority.set(loc, newNick);
			return true;
		}
		return false;
	}

	public boolean isDead(String aName) {
		// TODO handle arrayOutOfBound if getPlayerNumber returns -1 
		return isDead(getPlayerNumber(aName));
	}

	public void setRoles(int wolves, int seers) {
		int rnd;
		for (int i = 0; i < wolves; i++) {
			rnd = getRandomPlayer();
			wolf[rnd] = true;
			this.wolves.add(rnd);
		}
		for (int i = 0; i < seers; i++) {
			seer = getRandomPlayer();
		}
	}

	Vector<Integer> randList;

	/**
	 * Returns a random player. Never picks the same player twice.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if there are no more unique players. 
	 * 
	 * @return the number of the random player
	 */
	private int getRandomPlayer() {
		if (randList == null) {
			randList = new Vector<Integer>(numPlayers(), 1);
			for (int i = 0; i < numPlayers(); i++) {
				randList.add(i);
			}
		}
		return randList.remove((int) (Math.random() * randList.size()));
	}
}

