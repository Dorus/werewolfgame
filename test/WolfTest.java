package test;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberMatcher.methods;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

import java.io.IOException;
import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import werewolf.ImplBot;
import werewolf.Werewolf;
import werewolf.objects.Players;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Werewolf.class, PircBot.class, ImplBot.class, Time.class, Players.class })
public class WolfTest {

	@Mock
	ImplBot bot = PowerMock.createStrictMock(ImplBot.class);
	private Capture<TimerTask> wereTask;
	private Timer timer;
	private Werewolf werewolf;
	private Map<String, String[]> players = new HashMap<String, String[]>();

	{
		players.put("KZK", new String[] { "~KZK", "uto-9B284F3C.static.chello.nl" });
		players.put("Luke", new String[] { "~Luke", "uto-C7487DCD.nycmny.fios.verizon.net" });
		players.put("Dumnorix", new String[] { "~Dumnorix", "uto-94ADF0B9.adsl-dyn.isp.belgacom.be" });
		players.put("QuiDaM", new String[] { "~QuiDaM", "uto-9A6CF2A4.mc.videotron.ca" });
		players.put("Sjet", new String[] { "~IceChat77", "uto-5B0C17CC.telstraclear.net" });
		players.put("Tox", new String[] { "~c", "2B532809.35C52439.FBCBA1F7.IP" });
	};

	@Before
	public void setUp() throws Exception {
		suppress(method(PircBot.class, "connect", String.class));
		expectNew(ImplBot.class, isA(Werewolf.class)).andReturn(bot);

		PowerMock.mockStatic(Thread.class, methods(Thread.class, "sleep"));
		Thread.sleep(anyLong());
		EasyMock.expectLastCall().anyTimes();

		PowerMock.mockStatic(Math.class, methods(Math.class, "random"));
		Math.random();
		EasyMock.expectLastCall().andReturn(0.5).anyTimes();

		timer = PowerMock.createMock(Timer.class);
		wereTask = new Capture<TimerTask>();

		doConnect();
		doStart();
	}

	@Test
	public void testNotEnoughPlayers() throws Exception {
		doEndJoinNotEnough();
		doStart();
		doEndJoinNotEnough();
		replayAll();
		initOnePlayer();
		wereTask.getValue().run();
		initDoMsg("KZK", "!start");
		wereTask.getValue().run();
		verifyAll();
	}

	@Test
	public void testTwoPlayers() {
		doJoinHunt("Luke");
		doEndJoinNotEnoughOne();
		bot.setMode("#werewolf", "-vv KZK Luke");
		doEndJoinNotEnoughTwo();
		replayAll();
		initOnePlayer();
		initJoin("Luke");
		wereTask.getValue().run();
		verifyAll();
	}

	@Test
	public void testSixPlayers() {
		doFivePlayers();
		replayAll();
		initGame();
		verifyAll();
	}

	@Test
	public void testNightOne() throws Exception {
		doFivePlayers();
		doSendNotice("Dumnorix", "You will see the identity of KZK upon the dawning of tomorrow.");
		doSendNotice("QuiDaM", "You have chosen Tox to feast on tonight.");
		doSendMessage("02When the villagers gather at the village center, one comes running from the hanging tree, screaming at others to follow. When they arrive at the hanging ree, a gentle creaking echoes through the air as the body of 02Tox02 swings gently in the breeze, it's arms ripped off at the shoulders. It appears the attacker was not without a sense of irony...");
		expect(bot.getNick()).andReturn("Kalbot");
		doSendMessage("020202Tox02, the Villager, has been killed!");
		bot.setMode("#werewolf", "-v Tox");
		expect(bot.getNick()).andReturn("Kalbot");
		doSendNotice("Dumnorix", "You find the identity of KZK to be Villager?!");
		bot.setMode("#werewolf", "+vvvv KZK Luke Dumnorix QuiDaM");
		bot.setMode("#werewolf", "+v Sjet");
		doSendMessage(
				"04Villagers, you have 059004 seconds to discuss suspicions, or cast accusations, after which time a lynch vote will be called.");
		doSetTimer(90);
		replayAll();
		initGame();
		initDoPrivMsg("Dumnorix", "see KZK");
		initDoPrivMsg("QuiDaM", "kill Tox");
		wereTask.getValue().run();
		verifyAll();
	}

	private void doSetTimer(long time) {
		expect(bot.getOutgoingQueueSize()).andReturn(0);
		timer.schedule(EasyMock.capture(wereTask), EasyMock.eq(time * 1000));
	}

	private void doSendMessage(String msg) {
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendMessage("#werewolf", msg);
	}

	private void doSendNotice(String name, String msg) {
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice(name, msg);
	}

	private void doConnect() throws NickAlreadyInUseException, IOException, IrcException {
		bot.setLogin_("Werewolf");
		bot.setVersion_("Werewolf Game Bot by LLamaBoy and Darkshine - using pIRC framework from http://www.jible.org");
		bot.setMessageDelay(500);
		EasyMock.expectLastCall().times(2);
		bot.setName_("Kalbot");
		bot.connect("irc.utonet.org");
		bot.identify("password");
		bot.joinChannel("#werewolf");
		bot.setMode("#werewolf", "-m");
	}

	private void doStart() throws Exception {
		doSendMessage(
				"0202KZK02 has started a game. Everyone else has 029002 seconds to join in the mob. '/msg Kalbot join' to join the game.");
		doSendNotice("#werewolf", "KZK has started a game!");
		bot.setMode("#werewolf", "+v KZK");
		doSendNotice("KZK", "Added to the game. Your role will be given once registration elapses.");
		expect(bot.getOutgoingQueueSize()).andReturn(0);
		expectNew(Timer.class).andReturn(timer);
		timer.schedule(EasyMock.capture(wereTask), EasyMock.eq(90L * 1000L));
	}

	private void doEndJoinNotEnough() {
		doEndJoinNotEnoughOne();
		bot.setMode("#werewolf", "-v KZK");
		doEndJoinNotEnoughTwo();
	}

	private void doEndJoinNotEnoughOne() {
		doSendMessage("03Joining ends.");
		bot.setMode("#werewolf", "-m");
	}

	private void doEndJoinNotEnoughTwo() {
		doSendMessage("03Sorry, not enough people to make a valid mob.");
	}

	private void doFivePlayers() {
		doJoinHunt("Luke");
		doJoinHunt("Dumnorix");
		doJoinHunt("QuiDaM");
		doJoinHunt("Sjet");
		doJoinHunt("Tox");
		doEndJoin();

		doIsVillager("KZK");
		doIsVillager("Luke");
		doIsSeer("Dumnorix");
		doIsWerewolf("QuiDaM");
		doIsVillager("Sjet");
		doIsVillager("Tox");

		bot.setMode("#werewolf", "-vvvv KZK Luke Dumnorix QuiDaM");
		bot.setMode("#werewolf", "-vv Sjet Tox");

		doSendMessage(
				"02Night descends on the sleepy village, and a full moon rises. Unknown to the villagers, tucked up in their warm beds, the early demise of one of their number is being plotted.");
		doSendMessage(
				"04Werewolf, you have 056004 seconds to decide who to attack. To make your final decision type '/msg Kalbot kill <player>'");
		doSendMessage(
				"04Seer, you have 056004 seconds to PM one name to Kalbot and discover their true intentions. To enquire with the spirits type '/msg Kalbot see <player>'");

		doSetTimer(60);
		expect(bot.getOutgoingQueueSize()).andReturn(0);
	}

	private void doJoinHunt(String name) {
		bot.setMode("#werewolf", "+v " + name);
		doSendMessage("0202" + name + "02 has joined the hunt.");
		doSendNotice(name, "Added to the game. Your role will be given once registration elapses.");
	}

	private void doEndJoin() {
		doSendMessage("03Joining ends.");
		bot.setMode("#werewolf", "+m");
	}

	private void doIsWerewolf(String name) {
		doSendNotice(
				name,
				"You are a prowler of the night, a Werewolf! You must decide your nightly victims. By day you must deceive the villager and attempt to blend in. Keep this information to yourself! Good luck!");
	}

	private void doIsSeer(String name) {
		doSendNotice(
				name,
				"You are one granted the gift of second sight, a Seer! Each night you may enquire as to the nature of one of your fellow village dwellers, and WereBot will tell you whether or not that person is a Werewolf - a powerful gift indeed! But beware revealing this information to the Werewolf, or face swift retribution!");
	}

	private void doIsVillager(String Name) {
		doSendNotice(
				Name,
				"You are a peaceful peasant turned vigilante, a Villager! You must root out the Werewolf by casting accusations or protesting innocence at the daily village meeting, and voting who you believe to be untrustworthy during the daily Lynch Vote. Good luck!");
	}

	private void initOnePlayer() {
		werewolf = new Werewolf();
		initDoMsg("KZK", "!start");
	}

	private void initGame() {
		initOnePlayer();
		initJoin("Luke");
		initJoin("Dumnorix");
		initJoin("QuiDaM");
		initJoin("Sjet");
		initJoin("Tox");
		wereTask.getValue().run();
	}

	private void initJoin(String name) {
		initDoPrivMsg(name, "join");
	}

	private void initDoMsg(String name, String command) {
		String[] playerInfo = players.get(name);
		werewolf.onMessage("#werewolf", name, playerInfo[0], playerInfo[1], command);
	}

	private void initDoPrivMsg(String name, String command) {
		String[] playerInfo = players.get(name);
		werewolf.onPrivateMessage(name, playerInfo[0], playerInfo[1], command);
	}

}
