package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.Player;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public class PlayerInventory extends Inventory {

    private final Player player;

    public PlayerInventory(Match match, Player player) {
        super(match, player);
        this.player = player;
    }

    @Override
    public int getSelectedIndex() {
        return getMatch().getHud().getSelectedSlot();
    }

    @Override
    public void useSelectedItem(Soldier soldier) {
        if (isSelectedBeingUsed()) {
            return;
        }
        EntityObject selected = getSelectedItem();
        if (selected instanceof Medkit) {
            if (soldier.getHealth() < 100) {
                startAction("healing", Medkit.USE_SECONDS, true);
            }
        } else if (selected instanceof Weapon) {
            Weapon weapon = (Weapon) selected;
            if (weapon.getBullets() == 0) {
                reloadSelectedWeapon();
            } else {
                weapon.shoot(soldier);
            }
        } else if (selected instanceof Empty) {
            soldier.punch();
        }
    }

    @Override
    public void reloadSelectedWeapon() {
        EntityObject selected = getSelectedItem();
        if (!isSelectedBeingUsed() && selected instanceof Weapon
                && ((Weapon) selected).getBullets() < ((Weapon) selected).getMagCapacity()) {
            startAction("reloading", ((Weapon) selected).getReloadSeconds(), false);
        }
    }

    /** Healing roots the player in place; reloading only slows them down. */
    private void startAction(String activity, float seconds, boolean blocksMovement) {
        setSelectedBeingUsed(true);
        if (blocksMovement) {
            player.blockActions();
        } else {
            player.slowDown();
        }
        getMatch().getProgress().start(activity, seconds);
    }

    /** Called by the progress bar when the action completes. */
    public void finishAction() {
        setSelectedBeingUsed(false);
        player.unblockActions();
        EntityObject selected = getSelectedItem();
        selected.transformSoldier(player);
        if (selected instanceof Medkit) {
            getItems().set(getSelectedIndex(), new Empty(getMatch()));
        }
    }

    /** Called when an action is interrupted, for example by death. */
    public void cancelAction() {
        setSelectedBeingUsed(false);
        player.unblockActions();
    }
}
