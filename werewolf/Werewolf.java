package werewolf;

/******************
 * Werewolf.java
 * Main code file for The Werewolf Game bot, based on pIRC Bot framework (www.jibble.org)
 * Coded by Dorus Peelen
 * Based on code by Mark Vine
 * All death/character/other description texts written by Darkshine
 * 31/5/2004 - 4/6/2004
 * v1.00
 *****************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import werewolf.objects.GameStatus;
import werewolf.objects.Players;
import werewolf.objects.Votes;

public class Werewolf extends PircBot {
	private Votes votes2;
	private Votes wolfVictim2;
	private Players players2;

	public final static int // (final because cannot be altered)
			MINPLAYERS = 5, // Minimum number of players to start a game
			MAXPLAYERS = 12, // Maximum number of players allowed in the game
			TWOWOLVES = 8, // Minimum number of players needed for 2 wolves

			// Final ints to describe the types of message that can be sent
			// (Narration, game, control, notice), so
			// they can be coloured accordingly after being read from the file
			NOTICE = 1, NARRATION = 2, GAME = 3, CONTROL = 4, NONE = 5;

	private int joinTime = 60, // time (in seconds) for people to join the game
			dayTime = 90, // time (in seconds) for daytime duration (variable because
			// it changes with players)
			nightTime = 60, // time (in seconds) for night duration
			voteTime = 30, // time (in seconds) for the lynch vote
			toSee = -1; // index of the player the seer has selected to see. If no
									// player, this is -1

	private boolean connected = false, // boolean to show if the bot is connected
			firstNight, // boolean to show if it's the first night (unique night
									// message)
			tieGame = true, // boolean to determine if there will be a random tie
											// break with tied votes.
			debug, // boolean to show if debug mode is one or off (Print to log files)
			doBarks; // boolean to show if the bot should make random comments after
								// long periods of inactivity

	private GameStatus status = GameStatus.IDLE;

	private Timer gameTimer, // The game Timer (duh)
			idleTimer; // timer to count idle time for random comments

	private String name, // The bot's name
			network, // The network to connect to
			gameChan, // The channel the game is played in
			command, // The command the bot sends to the nickservice to identify on
								// the network
			gameFile; // Specifies the file name to read the game texts from.
	private long delay; // The delay for messages to be sent

	public Werewolf() {
		this.setLogin("Werewolf");
		this.setVersion("Werewolf Game Bot by LLamaBoy and Darkshine - using pIRC framework from http://www.jible.org");
		this.setMessageDelay(500);

		String filename = "werewolf.ini", lineRead = "";
		gameFile = "wolfgame.txt";
		BufferedReader buff;

		try {
			buff = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + filename)));

			while (!lineRead.startsWith("botname"))
				lineRead = buff.readLine();
			name = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());

			while (!lineRead.startsWith("network"))
				lineRead = buff.readLine();
			network = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());

			while (!lineRead.startsWith("channel"))
				lineRead = buff.readLine();
			gameChan = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());

			while (!lineRead.startsWith("nickcmd"))
				lineRead = buff.readLine();
			command = lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length());

			while (!lineRead.startsWith("debug"))
				lineRead = buff.readLine();

			String onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if (onoff.equalsIgnoreCase("on"))
				debug = true;
			else if (onoff.equalsIgnoreCase("off"))
				debug = false;
			else {
				System.out.println("Unknown debug value, defaulting to on.");
				debug = true;
			}

			while (!lineRead.startsWith("delay"))
				lineRead = buff.readLine();

			delay = Long.parseLong(lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length()));
			this.setMessageDelay(delay);

			while (!lineRead.startsWith("jointime"))
				lineRead = buff.readLine();
			try {
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				joinTime = tmp;
			} catch (NumberFormatException nfx) {
				System.out.println("Bad day time value; defaulting to 60 seconds");
				joinTime = 60;
			}

			while (!lineRead.startsWith("daytime"))
				lineRead = buff.readLine();
			try {
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				dayTime = tmp;
			} catch (NumberFormatException nfx) {
				System.out.println("Bad day time value; defaulting to 90 seconds");
				dayTime = 90;
			}

			while (!lineRead.startsWith("nighttime"))
				lineRead = buff.readLine();
			try {
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				nightTime = tmp;
			} catch (NumberFormatException nfx) {
				System.out.println("Bad night time value; defaulting to 45 seconds");
				nightTime = 45;
			}

			while (!lineRead.startsWith("votetime"))
				lineRead = buff.readLine();
			try {
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				voteTime = tmp;
			} catch (NumberFormatException nfx) {
				System.out.println("Bad vote time value; defaulting to 90 seconds");
				voteTime = 30;
			}

			while (!lineRead.startsWith("tie"))
				lineRead = buff.readLine();

			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if (onoff.equalsIgnoreCase("on"))
				tieGame = true;
			else if (onoff.equalsIgnoreCase("off"))
				tieGame = false;
			else {
				System.out.println("Unknown vote tie value, defaulting to on.");
				tieGame = true;
			}

			while (!lineRead.startsWith("idlebarks"))
				lineRead = buff.readLine();

			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if (onoff.equalsIgnoreCase("on"))
				doBarks = true;
			else if (onoff.equalsIgnoreCase("off"))
				doBarks = false;
			else {
				System.out.println("Unknown vote tie value, defaulting to off.");
				doBarks = true;
			}
		} catch (FileNotFoundException fnfx) {
			System.err.println("Initialization  file " + filename + " not found.");
			fnfx.printStackTrace();
			System.exit(1);
		} catch (IOException iox) {
			System.err.println("File read Exception");
			iox.printStackTrace();
			System.exit(1);
		} catch (Exception x) {
			System.err.println("Other Exception caught");
			x.printStackTrace();
			System.exit(1);
		}

		if (debug) {
			this.setVerbose(true);
			try {
				File file = new File("wolf.log");
				if (!file.exists())
					file.createNewFile();
				PrintStream fileLog = new PrintStream(new FileOutputStream(file, true));
				System.setOut(fileLog);
				System.out.println((new Date()).toString());
				System.out.println("Starting log....");

				File error = new File("error.log");
				if (!file.exists())
					file.createNewFile();
				PrintStream errorLog = new PrintStream(new FileOutputStream(error, true));
				System.setErr(errorLog);
				System.err.println((new Date()).toString());
				System.err.println("Starting error log....");

			} catch (FileNotFoundException fnfx) {
				fnfx.printStackTrace();
			} catch (IOException iox) {
				iox.printStackTrace();
			}
		}

		connectAndJoin();
		startIdle();
	}

	// overloaded methods with less parameters
	// TODO: Turn this function inside out so we don't need 12 flavors
	private String getFromFile(String text, int type) {
		return getFromFile(text, null, null, 0, type, null);
	}

	private String getFromFile(String text, String player, int type) {
		return getFromFile(text, player, null, 0, type, null);
	}

	private String getFromFile(String text, int time, int type) {
		return getFromFile(text, null, null, time, type, null);
	}

	/*
	 * private String getFromFile(String text, String player, int time, int type) { return getFromFile(text, player, null,
	 * time, type, null); }
	 * 
	 * private String getFromFile(String text, int type, String role) { return getFromFile(text, null, null, 0, type,
	 * role); }
	 * 
	 * private String getFromFile(String text, String player, int type, String role) { return getFromFile(text, player,
	 * null, 0, type, role); }
	 * 
	 * private String getFromFile(String text, int time, int type, String role) { return getFromFile(text, null, null,
	 * time, type, role); }
	 */
	private String getFromFile(String text, String player, String player2, int type) {
		return getFromFile(text, player, player2, 0, type, null);
	}

	/*
	 * private String getFromFile(String text, String player, String player2, int time, int type) { return
	 * getFromFile(text, player, player2, time, type, null); }
	 * 
	 * private String getFromFile(String text, String player, String player2, int type, String role) { return
	 * getFromFile(text, player, player2, 0, type, role); }
	 */
	private String getFromFile(String text, String player, int time, int type, String role) {
		return getFromFile(text, player, null, time, type, role);
	}

	// the ACTUAL method
	protected String getFromFile(String text, String player, String player2, int time, int type, String role) {
		BufferedReader buff;

		try {
			buff = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + gameFile)));
			String lineRead = "";

			while (!lineRead.equals(text)) {
				lineRead = buff.readLine();
			}

			Vector<String> texts = new Vector<String>(1, 1);
			lineRead = buff.readLine();

			while (!lineRead.equals("")) {
				texts.add(lineRead);
				lineRead = buff.readLine();
			}

			buff.close();

			int rand = (int) (Math.random() * texts.size());
			String toSend = texts.get(rand);

			if (texts.size() > 0) {
				switch (type) {
				case NOTICE: // no colour formatting
					toSend = toSend.replaceAll("PLAYER2", player2);
					toSend = toSend.replaceAll("PLAYER", player);
					toSend = toSend.replaceAll("TIME", "" + time);
					toSend = toSend.replaceAll("ISAWOLF?", role);

					toSend = getWolfFromFile(toSend);
					toSend = toSend.replaceAll("BOTNAME", this.getNick());
					toSend = toSend.replaceAll("ROLE", role);

					// for one of the Bored texts
					java.text.DecimalFormat two = new java.text.DecimalFormat("00");

					toSend = toSend.replaceAll(
							"RANDDUR",
							((int) (Math.random() * 2)) + " days, " + two.format((int) (Math.random() * 24)) + ":"
									+ two.format((int) (Math.random() * 61)) + ":" + two.format((int) (Math.random() * 61)));
					toSend = toSend.replaceAll("RANDNUM", "" + ((int) (Math.random() * 50) + 1));

					return toSend;
					// break;

				case NARRATION: // blue colour formatting
					toSend = toSend.replaceAll("PLAYER2", Colors.DARK_BLUE + Colors.BOLD + player2 + Colors.NORMAL
							+ Colors.DARK_BLUE);
					toSend = toSend.replaceAll("PLAYER", Colors.DARK_BLUE + Colors.BOLD + player + Colors.NORMAL
							+ Colors.DARK_BLUE);
					toSend = toSend.replaceAll("TIME", Colors.DARK_BLUE + Colors.BOLD + time + Colors.NORMAL + Colors.DARK_BLUE);
					toSend = getWolfFromFile(toSend);
					toSend = toSend.replaceAll("BOTNAME", this.getNick());
					toSend = toSend.replaceAll("ROLE", role);
					return Colors.DARK_BLUE + toSend;
					// break;

				case GAME: // red colour formatting
					toSend = toSend.replaceAll("PLAYER2", Colors.BROWN + Colors.UNDERLINE + player2 + Colors.NORMAL + Colors.RED);
					toSend = toSend.replaceAll("PLAYER", Colors.BROWN + Colors.UNDERLINE + player + Colors.NORMAL + Colors.RED);
					toSend = toSend.replaceAll("TIME", Colors.BROWN + Colors.UNDERLINE + time + Colors.NORMAL + Colors.RED);
					toSend = getWolfFromFile(toSend);
					toSend = toSend.replaceAll("BOTNAME", this.getNick());
					toSend = toSend.replaceAll("ROLE", role);
					return Colors.RED + toSend;
					// break;

				case CONTROL: // Green colour formatting
					toSend = toSend.replaceAll("PLAYER2", Colors.DARK_GREEN + Colors.UNDERLINE + player2 + Colors.NORMAL
							+ Colors.DARK_GREEN);
					toSend = toSend.replaceAll("PLAYER", Colors.DARK_GREEN + Colors.UNDERLINE + player + Colors.NORMAL
							+ Colors.DARK_GREEN);
					toSend = toSend.replaceAll("TIME", Colors.DARK_GREEN + Colors.UNDERLINE + time + Colors.NORMAL
							+ Colors.DARK_GREEN);
					toSend = getWolfFromFile(toSend);
					toSend = toSend.replaceAll("BOTNAME", this.getNick());
					toSend = toSend.replaceAll("ROLE", role);
					return Colors.DARK_GREEN + toSend;
					// break;

				case NONE:
					return toSend;

				default:
					throw new UnsupportedOperationException();
				}
			}
		} catch (Exception x) {
			x.printStackTrace();
		}

		return null; // only reachable trough exception
	}

	private String getWolfFromFile(String toSend) {
		if (players2.numWolves() != 0) {
			return toSend.replaceAll("WOLF",
					(players2.numWolves() == 1 ? getFromFile("1-WOLF", NONE) : getFromFile("MANY-WOLVES", NONE)));
		}
		return toSend;
	}

	@Override
	protected void onPrivateMessage(String aSender, String login, String hostname, String message) {
		aSender = message.substring(0, message.indexOf(" "));
		message = message.substring(message.indexOf(" ") + 1);
		if (status != GameStatus.IDLE) // commands only work if the game is on
		{
			int sender = players2.getPlayerNumber(aSender);
			int target = players2.getPlayerNumber(message.substring(message.indexOf(" ") + 1, message.length()).trim());

			if (message.toLowerCase().equalsIgnoreCase("join")) // join the game
			{
				joinGame(aSender);
				return;
			}

			if (sender != -1) {
				if (status == GameStatus.VOTE && message.toLowerCase().startsWith("vote"))
				// commands for when it's vote time && vote to lynch someone.
				{
					doVote(sender, target);
				} else if (status == GameStatus.NIGHT) // commands for the night
				{
					if (message.toLowerCase().startsWith("kill"))
					// only wolves can kill someone. They may change their minds if they
					// wish to.
					{
						if (players2.isWolf(sender)) {
							if (target == -1) {
								this.sendNotice(sender, "Please choose a valid player.");
								return;
							}
							if (players2.isDead(target)) {
								this.sendNotice(sender, "That person is already dead.");
								return;
							}
							if (target == sender) {
								this.sendNotice(sender, "You cannot eat yourself!");
								return;
							}
							if (players2.isWolf(target)) {
								this.sendNotice(sender, "You cannot eat your soulmate!");
								return;
							}
							wolfVictim2.addVote(sender, target);

							if (players2.numWolves() == 1) {
								this.sendNotice(sender, getFromFile("WOLF-CHOICE", players2.get(target), NOTICE));
							} else {
								this.sendNotice(sender, getFromFile("WOLVES-CHOICE", players2.get(target), NOTICE));

								for (int i = 0; i < players2.numWolves(); i++) {
									if (!(players2.getWolve(i) == sender))
										this.sendNotice(players2.getWolve(i),
												getFromFile("WOLVES-CHOICE-OTHER", players2.get(sender), players2.get(target), NOTICE));
								}
							}

						} else
							this.sendNotice(sender, getFromFile("NOT-WOLF", NOTICE));
					}
					if (message.toLowerCase().startsWith("see")) // only the seer may
																												// watch over someone
					{
						if (!players2.isSeer(sender)) {
							this.sendNotice(sender, getFromFile("NOT-SEER", NOTICE));
							return;
						}
						if (players2.isDead(sender)) {
							this.sendNotice(sender, getFromFile("SEER-DEAD", NOTICE));
							return;
						}
						if (target != -1) {
							if (sender == target) {
								this.sendNotice(sender, "You already know that you are human!");
								return;
							}
							toSee = target;
							this.sendNotice(sender, getFromFile("WILL-SEE", players2.get(toSee), NOTICE));
						}
					}
				}
				if (message.toLowerCase().equalsIgnoreCase("alive")) {
					String names = "The players left alive are: ";

					for (int i = 0; i < players2.numPlayers(); i++) {
						if (!players2.isDead(i) && players2.get(i) != null)
							names += players2.get(i) + " ";
					}

					this.sendNotice(sender, names);
				}
				if (message.toLowerCase().equalsIgnoreCase("role")) {
					if (status != GameStatus.PRE) {
						if (players2.isWolf(sender)) {
							if (players2.numWolves() == 1)
								this.sendNotice(sender, getFromFile("W-ROLE", NOTICE));
							else {
								this.sendNotice(sender, getFromFile("WS-ROLE", players2.getOtherWolve(sender), 0, NOTICE, null));
							}
						} else if (players2.isSeer(sender)) {
							this.sendNotice(sender, getFromFile("S-ROLE", NOTICE));
						}
					} else {
						this.sendNotice(sender, getFromFile("V-ROLE", NOTICE));
					}
				}
			}
		}
	}

	private void doVote(int sender, int target) {
		if (players2.isDead(sender)) {
			this.sendNotice(sender, "You are dead. You cannot vote.");
			return;
		}

		if (!votes2.hasVoted(sender)) {
			try {
				if (target != -1) {
					if (players2.isDead(target)) {
						this.sendNotice(sender, "Your choice is already dead.");
						return;
					}

					votes2.addVote(sender, target);

					this.sendMessage(gameChan, getFromFile("HAS-VOTED", players2.get(sender), players2.get(target), NARRATION));

				} else
					this.sendNotice(sender, "Your choice is not playing in the current game. Please select someone else.");
			} catch (Exception x) {
				this.sendNotice(sender, "Please vote in the correct format: /msg " + this.getName() + " vote <player>");
				x.printStackTrace();
			}
		} else
			this.sendNotice(sender, "You have already voted. You may not vote again until tomorrow.");

	}

	private void sendNotice2(String sender, String notice2) {
		sendNotice("#werewolf2", sender + ": " + notice2);
	}

	private void sendNotice(int sender, String notice2) {
		sendNotice2(players2.get(sender), notice2);
	}

	private void joinGame(String sender) {
		if (status != GameStatus.PRE && status != GameStatus.IDLE) {
			// if the game is already running, dont add anyone else to either list
			this.sendNotice2(sender, "The game is currently underway. Please wait for "
					+ "the current game to finish before trying again.");
			return;
		}
		// TODO: uncomment
		// if (!isInChannel(sender)) {
		// return;
		// }
		if (players2.isAdded(sender)) // has the player already joined?
		{
			this.sendNotice2(sender, "You are already on the player list. Please wait for the next game to join again.");
			return;
		}
		if (players2.numPlayers() < MAXPLAYERS) {
			// if there are less than MAXPLAYERS player joined
			if (players2.addPlayer(sender)) // add another one
			{
				this.setMode(gameChan, "+v " + sender);
				this.sendMessage(gameChan, getFromFile("JOIN", sender, NARRATION));
				this.sendNotice2(sender, getFromFile("ADDED", NOTICE));
			} else {
				// let the user know the adding failed, so he can try again
				this.sendNotice2(sender, "Could not add you to player list. Please try again.");
			}
		} else {
			// if play list is full, add to priority list
			if (players2.numPriority() < MAXPLAYERS) {
				if (players2.addPriority(sender)) {
					this.sendNotice2(sender, "Sorry, the maximum players has been reached. You have been placed in the "
							+ "priority list for the next game.");
				} else {
					this.sendNotice2(sender, "Could not add you to priority list. Please try again.");
				}
			} else {
				// if both lists are full, let the user know to wait for the current
				// game to end
				this.sendNotice2(sender, "Sorry, both player and priority lists are full. Please wait for the "
						+ "the current game to finish before trying again.");
			}
		}
	}

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		if (message.toLowerCase().startsWith("!quit")) {
			if (isSenderOp(sender)) {
				if (status != GameStatus.IDLE)
					doVoice(false);
				this.setMode(gameChan, "-m");
				this.partChannel(gameChan, "Werewolf Game Bot created by LLamaBoy."
						+ " Texts by Darkshine. Based on the PircBot at http://www.jibble.org/");
				this.sendMessage(sender, "Now quitting...");
				this.quitServer();
				System.out.println("Normal shutdown complete");
				for (int i = 0; i < 4; i++) {
					System.out.println();
					System.err.println();
				}
				System.out.close();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ix) {
					ix.printStackTrace();
				}
				System.exit(0);
			}
		} else if (status == GameStatus.IDLE) {
			if (message.startsWith("!startrek")) // Easter egg suggested by Deepsmeg.
			{
				gameFile = "startrek.txt";
			}
			if (message.startsWith("!start")) // initiates a game.
			{
				startGame(sender);

				// if (players2.addPlayer(sender)) {
				// this.setMode(gameChan, "+v " + sender);
				// this.sendNotice2(sender, getFromFile("ADDED", null, 0, NOTICE,
				// null));
				// } else
				// this.sendNotice2(sender,
				// "Could not add you to player list. Please try again." + " (/msg "
				// + this.getName() + " join.");
			} else if (message.startsWith("!daytime ")) // alter the duration of the
																									// day
			{
				if (isSenderOp(sender)) {
					try {
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if (time > 0) {
							dayTime = time;
							this.sendMessage(gameChan, this.getFromFile("DAYCHANGE", time, CONTROL));
							// "Duration of the day now set to " + Colors.DARK_GREEN +
							// Colors.UNDERLINE + dayTime + Colors.NORMAL + " seconds");
						} else
							throw new Exception();
					} catch (Exception x) {
						this.sendMessage(gameChan,
								"Please provide a valid value for the daytime length (!daytime <TIME IN SECONDS>)");
					}
				}
			} else if (message.startsWith("!nighttime ")) // alter the duration of the
																										// night
			{
				if (isSenderOp(sender)) {
					try {
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if (time > 0) {
							nightTime = time;
							this.sendMessage(gameChan, this.getFromFile("NIGHTCHANGE", time, CONTROL));
						} else
							throw new Exception();
					} catch (Exception x) {
						this.sendMessage(gameChan,
								"Please provide a valid value for the night time length (!nighttime <TIME IN SECONDS>)");
					}
				}
			} else if (message.startsWith("!votetime ")) // alter the time for a vote
			{
				if (isSenderOp(sender)) {
					try {
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if (time > 0) {
							voteTime = time;
							this.sendMessage(gameChan, this.getFromFile("VOTECHANGE", time, CONTROL));
						} else
							throw new Exception();
					} catch (Exception x) {
						this.sendMessage(gameChan,
								"Please provide a valid value for the Lynch Vote time length (!votetime <TIME IN SECONDS>)");
					}
				}
			} else if (message.startsWith("!tie ")) // tie option
			{
				if (isSenderOp(sender)) {
					try {
						String tie = message.substring(message.indexOf(" ") + 1, message.length());
						if (tie.equalsIgnoreCase("on")) {
							tieGame = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote tie activated");
						} else if (tie.equalsIgnoreCase("off")) {
							tieGame = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote tie deactivated");
						} else
							throw new Exception();
					} catch (Exception x) {
						this.sendMessage(gameChan, "Please provide a valid value for the vote tie condition (!tie ON/OFF)");
					}
				}
			} else if (message.startsWith("!shush")) // stop idle barks
			{
				if (isSenderOp(sender)) {
					if (doBarks) {
						doBarks = false;
						idleTimer.cancel();
						idleTimer = null;
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I won't speak any more.");
					} else
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I'm already silent. Moron.");
				}
			} else if (message.startsWith("!speak")) // enable idle barks
			{
				if (isSenderOp(sender)) {
					if (!doBarks) {
						doBarks = true;
						startIdle();
						this.sendMessage(gameChan, Colors.DARK_GREEN + "Speakage: on.");
					} else
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I'm already speaking and stuff. kthx");
				}
			} else if (message.startsWith("!daytime"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Day length is " + dayTime + " seconds.");
			else if (message.startsWith("!nighttime"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Night length is " + nightTime + " seconds.");
			else if (message.startsWith("!votetime"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch Vote length is " + voteTime + " seconds.");
			else if (message.startsWith("!tie"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch vote tie is " + (tieGame ? "on." : "off."));
		} else if (message.indexOf("it") != -1 && message.indexOf(this.getNick()) != -1 && (status == GameStatus.DAY)
				&& message.indexOf("it") < message.indexOf(this.getNick()))
		// when it looks like the bot is accused, send a reply
		{
			this.sendMessage(gameChan, "Hey, screw you, " + sender + "! I didn't kill anyone!");
		} else {
			if (status != GameStatus.IDLE) {
				if (status != GameStatus.PRE) {
					for (int i = 0; i < players2.numPlayers(); i++) {
						if (players2.isDead(i) && sender.equals(players2.get(i)))
							this.deVoice(gameChan, sender);
					}

					// Not Working Kick the seer if he quotes the SEER-SEE msg.
					// String check = getFromFile("SEER-SEE", "QZ", 0, 0);
					// int ind = message.indexOf(check.substring(0, check.indexOf("QZ") -
					// 2));
					//
					// if(ind > 0)
					// {
					// if(message.indexOf(this.getName()) > 0)
					// {
					// if(ind > message.indexOf(this.getName())) //check if the player
					// fabricated a quote to incriminate the bot
					// :)
					// this.kick(gameChan, sender,
					// getFromFile("CHEAT-KICK", sender, 0, NOTICE));
					// }
					// else
					// this.kick(gameChan, sender,
					// getFromFile("CHEAT-KICK", sender, 0, NOTICE));
					// }
				}
			}
		}
	}

	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		if (status != GameStatus.IDLE) {
			if (status != GameStatus.PRE) {
				if (players2.isDead(players2.getPlayerNumber(sender)))
					this.setMode(gameChan, "-v " + sender);
			}
		}
	}

	protected boolean isSenderOp(String nick) // to check if the sender of a
																						// message is op, necessary for some
																						// options
	{
		User users[] = this.getUsers(gameChan);

		for (int i = 0; i < users.length; i++) {
			if (users[i].getNick().equals(nick))
				return users[i].isOp();
		}
		return false;
	}

	// if the player changed their nick and they're in the game, changed the
	// listed name
	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
		if (players2.changeNick(oldNick, newNick)) {
			if (players2.isPlaying(oldNick)) {
				if (!players2.isDead(oldNick)) {
					this.sendMessage(gameChan, Colors.DARK_GREEN + oldNick + " has changed nick to " + newNick
							+ "; Player list updated.");
				}
			} else if (players2.isAdded(oldNick)) {
				this.sendMessage(gameChan, Colors.DARK_GREEN + newNick + " has changed nick; Priority list updated.");
			}
		}
	}

	// if a player leaves while te game is one, remove him from the player list
	// and if there is a priority list, add the first person from that in his
	// place.
	@Override
	protected void onPart(String channel, String aSender, String login, String hostname) {
		if (status != GameStatus.IDLE) {
			int sender = players2.getPlayerNumber(aSender);
			if (sender != -1) {
				if (!players2.isDead(sender) && players2.hasPriority()) {
					players2.replace(sender);
					String newPlayer = players2.get(sender);

					this.sendNotice2(newPlayer, getFromFile("FLEE-PRIORITY-NOTICE", aSender, NOTICE));
					this.sendMessage(gameChan, getFromFile("FLEE-PRIORITY", aSender, newPlayer, CONTROL));

					if (status == GameStatus.DAY || status == GameStatus.VOTE) {
						this.voice(gameChan, newPlayer);
					}

					if (players2.isSeer(sender)) {
						this.sendNotice2(newPlayer, getFromFile("S-ROLE", NOTICE));
					} else if (players2.isWolf(sender)) {
						if (players2.numWolves() == 1) {
							this.sendNotice2(newPlayer, getFromFile("W-ROLE", NOTICE));
						} else {
							for (int j = 0; j < players2.numWolves(); j++) {
								String wolve = players2.getWolveName(j);
								this.sendNotice2(wolve, getFromFile("WS-ROLE", wolve, NOTICE));
							}
						}
					} else {
						this.sendNotice2(newPlayer, getFromFile("V-ROLE", NOTICE));
					}
				} else if (status == GameStatus.PRE) {
					players2.remove(sender);
					this.sendMessage(gameChan, getFromFile("FLEE", players2.get(sender), 0, NARRATION, null));
				} else {
					if (!players2.isDead(sender)) {
						if (players2.isWolf(sender)) {
							this.sendMessage(gameChan, getFromFile("FLEE-WOLF", players2.get(sender), 0, NARRATION, null));
						} else {
							this.sendMessage(gameChan, getFromFile("FLEE-VILLAGER", players2.get(sender), 0, NARRATION, null));
						}

						players2.kill(sender);

						wolfVictim2.removeVote(sender);

						if (checkWin()) {
							gameTimer.cancel();
						}

						if (status == GameStatus.VOTE) {
							votes2.removeVote(sender);
						}
					}
				}
			}
		}
		if (players2.removePriority(aSender)) {
			this.sendMessage(gameChan, Colors.DARK_GREEN + Colors.UNDERLINE + aSender + Colors.NORMAL + Colors.DARK_GREEN
					+ ", a player on the priority list, has left. Removing from list...");
		}
	}

	@Override
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		if (status != GameStatus.IDLE) {
			this.sendMessage(gameChan, "Hey! Can't you see we're trying to play a game here? >:(");
		}
	}

	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		this.onPart(gameChan, sourceNick, sourceLogin, sourceHostname);
	}

	@Override
	protected void onDisconnect() {
		connected = false;
		connectAndJoin();
	}

	protected void connectAndJoin() {
		while (!connected) // keep trying until successfully connected
		{
			try {
				this.setName(name);
				this.connect(network);
				Thread.sleep(1000); // allow it some time before identifiying
				// if (!ns.equalsIgnoreCase("none")) {
				// this.sendMessage(ns, command); // identify with nickname service
				this.identify(command);
				Thread.sleep(2000); // allow the ident to go through before joining
				// }
				this.joinChannel(gameChan);
				this.setMode(gameChan, "-m");
				connected = true;
			} catch (IOException iox) {
				System.err.println("Could not connect/IO");
			} catch (NickAlreadyInUseException niux) {
				System.err.println("Nickname is already in use. Choose another nick");
				System.exit(1);
			} catch (IrcException ircx) {
				System.err.println("Could not connect/IRC");
			} catch (InterruptedException iex) {
				System.err.println("Could not connect/Thread");
			}
		}
	}

	protected void startIdle() {
		if (doBarks) {
			idleTimer = new Timer();
			// trigger a chat every 60-240 mins
			idleTimer.schedule(new WereTask(), ((int) (Math.random() * 7200) + 3600) * 1000);
		}
	}

	@Override
	protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {
		if (recipientNick.equals(this.getName()))
			this.joinChannel(channel);
		else
			this.onPart(gameChan, recipientNick, null, null);
	}

	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		if (!sender.equals(this.getNick())) {
			if (status == GameStatus.PRE)
				this.sendNotice2(sender, getFromFile("GAME-STARTED", null, 0, NOTICE, null));
			else if (status != GameStatus.IDLE)
				this.sendNotice2(sender, getFromFile("GAME-PLAYING", null, 0, NOTICE, null));
		}
	}

	protected void startGame(String sender) {
		if (doBarks)
			idleTimer.cancel(); // don't make comments while the game's on.

		if (players2 == null)
			players2 = new Players();
		else {
			players2.reset();
		}

		players2.start(MAXPLAYERS);
		for (int i = 0; i < players2.numPlayers(); i++) {
			this.voice(gameChan, players2.get(i));
			this.sendMessage(gameChan, getFromFile("JOIN", sender, 0, NARRATION, null));
		}

		status = GameStatus.PRE;
		firstNight = true;
		toSee = -1;

		this.sendMessage(gameChan, getFromFile("STARTGAME", sender, joinTime, NARRATION, null));

		this.sendNotice2(gameChan, getFromFile("STARTGAME-NOTICE", sender, 0, NOTICE, null));

		waitForOutgoingQueueSizeIsZero();

		gameTimer = new Timer();
		gameTimer.schedule(new WereTask(), joinTime * 1000);
	}

	private void playNight() {
		doVoice(false);

		if (firstNight) {
			firstNight = false;
			this.sendMessage(gameChan, getFromFile("FIRSTNIGHT", null, 0, NARRATION, null));
		} else {
			this.sendMessage(gameChan, Colors.DARK_BLUE + getFromFile("NIGHTTIME", null, 0, NARRATION, null));
		}
		if (players2.numWolves() == 1) {
			this.sendMessage(gameChan, getFromFile("WOLF-INSTRUCTIONS", null, nightTime, GAME, null));
		} else {
			this.sendMessage(gameChan, getFromFile("WOLVES-INSTRUCTIONS", null, nightTime, GAME, null));
		}

		if (!players2.isSeerDead()) {
			this.sendMessage(gameChan, getFromFile("SEER-INSTRUCTIONS", null, nightTime, GAME, null));
		}

		waitForOutgoingQueueSizeIsZero();

		gameTimer.schedule(new WereTask(), nightTime * 1000);
	}

	/**
   * 
   */
	private void waitForOutgoingQueueSizeIsZero() {
		while (this.getOutgoingQueueSize() != 0)
			Thread.yield();
	}

	private void playDay() {
		String role;
		if (toSee != -1) {
			if (players2.isSeerDead()) {
				this.sendNotice(players2.getSeer(), getFromFile("SEER-SEE-KILLED", players2.get(toSee), 0, NOTICE, null));
			} else {
				if (players2.isDead(toSee)) {
					this.sendNotice(players2.getSeer(),
							getFromFile("SEER-SEE-TARGET-KILLED", players2.get(toSee), 0, NOTICE, null));
				} else {
					if (players2.isWolf(toSee)) {
						role = getFromFile("ROLE-WOLF", null, 0, NOTICE, null);
					} else {
						role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE, null);
					}
					this.sendNotice(players2.getSeer(), getFromFile("SEER-SEE", players2.get(toSee), toSee, NOTICE, role));
				}
			}
		}

		doVoice(true);
		this.sendMessage(gameChan, getFromFile("DAYTIME", null, dayTime, GAME, null));
		waitForOutgoingQueueSizeIsZero();

		gameTimer.schedule(new WereTask(), dayTime * 1000);
	}

	private void playVote() {
		Vector<Integer> hasNotVoted = votes2.notVoted(2);
		for (Integer player : hasNotVoted) {
			if (!players2.isDead(player)) {
				players2.kill(player);

				this.sendMessage(gameChan, getFromFile("NOT-VOTED", players2.get(player), 0, NARRATION, null));
				this.sendNotice2(players2.get(player), getFromFile("NOT-VOTED-NOTICE", null, 0, NOTICE, null));

				this.setMode(gameChan, "-v " + players2.get(player));

			}
		}

		if (checkWin())
			return;

		this.sendMessage(gameChan, getFromFile("VOTETIME", null, voteTime, GAME, null));

		waitForOutgoingQueueSizeIsZero();

		gameTimer.schedule(new WereTask(), voteTime * 1000);
	}

	// method to batch voice/devoice all the users on the playerlist.
	protected void doVoice(boolean on) {
		String nicks = "", modes = "";
		int count = 0;

		if (on)
			modes = "+";
		else
			modes = "-";

		for (int i = 0; i < players2.numPlayers(); i++) {
			try {
				if (!players2.isDead(i) && players2.get(i) != null) {
					nicks += players2.get(i) + " ";
					modes += "v";
					count++;
					if (count % 4 == 0) {
						this.setMode(gameChan, modes + " " + nicks);
						nicks = "";

						if (on)
							modes = "+";
						else
							modes = "-";

						count = 0;
					}
				}
			} catch (NullPointerException npx) {
				System.out.println("Could not devoice, no dead array");
			}
		}

		this.setMode(gameChan, modes + " " + nicks); // mode the stragglers that
																									// dont make a full 4
	}

	/**
	 * Check if the name is in the game channel at the time.
	 * 
	 * @param aName
	 *          the name
	 * @return true if in channel
	 */
	protected boolean isInChannel(String aName) {
		User[] users = this.getUsers(gameChan);

		for (int i = 0; i < users.length; i++) {
			if (users[i].getNick().equals(aName))
				return true;
		}

		return false;
	}

	private int tallyVotes() {
		this.sendMessage(gameChan, getFromFile("TALLY", null, 0, CONTROL, null));
		Vector<Integer> targets = votes2.getTarget();
		if (targets.size() == 0) {
			this.sendMessage(gameChan, getFromFile("NO-VOTES", null, 0, NARRATION, null));
			return -1;
		} else if (targets.size() == 1) {
			return targets.get(0);
		} else if (tieGame) {
			this.sendMessage(gameChan, getFromFile("TIE", null, 0, CONTROL, null));
			int rand = (int) (Math.random() * targets.size());
			return targets.get(rand);
		} else {
			this.sendMessage(gameChan, Colors.DARK_BLUE + getFromFile("NO-LYNCH", null, 0, NARRATION, null));
			return -1;
		}
	}

	private void doLynch(int guilty) {
		if (guilty == -1) {
			return;
		}

		String guiltyStr = players2.get(guilty);
		players2.kill(guilty);

		if (!isInChannel(guiltyStr)) {
			this.sendMessage(gameChan, getFromFile("LYNCH-LEFT", null, 0, CONTROL, null));
			return;
		}

		String role;
		if (players2.isSeer(guilty)) {
			this.sendMessage(gameChan, getFromFile("SEER-LYNCH", guiltyStr, 0, NARRATION, null));
			role = getFromFile("ROLE-SEER", null, 0, NARRATION, null);
		} else if (players2.isWolf(guilty)) {
			this.sendMessage(gameChan, getFromFile("WOLF-LYNCH", guiltyStr, 0, NARRATION, null));
			role = getFromFile("ROLE-WOLF", null, 0, NARRATION, null);
		} else {
			this.sendMessage(gameChan, getFromFile("VILLAGER-LYNCH", guiltyStr, 0, NARRATION, null));
			role = getFromFile("ROLE-VILLAGER", null, 0, NARRATION, null);
		}
		this.sendMessage(gameChan, getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION, role));

		if (players2.isSeer(guilty) || players2.isWolf(guilty)) {
			this.deVoice(gameChan, guiltyStr);
		} else {
			this.sendNotice2(guiltyStr, getFromFile("DYING-BREATH", null, 0, NOTICE, null));
		}
	}

	protected void wolfKill() {
		Vector<Integer> targets = wolfVictim2.getTarget();
		if (targets.size() == 0) {
			this.sendMessage(gameChan, getFromFile("NO-KILL", null, 0, NARRATION, null));
			return;
		}
		int target;
		if (targets.size() == 1) {
			target = targets.get(0);
		} else {
			target = targets.get((int) Math.random() * targets.size());
		}

		players2.kill(target); // make the player dead
		String role;
		if (players2.isSeer(target)) {
			this.sendMessage(gameChan, getFromFile("SEER-KILL", players2.get(target), 0, NARRATION, null));
			role = getFromFile("ROLE-SEER", null, 0, NOTICE, null);
		} else {
			this.sendMessage(gameChan, getFromFile("VILLAGER-KILL", players2.get(target), 0, NARRATION, null));
			role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE, null);
		}

		this.sendMessage(gameChan, Colors.DARK_BLUE + getFromFile("IS-KILLED", players2.get(target), 0, NARRATION, role));

		this.setMode(gameChan, "-v " + players2.get(target));
	}

	protected void setRoles() {

		if (players2.numPlayers() < TWOWOLVES) {
			// if there are lese than TWOWOLVES players, only one wolf
			players2.setRoles(1, 1);
		} else // otherwise, 2 wolves, and they know each other
		{
			players2.setRoles(2, 1);
			this.sendMessage(gameChan, getFromFile("TWOWOLVES", null, 0, CONTROL, null));
		}

		for (int i = 0; i < players2.numPlayers(); i++) {
			if (players2.isSeer(i)) {
				this.sendNotice2(players2.get(i), getFromFile("SEER-ROLE", null, 0, NOTICE, null));
			} else if (players2.isWolf(i)) {
				if (players2.numWolves() == 1) {
					this.sendNotice(i, getFromFile("WOLF-ROLE", null, 0, NOTICE, null));
				} else {
					this.sendNotice(i, getFromFile("WOLVES-ROLE", players2.getOtherWolve(i), 0, NOTICE, null));
				}
			} else {
				this.sendNotice(i, getFromFile("VILLAGER-ROLE", null, 0, NOTICE, null));
			}
		}
	}

	protected boolean checkWin() {
		if (players2.numWolves() == 0) // humans win
		{
			this.sendMessage(gameChan, getFromFile("VILLAGERS-WIN", null, 0, NARRATION, null));
			this.sendMessage(gameChan, getFromFile("CONGR-VILL", null, 0, NARRATION, null));
		} else if (players2.numWolves() * 2 >= players2.numAlive()) // wolves win
		{
			if (players2.numWolves() < 1) {
				this.sendMessage(gameChan, getFromFile("WOLF-WIN", players2.getWolveName(0), 0, NARRATION, null));
				this.sendMessage(gameChan, getFromFile("CONGR-WOLF", players2.getWolveName(0), 0, NARRATION, null));
			} else {
				this.sendMessage(gameChan, Colors.DARK_BLUE + getFromFile("WOLVES-WIN", null, 0, NARRATION, null));
				this.sendMessage(gameChan, getFromFile("CONGR-WOLVES", null, 0, NARRATION, null));
				this.sendMessage(gameChan, getFromFile("WOLVES-WERE", null, 0, CONTROL, null) + players2.getWolves());
			}
		} else {
			// No-one wins
			return false;
		}
		status = GameStatus.IDLE;
		doVoice(false);
		this.setMode(gameChan, "-m");
		gameTimer.cancel(); // stop the game timer, since someone won.
		gameTimer = null;
		// reset the game file to default
		gameFile = "wolfgame.txt";
		// start the idling again
		startIdle();
		return true;
	}

	public static void main(String args[]) {
		new Werewolf();
	}

	private class WereTask extends TimerTask {
		public WereTask() {
		}

		@Override
		public void run() {
			timed();
		}
	}

	public void timed() {
		if (status == GameStatus.IDLE) {
			User[] users = getUsers(gameChan);
			String rand;
			do {
				rand = users[((int) (Math.random() * users.length))].getNick();
			} while (rand.equals(getNick()));

			String msg = getFromFile("BORED", rand, 0, NOTICE, null);
			if (msg != null)
				sendMessage(gameChan, msg);
			startIdle();
		} else if (status == GameStatus.PRE) // the start of the game
		{
			waitForOutgoingQueueSizeIsZero();
			sendMessage(gameChan, Colors.DARK_GREEN + "Joining ends.");

			if (players2.numPlayers() < MINPLAYERS) {
				// Not enough players
				setMode(gameChan, "-m");
				doVoice(false);

				sendMessage(gameChan, getFromFile("NOT-ENOUGH", null, 0, CONTROL, null));
				status = GameStatus.IDLE;
				gameFile = "wolfgame.txt"; // reset the game file
				startIdle();

				return;
			}

			this.setMode(gameChan, "+m");

			status = GameStatus.NIGHT; // stop the joining

			votes2 = new Votes(players2.numPlayers());
			wolfVictim2 = new Votes(players2.numPlayers());

			setRoles();

			if (players2.numPlayers() == 5) { // if there are 5 players, day comes
				// first
				status = GameStatus.DAY;
				// once everything is set up, start the game proper
				playDay();
			} else {
				playNight();
			}

			waitForOutgoingQueueSizeIsZero();

		} else if (status == GameStatus.DAY) // the day ends
		{
			status = GameStatus.VOTE;

			playVote();

		} else if (status == GameStatus.VOTE) // voting time begins
		{
			status = GameStatus.NIGHT;

			doLynch(tallyVotes());

			votes2.reset();

			toSee = -1;

			if (checkWin()) {
				return;
			}
			playNight();
		} else if (status == GameStatus.NIGHT) // the night ends
		{
			wolfKill();

			wolfVictim2 = new Votes(players2.numPlayers());

			status = GameStatus.DAY;
			if (checkWin()) {
				return;
			}
			playDay();
		}

	}
}
