package werewolf;

import org.jibble.pircbot.PircBot;

public class ImplBot extends PircBot {

	IntBot bot;

	public ImplBot(IntBot bot) {
		this.bot = bot;
	}
	
	public void setLogin_(String login) {
		super.setLogin(login);
	}

	public void setVersion_(String version) {
		super.setVersion(version);
	}
	
	@Override
	protected void onPrivateMessage(String aSender, String login, String hostname, String message) {
		bot.onPrivateMessage(aSender, login, hostname, message);
	}
	
	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		bot.onMessage(channel, sender, login, hostname, message);
	}
	
	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		bot.onAction(sender, login, hostname, target, action);
	}
	
	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
		bot.onNickChange(oldNick, login, hostname, newNick);
	}
	
	@Override
	protected void onPart(String channel, String aSender, String login, String hostname){
		bot.onPart(channel, aSender, login, hostname);
	}

	@Override
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		bot.onTopic(channel, topic, setBy, date, changed);
	}
	
	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		bot.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
	}
	
	@Override
	protected void onDisconnect() {
		bot.onDisconnect();
	}

	public void setName_(String name) {
		super.setName(name);
	}
	
	@Override
	protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {
		bot.onKick(channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		bot.onJoin(channel, sender, login, hostname);
	}
}
