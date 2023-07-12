import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Card class representing a card with color and value
class Card {
    private final String color;
    private final String value;

    public Card(String color, String value) {
        this.color = color;
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return color + " " + value;
    }
}

// SpecialCard class extending the Card class
class SpecialCard extends Card {
    public SpecialCard(String color, String value) {
        super(color, value);
    }
}

// CardFactory interface for creating cards
interface CardFactory {
    Card createCard(String color, String value);
}

// BasicCardFactory class implementing CardFactory
class BasicCardFactory implements CardFactory {
    @Override
    public Card createCard(String color, String value) {
        return new Card(color, value);
    }
}

// SpecialCardFactory class implementing CardFactory
class SpecialCardFactory implements CardFactory {
    @Override
    public Card createCard(String color, String value) {
        return new SpecialCard(color, value);
    }
}

// Deck class representing a deck of cards
class Deck {
    private final List<Card> cards;
    private final CardFactory cardFactory;
    private List<Card> playedCards;

    public Deck(CardFactory cardFactory) {
        this.cards = new ArrayList<>();
        this.cardFactory = cardFactory;
        this.playedCards = new ArrayList<>();
        initializeDeck();
    }

    // Initialize the deck of cards
    private void initializeDeck() {
        String[] colors = {"Red", "Blue", "Green", "Yellow"};
        String[] values = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
        for (String color : colors) {
            for (String value : values) {
                cards.add(cardFactory.createCard(color, value));
            }
        }
    }

    // Shuffle the deck
    public void shuffle() {
        Collections.shuffle(cards);
    }

    // Draw a card from the deck
    public Card drawCard() {
        if (cards.isEmpty()) {
            reshufflePlayedCards();
        }
        return cards.remove(cards.size() - 1);
    }

    // Add a played card to the playedCards list
    public void addPlayedCard(Card card) {
        playedCards.add(card);
    }

    // Reshuffle played cards back into the deck
    private void reshufflePlayedCards() {
        System.out.println("Reshuffling played cards...");
        cards.addAll(playedCards);
        playedCards.clear();
        shuffle();
    }
}

// GameObserver interface for observing game events
interface GameObserver {
    void onCardPlayed(String playerName, Card card);
    void onCardDrawn(String playerName, Card card);
    void onPlayerWins(String playerName);
}

// Player class representing a player in the game
class Player implements GameObserver {
    private final String name;
    private final List<Card> hand;
    private boolean skipped;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.skipped = false;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    // Draw a card and add it to the player's hand
    public void drawCard(Card card) {
        hand.add(card);
    }

    // Play a card from the player's hand
    public Card playCard(int index) {
        return hand.remove(index);
    }

    @Override
    public void onCardPlayed(String playerName, Card card) {
        if (playerName.equals(name)) {
            System.out.println(name + " played " + card);
        }
    }

    @Override
    public void onCardDrawn(String playerName, Card card) {
        if (playerName.equals(name)) {
            System.out.println(name + " draws " + card);
        }
    }

    @Override
    public void onPlayerWins(String playerName) {
        if (playerName.equals(name)) {
            System.out.println(name + " wins!");
        }
    }
}

// Abstract Game class
abstract class Game {
    protected final List<Player> players;
    protected final Deck deck;
    protected Card currentCard;
    protected boolean clockwise;

    public Game(List<Player> players, CardFactory cardFactory) {
        this.players = players;
        this.deck = new Deck(cardFactory);
        this.clockwise = true;
    }

    protected abstract void printGameState();

    protected abstract int getValidCardIndex(Player player);

    protected abstract boolean hasWinner();

    // Notify observers when a card is played
    protected void notifyCardPlayed(String playerName, Card card) {
        for (Player player : players) {
            player.onCardPlayed(playerName, card);
        }
        deck.addPlayedCard(card);
    }

    // Notify observers when a card is drawn
    protected void notifyCardDrawn(String playerName, Card card) {
        for (Player player : players) {
            player.onCardDrawn(playerName, card);
        }
    }

    // Notify observers when a player wins
    protected void notifyPlayerWins(String playerName) {
        for (Player player : players) {
            player.onPlayerWins(playerName);
        }
    }

    // Reverse the turn order
    protected void reverseTurn() {
        clockwise = !clockwise;
    }

    // Get the index of the next player
    protected int getNextPlayerIndex(int currentPlayerIndex) {
        if (clockwise) {
            return (currentPlayerIndex + 1) % players.size();
        } else {
            return (currentPlayerIndex - 1 + players.size()) % players.size();
        }
    }

    // Skip the next player's turn
    protected void skipNextPlayer(int currentPlayerIndex) {
        int nextPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
        players.get(nextPlayerIndex).setSkipped(true);
    }

    // Draw cards for the next player
    protected void drawCards(int currentPlayerIndex, int numCards) {
        int nextPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
        Player nextPlayer = players.get(nextPlayerIndex);
        for (int i = 0; i < numCards; i++) {
            Card card = deck.drawCard();
            nextPlayer.drawCard(card);
            notifyCardDrawn(nextPlayer.getName(), card);
        }
    }

    // Main game loop
    public void play() {
        deck.shuffle();
        dealInitialCards();
        int currentPlayerIndex = 0;
        while (!hasWinner()) {
            printGameState();
            Player currentPlayer = players.get(currentPlayerIndex);
            if (currentPlayer.isSkipped()) {
                currentPlayer.setSkipped(false);
                currentPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
                continue;
            }
            int validCardIndex = getValidCardIndex(currentPlayer);
            if (validCardIndex != -1) {
                Card card = currentPlayer.playCard(validCardIndex);
                currentCard = card;
                notifyCardPlayed(currentPlayer.getName(), card);
                if (card instanceof SpecialCard) {
                    handleSpecialCard((SpecialCard) card, currentPlayerIndex);
                }
                if (currentPlayer.getHand().isEmpty()) {
                    notifyPlayerWins(currentPlayer.getName());
                    break;
                }
            } else {
                Card card = deck.drawCard();
                currentPlayer.drawCard(card);
                notifyCardDrawn(currentPlayer.getName(), card);
                if (card instanceof SpecialCard) {
                    handleSpecialCard((SpecialCard) card, currentPlayerIndex);
                }
            }
            currentPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
        }
    }

    // Deal initial cards to players
    private void dealInitialCards() {
        for (Player player : players) {
            for (int i = 0; i < 7; i++) {
                Card card = deck.drawCard();
                player.drawCard(card);
            }
        }
        currentCard = deck.drawCard();
        notifyCardPlayed("Game", currentCard);
    }

    // Handle special card effects
    private void handleSpecialCard(SpecialCard card, int currentPlayerIndex) {
        if (card.getValue().equals("Skip")) {
            skipNextPlayer(currentPlayerIndex);
        } else if (card.getValue().equals("Reverse")) {
            reverseTurn();
        } else if (card.getValue().equals("Draw Two")) {
            drawCards(currentPlayerIndex, 2);
            skipNextPlayer(currentPlayerIndex);
        }
    }
}

// UNO game implementation
class UnoGame extends Game {
    public UnoGame(List<Player> players, CardFactory cardFactory) {
        super(players, cardFactory);
    }

    @Override
    protected void printGameState() {
        System.out.println("-----------------------------------");
        System.out.println("Current card: " + currentCard);
        System.out.println("Players:");
        for (Player player : players) {
            System.out.println(player.getName() + "'s hand: " + player.getHand());
        }
        System.out.println("-----------------------------------");
    }

    @Override
    protected int getValidCardIndex(Player player) {
        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.getColor().equals(currentCard.getColor()) || card.getValue().equals(currentCard.getValue())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected boolean hasWinner() {
        for (Player player : players) {
            if (player.getHand().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}

public class GameDriver {
    public static void main(String[] args) {
        Player player1 = new Player("player 1");
        Player player2 = new Player("player 2");
        List<Player> players = Arrays.asList(player1, player2);

        CardFactory cardFactory = new BasicCardFactory();
        Game unoGame = new UnoGame(players, cardFactory);
        unoGame.play();
    }
}
