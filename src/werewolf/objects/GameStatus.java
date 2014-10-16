package werewolf.objects;

public enum GameStatus {
  /**
   * Game not started
   */
  IDLE,
  /**
   * Game in join mode
   */
  PRE,
  /**
   * Daytime
   */
  DAY,
  /**
   * Vote time
   */
  VOTE,
  /**
   * Night time
   */
  NIGHT
}
