package test;

import java.io.IOException;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.internal.matchers.Any;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.IsAnything;
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
import static org.powermock.api.easymock.PowerMock.*;
import static org.easymock.EasyMock.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Werewolf.class, PircBot.class, ImplBot.class, Time.class, Players.class })
public class WolfTest {

	@Mock
	ImplBot bot = PowerMock.createStrictMock(ImplBot.class);
	private Capture<TimerTask> wereTask;
	private Timer timer;
	private Werewolf werewolf;

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
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendMessage(
				"#werewolf",
				"0202KZK02 has started a game. Everyone else has 029002 seconds to join in the mob. '/msg Kalbot join' to join the game.");
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice("#werewolf", "KZK has started a game!");
		bot.setMode("#werewolf", "+v KZK");
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice("KZK", "Added to the game. Your role will be given once registration elapses.");
		expect(bot.getOutgoingQueueSize()).andReturn(0);
		expectNew(Timer.class).andReturn(timer);
		timer.schedule(EasyMock.capture(wereTask), EasyMock.eq(90L * 1000L));
	}

	private void doEndJoinNotEnough() {
		doEndJoinNotEnoughOne();
		bot.setMode("#werewolf", "-v KZK");
		doEndJoinNotEnoughTwo();
	}

	private void doEndJoinNotEnoughTwo() {
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendMessage("#werewolf", "03Sorry, not enough people to make a valid mob.");
	}

	private void doEndJoinNotEnoughOne() {
		expect(bot.getOutgoingQueueSize()).andReturn(0);
		bot.sendMessage("#werewolf", "03Joining ends.");
		bot.setMode("#werewolf", "-m");
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
		expect(bot.getNick()).andReturn("Kalbot");

		bot.sendMessage(
				"#werewolf",
				"02Night descends on the sleepy village, and a full moon rises. Unknown to the villagers, tucked up in their warm beds, the early demise of one of their number is being plotted.");
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendMessage(
				"#werewolf",
				"04Werewolf, you have 056004 seconds to decide who to attack. To make your final decision type '/msg Kalbot kill <player>'");
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendMessage(
				"#werewolf",
				"04Seer, you have 056004 seconds to PM one name to Kalbot and discover their true intentions. To enquire with the spirits type '/msg Kalbot see <player>'");

		expect(bot.getOutgoingQueueSize()).andReturn(0);
		timer.schedule(EasyMock.capture(wereTask), EasyMock.eq(60L * 1000L));
		expect(bot.getOutgoingQueueSize()).andReturn(0);
	}

	private void doEndJoin() {
		expect(bot.getOutgoingQueueSize()).andReturn(0);
		bot.sendMessage("#werewolf", "03Joining ends.");
		bot.setMode("#werewolf", "+m");
	}

	private void doIsWerewolf(String name) {
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice(
				name,
				"You are a prowler of the night, a Werewolf! You must decide your nightly victims. By day you must deceive the villager and attempt to blend in. Keep this information to yourself! Good luck!");
	}

	private void doIsSeer(String name) {
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice(
				name,
				"You are one granted the gift of second sight, a Seer! Each night you may enquire as to the nature of one of your fellow village dwellers, and WereBot will tell you whether or not that person is a Werewolf - a powerful gift indeed! But beware revealing this information to the Werewolf, or face swift retribution!");
	}

	private void doIsVillager(String Name) {
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice(
				Name,
				"You are a peaceful peasant turned vigilante, a Villager! You must root out the Werewolf by casting accusations or protesting innocence at the daily village meeting, and voting who you believe to be untrustworthy during the daily Lynch Vote. Good luck!");
	}

	private void doJoinHunt(String name) {
		bot.setMode("#werewolf", "+v " + name);
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendMessage("#werewolf", "0202" + name + "02 has joined the hunt.");
		expect(bot.getNick()).andReturn("Kalbot");
		bot.sendNotice(name, "Added to the game. Your role will be given once registration elapses.");
	}

	@Test
	public void testNotEnoughPlayers() throws Exception {
		doEndJoinNotEnough();
		// second try
		doStart();
		doEndJoinNotEnough();

		replayAll();
		initOnePlayer();
		wereTask.getValue().run();
		// second try
		werewolf.onMessage("#werewolf", "KZK", "~KZK", "uto-9B284F3C.static.chello.nl", "!start");
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
		werewolf.onPrivateMessage("Luke", "~Luke", "uto-C7487DCD.nycmny.fios.verizon.net", "join");
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

	private void initOnePlayer() {
		werewolf = new Werewolf();
		werewolf.onMessage("#werewolf", "KZK", "~KZK", "uto-9B284F3C.static.chello.nl", "!start");
	}

	private void initGame() {
		initOnePlayer();
		werewolf.onPrivateMessage("Luke", "~Luke", "uto-C7487DCD.nycmny.fios.verizon.net", "join");
		werewolf.onPrivateMessage("Dumnorix", "~Dumnorix", "uto-94ADF0B9.adsl-dyn.isp.belgacom.be", "join");
		werewolf.onPrivateMessage("QuiDaM", "~QuiDaM", "uto-9A6CF2A4.mc.videotron.ca", "join");
		werewolf.onPrivateMessage("Sjet", "~IceChat77", "uto-5B0C17CC.telstraclear.net", "join");
		werewolf.onPrivateMessage("Tox", "~c", "2B532809.35C52439.FBCBA1F7.IP", "join");
		wereTask.getValue().run();
	}

}
