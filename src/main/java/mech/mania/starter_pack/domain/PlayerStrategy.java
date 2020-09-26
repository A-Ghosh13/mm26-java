package mech.mania.starter_pack.domain;

import mech.mania.starter_pack.domain.model.GameState;
import mech.mania.starter_pack.domain.model.board.Board;
import mech.mania.starter_pack.domain.model.characters.*;
import mech.mania.starter_pack.domain.model.characters.Character;
import mech.mania.starter_pack.domain.model.items.*;
import mech.mania.starter_pack.domain.model.board.*;
import mech.mania.starter_pack.domain.memory.MemoryObject;
import mech.mania.starter_pack.domain.model.items.Item;
import mech.mania.starter_pack.domain.model.items.Weapon;
import java.util.logging.Logger;
import java.util.List;

public class PlayerStrategy implements Strategy {
    /**
     * This MemoryObject allows you to store persistent data of the types
     * int, double/float, string, and boolean. See the MemoryObject documentation
     * (https://github.com/jackducham/mm26-infra/blob/ready-for-release-94/memory-object/API.md)
     * for details on usage.
     */
    private MemoryObject memory;
    private API api;
    private static Logger log = Logger.getLogger(PlayerStrategy.class.toString());

    public PlayerStrategy(MemoryObject memory){
        this.memory = memory;
    }

    /**
     * Takes an input of a target position and returns the position to which the Character should move on its next turn
     * in order to arrive at the target position in the shortest time.
     * @param player your Player object
     * @param destination the position you want to end up at
     * @return the position you should move to
     */
    private Position findPositionToMove(Player player, Position destination) {
        List<Position> path = api.findPath(player.getPosition(), destination);
        Position pos;
        if (path.size() < player.getSpeed()) {
            pos = path.get(path.size() - 1);
        } else {
            pos = path.get(player.getSpeed() - 1);
        }
        return pos;
    }

    /**
     * TODO: implement your strategy here! Return a CharacterDecision using either of the following constructors:
     * CharacterDecision(DecisionType decision, Position actionPosition)
     * CharacterDecision(DecisionType decision, int actionIndex)
     *
     * The default constructor makes no decision -- your player will not act in the next turn
     */
    public CharacterDecision makeDecision(String playerName, GameState gameState){
        /*
         * This API object gives you access to a few helper functions including pathfinding!
         * You'll have to reinitialize it with the new GameState and your playerName ever turn.
         */
        API api = new API(gameState, playerName);
        Board board = gameState.getPvpBoard();
        Player myPlayer = gameState.getAllPlayers().get(playerName);
        Position currPos = myPlayer.getPosition();

        // if lastAction was PICKUP, EQUIP first item in inventory
        String lastAction = memory.getValueString("lastAction");
        if (lastAction != null && lastAction.equals("PICKUP")) {
            memory.setValue("lastAction", "EQUIP");
            log.info("equip");
            return new CharacterDecision(CharacterDecision.DecisionType.EQUIP, myPlayer.getFirstInventoryIndex());
        }

        // pick up first item on current tile
        List<Item> tileItems = board.getTileAtPosition(currPos).getItems();
        if (tileItems != null && !tileItems.isEmpty()) {
            memory.setValue("lastAction", "PICKUP");
            log.info("pickup");
            return new CharacterDecision(CharacterDecision.DecisionType.PICKUP, 0);
        }

        Weapon weapon = myPlayer.getWeapon();
        List<Character> enemies = api.findEnemies(currPos);

        //move to spawn point if health below 20% max health
        if (myPlayer.getCurrentHealth() < myPlayer.getMaxHealth() * 0.2) {
            memory.setValue("lastAction", "MOVE");
            log.info("move to spawn");
            return new CharacterDecision(CharacterDecision.DecisionType.MOVE, findPositionToMove(myPlayer, myPlayer.getSpawnPoint()));
        }

        // attack closest enemy
        Position enemyPos = enemies.get(0).getPosition();
        if (currPos.manhattanDistance(enemyPos) <= weapon.getRange()) {
            memory.setValue("lastAction", "ATTACK");
            log.info("attack");
            return new CharacterDecision(CharacterDecision.DecisionType.ATTACK, enemyPos);
        }

        // move towards closest enemy
        memory.setValue("lastAction", "MOVE");
        log.info("move to monster");
        return new CharacterDecision(CharacterDecision.DecisionType.MOVE, findPositionToMove(myPlayer, enemyPos));
    }
}
