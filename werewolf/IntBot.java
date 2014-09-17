package werewolf;

public interface IntBot {
	void onPrivateMessage(String aSender, String login, String hostname, String message);
	void onMessage(String channel, String sender, String login, String hostname, String message);
	void onAction(String sender, String login, String hostname, String target, String action);
	void onNickChange(String oldNick, String login, String hostname, String newNick);
	void onPart(String channel, String aSender, String login, String hostname);
	void onTopic(String channel, String topic, String setBy, long date, boolean changed);
	void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason);
	void onDisconnect();
	void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason);
	void onJoin(String channel, String sender, String login, String hostname);
}
