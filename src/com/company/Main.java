package com.company;
import java.util.*;

public class Main {

    enum CardType {
        NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR
    }

    enum Color {
        RED, GREEN, BLUE, YELLOW, NONE
    }

    public static class Card {
        private CardType type;
        private Color color;
        private int number;

        public Card(CardType type, Color color, int number) {
            this.type = type;
            this.color = color;
            this.number = number;
        }

        public CardType getType() {
            return type;
        }

        public Color getColor() {
            return color;
        }

        public int getNumber() {
            return number;
        }

        @Override
        public String toString() {
            if (type == CardType.NUMBER) {
                return color + " " + number;
            } else {
                return color + " " + type;
            }
        }
    }

    public static class Player {
        String playerName;
        int playerNumber;
        List<Card> playerHand;
        Scanner scan = new Scanner(System.in);

        public Player(int playerNum) {
            playerNumber = playerNum;
            playerHand = new ArrayList<>();
            getPlayerName();
        }

        public void getPlayerName() {
            System.out.print("Enter name of player " + (playerNumber + 1) + ": ");
            playerName = scan.nextLine();
            System.out.println();
        }

        public void addCardToHand(Card card) {
            playerHand.add(card);
        }

        public Card removeCardFromHand(int index) {
            return playerHand.remove(index);
        }

        public void displayPlayerHand() {
            System.out.println(playerName + "'s cards:");
            for (int i = 0; i < playerHand.size(); ++i) {
                System.out.println((i + 1) + ". " + playerHand.get(i).toString());
            }
        }

        public int getHandSize() {
            return playerHand.size();
        }
    }

    static class Deck {
        private Stack<Card> deck;

        public Deck() {
            deck = new Stack<>();
            initializeDeck();
            shuffle();
        }

        private void initializeDeck() {
            for (Color color : Color.values()) {
                if (color == Color.NONE) continue;
                for (int i = 0; i <= 9; ++i) {
                    deck.add(new Card(CardType.NUMBER, color, i));
                    if (i != 0) deck.add(new Card(CardType.NUMBER, color, i));
                }
                deck.add(new Card(CardType.SKIP, color, -1));
                deck.add(new Card(CardType.REVERSE, color, -1));
                deck.add(new Card(CardType.DRAW_TWO, color, -1));
            }
            for (int i = 0; i < 4; i++) {
                deck.add(new Card(CardType.WILD, Color.NONE, -1));
                deck.add(new Card(CardType.WILD_DRAW_FOUR, Color.NONE, -1));
            }
        }

        public Card drawCard() {
            if (deck.isEmpty()) {
                reshuffle();
            }
            return deck.pop();
        }

        //handles the scenario where the deck is empty
        private void reshuffle() {
            Card topCard = DiscardPile.drawCard();
            while (!DiscardPile.isEmpty()) {
                deck.add(DiscardPile.drawCard());
            }
            Collections.shuffle(deck);
            DiscardPile.addCard(topCard);
        }

        private void shuffle() {
            Collections.shuffle(deck);
        }
    }

    static class DiscardPile {
        private static Stack<Card> discardPile;

        public DiscardPile() {
            discardPile = new Stack<>();
        }

        public static void addCard(Card card) {
            discardPile.push(card);
        }

        public static Card drawCard() {
            return discardPile.peek();
        }

        public static boolean isEmpty() {
            return discardPile.isEmpty();
        }
    }

    public static abstract class Game {
        protected List<Player> players;
        protected Deck deck;
        protected DiscardPile discardPile;
        protected boolean isClockwise;
        protected int currentPlayerIndex;
        Scanner scan = new Scanner(System.in);

        public Game(int numberOfPlayers) {
            players = new ArrayList<>();
            deck = new Deck();
            discardPile = new DiscardPile();
            isClockwise = true;
            currentPlayerIndex = 0;

            for (int i = 0; i < numberOfPlayers; ++i) {
                players.add(new Player(i));
            }

            dealInitialCards(7);
            DiscardPile.addCard(deck.drawCard());  // Adds the first playing card
        }

        public void dealInitialCards(int numberOfCards) {
            for (Player player : players) {
                for (int i = 0; i < numberOfCards; ++i) {
                    player.addCardToHand(deck.drawCard());
                }
            }
        }

        public abstract void play();

        protected void nextTurn() {
            if (isClockwise) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } else {
                currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
            }
        }

        protected abstract boolean isGameOver();

        protected abstract boolean canPlay(Card card, Card topCard);

        protected Color chooseColor() {
            while (true) {
                System.out.println("Choose a color: RED, GREEN, BLUE, YELLOW");
                String colorInput = scan.next().toUpperCase();
                try {
                    return Color.valueOf(colorInput);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid color. Please enter one of the following: RED, GREEN, BLUE, YELLOW.");
                }
            }
        }
    }

    public static class DefaultGame extends Game {
        public DefaultGame(int numberOfPlayers) {
            super(numberOfPlayers);
        }

        @Override
        public void play() {
            while (!isGameOver()) {
                Player currentPlayer = players.get(currentPlayerIndex);
                System.out.println(currentPlayer.playerName + "'s turn.");

                Card topCard = DiscardPile.drawCard();
                System.out.println("Top card on discard pile: " + topCard.toString());

                boolean validPlay = false;
                while (!validPlay) {
                    currentPlayer.displayPlayerHand();
                    System.out.println("Enter the card number to play or 0 to draw a card:");
                    int cardIndex = scan.nextInt() - 1;
                    if (cardIndex == -1) {
                        currentPlayer.addCardToHand(deck.drawCard());
                        validPlay = true;
                    } else {
                        Card chosenCard = currentPlayer.removeCardFromHand(cardIndex);
                        if (canPlay(chosenCard, topCard)) {
                            DiscardPile.addCard(chosenCard);
                            applyCardEffect(chosenCard);
                            validPlay = true;
                        } else {
                            System.out.println("You can't play that card. Try again.");
                            currentPlayer.addCardToHand(chosenCard);
                        }
                    }
                }
                nextTurn();
            }
            System.out.println("Game Over!");
        }

        @Override
        protected boolean isGameOver() {
            for (Player player : players) {
                if (player.getHandSize() == 0) {
                    System.out.println(player.playerName + " wins!");
                    return true;
                }
            }
            return false;
        }

        @Override
        protected boolean canPlay(Card card, Card topCard) {
            return card.getColor() == topCard.getColor() ||
                    (card.getType() == CardType.NUMBER && topCard.getType() == CardType.NUMBER && card.getNumber() == topCard.getNumber()) ||
                    (card.getType()!=CardType.NUMBER && topCard.getType() !=CardType.NUMBER && card.getType()==topCard.getType()) ||
                    card.getType() == CardType.WILD ||
                    card.getType() == CardType.WILD_DRAW_FOUR ;
        }

        private void applyCardEffect(Card card) {
            switch (card.getType()) {
                case SKIP:
                    System.out.println("Next player is skipped!");
                    nextTurn();
                    break;
                case REVERSE:
                    System.out.println("Direction is reversed!");
                    isClockwise=!isClockwise;
                    break;
                case DRAW_TWO:
                    nextTurn();
                    System.out.println(players.get(currentPlayerIndex).playerName + " draws two cards!");
                    players.get(currentPlayerIndex).addCardToHand(deck.drawCard());
                    players.get(currentPlayerIndex).addCardToHand(deck.drawCard());
                    break;
                case WILD:
                    System.out.println("Choose a color!");
                    card = new Card(CardType.WILD, chooseColor(), -1);
                    DiscardPile.addCard(card);
                    break;
                case WILD_DRAW_FOUR:
                    System.out.println("Choose a color and next player draws four cards!");
                    card = new Card(CardType.WILD_DRAW_FOUR, chooseColor(), -1);
                    DiscardPile.addCard(card);
                    nextTurn();
                    for (int i = 0; i < 4; ++i) {
                        players.get(currentPlayerIndex).addCardToHand(deck.drawCard());
                    }
                    break;
                default:
                    break;
            }
        }
    }
 on intellij?
    public static class GameDriver {
        public static void main(String[] args) {
            Game game = new DefaultGame(4);
            game.play();
        }
    }
}
